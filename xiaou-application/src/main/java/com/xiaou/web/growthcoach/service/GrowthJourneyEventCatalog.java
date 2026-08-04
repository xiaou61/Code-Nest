package com.xiaou.web.growthcoach.service;

import java.util.Set;

/** Versioned allow-list for the user growth funnel contract. */
public final class GrowthJourneyEventCatalog {

    public static final String SCHEMA_VERSION = "1";

    public static final Set<String> EVENT_TYPES = Set.of(
            "PRIMARY_ACTION_SHOWN",
            "PRIMARY_ACTION_STARTED",
            "PRIMARY_ACTION_COMPLETED",
            "OUTCOME_RECORDED"
    );

    public static final Set<String> ACTION_TYPES = Set.of(
            "PLAN_ADJUSTMENT",
            "JOB_BATTLE_GAP",
            "SKILL_PRACTICE",
            "TODAY_TASK",
            "CAREER_ACTION",
            "APPLICATION_FOLLOW_UP",
            "APPLICATION_PROGRESS",
            "PLAN_SETUP"
    );

    public static final Set<String> SOURCES = Set.of(
            "weekly_review",
            "job_battle_gap",
            "skill_insight",
            "job_market_signal",
            "today_action",
            "career_loop",
            "application_outcomes",
            "growth_autopilot"
    );

    private GrowthJourneyEventCatalog() {
    }
}
