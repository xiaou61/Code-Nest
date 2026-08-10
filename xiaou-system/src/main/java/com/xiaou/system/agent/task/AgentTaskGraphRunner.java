package com.xiaou.system.agent.task;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LangGraph4j graph that advances one durable task by one persisted cycle.
 */
@Component
public class AgentTaskGraphRunner {

    private static final String START = "__START__";
    private static final String END = "__END__";

    private final AgentTaskCycleRuntime runtime;
    private volatile CompiledGraph<AgentTaskCycleState> graph;

    public AgentTaskGraphRunner(AgentTaskCycleRuntime runtime) {
        this.runtime = runtime;
    }

    public AgentTaskCycleOutcome runCycle(String taskId, String leaseOwner) {
        Map<String, Object> input = new HashMap<>();
        input.put(AgentTaskCycleState.TASK_ID, taskId);
        input.put(AgentTaskCycleState.LEASE_OWNER, leaseOwner);
        try {
            AgentTaskCycleState state = getGraph().invoke(input)
                    .orElseThrow(() -> new IllegalStateException("任务图没有返回最终状态"));
            AgentTaskCycleOutcome outcome = state.outcome();
            return outcome == null
                    ? AgentTaskCycleOutcome.failed("任务图没有返回执行结果")
                    : outcome;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("管理员智能体任务图执行失败", e);
        }
    }

    private Map<String, Object> loadContext(AgentTaskCycleState state) {
        AgentTaskCycleContext context = runtime.loadContext(state.taskId(), state.leaseOwner());
        if (context == null) {
            context = new AgentTaskCycleContext(state.taskId(), state.leaseOwner(), true);
        }
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(AgentTaskCycleState.CONTEXT, context);
        if (context == null || !context.active()) {
            updates.put(AgentTaskCycleState.OUTCOME,
                    AgentTaskCycleOutcome.lostLease("任务租约已失效，图停止继续路由。"));
        }
        return updates;
    }

    private Map<String, Object> planNextStep(AgentTaskCycleState state) {
        AgentTaskCycleContext context = state.context();
        AgentTaskPlanDecision decision = context != null && context.snapshot() != null
                ? runtime.planNext(context)
                : runtime.planNext(state.taskId(), state.leaseOwner());
        return update(AgentTaskCycleState.DECISION, decision);
    }

    private Map<String, Object> policyGuard(AgentTaskCycleState state) {
        return update(AgentTaskCycleState.POLICY_DECISION,
                runtime.policyGuard(state.context(), state.decision()));
    }

    private Map<String, Object> executeOrFinish(AgentTaskCycleState state) {
        AgentTaskPlanDecision policyDecision = state.policyDecision();
        AgentTaskPlanDecision decision = policyDecision == null
                || "UNINITIALIZED".equals(policyDecision.code())
                ? state.decision() : policyDecision;
        AgentTaskCycleContext context = state.context();
        AgentTaskCycleOutcome outcome = context != null && context.snapshot() != null
                ? runtime.executeOrFinish(context, decision)
                : runtime.executeOrFinish(state.taskId(), state.leaseOwner(), decision);
        return update(AgentTaskCycleState.OUTCOME, outcome);
    }

    private Map<String, Object> waitForInput(AgentTaskCycleState state) {
        return update(AgentTaskCycleState.OUTCOME,
                runtime.waitForInput(state.context(), state.decision()));
    }

    private Map<String, Object> observe(AgentTaskCycleState state) {
        // Observe is an explicit graph phase; durable outcome recording happens in the
        // runtime's transactional execution methods. Keep this phase side-effect free so
        // one cycle preserves the existing plan/execute interaction contract.
        return update(AgentTaskCycleState.OUTCOME, state.outcome());
    }

    private Map<String, Object> route(AgentTaskCycleState state) {
        // A cycle is deliberately bounded to one invocation. The durable worker decides
        // whether to claim another cycle after this graph returns, so this terminal route
        // must not introduce another runtime interaction.
        return update(AgentTaskCycleState.ROUTE, "end");
    }

    private String routeAfterContext(AgentTaskCycleState state) {
        return state.context() != null && state.context().active() ? "active" : "end";
    }

    private String routeAfterPlan(AgentTaskCycleState state) {
        if (state.context() == null || !state.context().active()) {
            return "end";
        }
        AgentTaskPlanDecision decision = state.decision();
        if (decision == null) {
            return "end";
        }
        if (decision.waitingInput()) {
            return "waiting_input";
        }
        return decision.execute() ? "execute" : "finish";
    }

    private String routeAfterPolicy(AgentTaskCycleState state) {
        AgentTaskPlanDecision decision = state.policyDecision();
        return decision != null && decision.execute() ? "execute" : "reject";
    }

    private String routeAfterRoute(AgentTaskCycleState state) {
        return "end";
    }

    private CompiledGraph<AgentTaskCycleState> getGraph() {
        CompiledGraph<AgentTaskCycleState> local = graph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (graph == null) {
                graph = buildGraph();
            }
            return graph;
        }
    }

    private CompiledGraph<AgentTaskCycleState> buildGraph() {
        try {
            StateGraph<AgentTaskCycleState> stateGraph = new StateGraph<>(channels(), AgentTaskCycleState::new);
            stateGraph.addNode("load_context", AsyncNodeAction.node_async(this::loadContext));
            stateGraph.addNode("plan_next_step", AsyncNodeAction.node_async(this::planNextStep));
            stateGraph.addNode("policy_guard", AsyncNodeAction.node_async(this::policyGuard));
            stateGraph.addNode("execute_or_finish", AsyncNodeAction.node_async(this::executeOrFinish));
            stateGraph.addNode("wait_for_input", AsyncNodeAction.node_async(this::waitForInput));
            stateGraph.addNode("observe", AsyncNodeAction.node_async(this::observe));
            stateGraph.addNode("route", AsyncNodeAction.node_async(this::route));
            stateGraph.addEdge(START, "load_context");
            stateGraph.addConditionalEdges("load_context",
                    AsyncEdgeAction.edge_async(this::routeAfterContext),
                    Map.of("active", "plan_next_step", "end", END));
            stateGraph.addConditionalEdges("plan_next_step",
                    AsyncEdgeAction.edge_async(this::routeAfterPlan),
                    Map.of("execute", "policy_guard", "finish", "execute_or_finish",
                            "waiting_input", "wait_for_input", "end", END));
            stateGraph.addConditionalEdges("policy_guard",
                    AsyncEdgeAction.edge_async(this::routeAfterPolicy),
                    Map.of("execute", "execute_or_finish", "reject", "execute_or_finish"));
            stateGraph.addEdge("execute_or_finish", "observe");
            stateGraph.addEdge("wait_for_input", "observe");
            stateGraph.addEdge("observe", "route");
            stateGraph.addConditionalEdges("route",
                    AsyncEdgeAction.edge_async(this::routeAfterRoute),
                    Map.of("end", END));
            return stateGraph.compile();
        } catch (Exception e) {
            throw new IllegalStateException("构建管理员智能体任务图失败", e);
        }
    }

    private Map<String, Channel<?>> channels() {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put(AgentTaskCycleState.TASK_ID, Channels.base(() -> ""));
        channels.put(AgentTaskCycleState.LEASE_OWNER, Channels.base(() -> ""));
        channels.put(AgentTaskCycleState.CONTEXT,
                Channels.base(() -> new AgentTaskCycleContext(null, "")));
        channels.put(AgentTaskCycleState.DECISION, Channels.base(() ->
                AgentTaskPlanDecision.blocked("UNINITIALIZED", "任务图尚未规划。", false)));
        channels.put(AgentTaskCycleState.POLICY_DECISION, Channels.base(() ->
                AgentTaskPlanDecision.blocked("UNINITIALIZED", "任务图尚未完成策略检查。", false)));
        channels.put(AgentTaskCycleState.OUTCOME, Channels.base(() ->
                AgentTaskCycleOutcome.failed("任务图尚未执行。")));
        channels.put(AgentTaskCycleState.ROUTE, Channels.base(() -> ""));
        return channels;
    }

    private Map<String, Object> update(String key, Object value) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(key, value);
        return updates;
    }
}
