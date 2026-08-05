#!/usr/bin/env python3
"""Enforce Code Nest module seams that Maven itself cannot express."""

from __future__ import annotations

import sys
import json
import re
import xml.etree.ElementTree as element_tree
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
MAVEN_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}
LEGACY_COMMON_CONSUMERS = {
    "xiaou-application",
    "xiaou-community",
    "xiaou-interview",
    "xiaou-learning-asset",
    "xiaou-moment",
    "xiaou-plan",
    "xiaou-user",
}
FOUNDATION_MODULES = {
    "xiaou-common-core",
    "xiaou-common-web",
    "xiaou-common-security",
    "xiaou-common-cache",
    "xiaou-common-persistence",
}
APPLICATION_DOMAIN_IMPORT_ALLOWLIST: set[Path] = set()
EXTERNAL_PERSISTENCE_IMPORT = re.compile(
    r"^import com\.xiaou\.[^.]+\.(?:mapper|domain)\.",
    re.MULTILINE,
)
SRE_REVERSE_DEPENDENCY_IMPORT = re.compile(
    r"^import com\.xiaou\.(?:ai|system)\.",
    re.MULTILINE,
)
SRE_TRANSPORT_DTO_IMPORT = re.compile(
    r"^import com\.xiaou\.sre\.dto\.request\.Alertmanager(?:Alert|WebhookRequest);$",
    re.MULTILINE,
)
AI_SRE_IMPORT = re.compile(r"^import com\.xiaou\.sre\.", re.MULTILINE)
SYSTEM_SRE_ADAPTER_ALLOWLIST = {
    Path("xiaou-system/src/main/java/com/xiaou/system/agent/tools/SreIncidentRcaAgentTool.java"),
}
NOTIFICATION_IMPLEMENTATION_IMPORT = re.compile(
    r"^import com\.xiaou\.notification\.(?!api\.)",
    re.MULTILINE,
)
CACHE_IMPLEMENTATION_IMPORT = re.compile(
    r"^import com\.xiaou\.common\.cache\.Redis(?:ValueStore|TextStateStore);$",
    re.MULTILINE,
)
SPRING_REDIS_IMPORT = re.compile(
    r"^import org\.springframework\.data\.redis\.core\.(?:StringRedisTemplate|RedisTemplate);$",
    re.MULTILINE,
)
REDISSON_API_IMPORT = re.compile(r"^import org\.redisson\.api\.", re.MULTILINE)
REDISSON_DOMAIN_ADAPTER_ALLOWLIST = {
    Path("xiaou-application/src/main/java/com/xiaou/web/growthcoach/service/GrowthCoachPlanningLock.java"),
    Path("xiaou-chat/src/main/java/com/xiaou/chat/service/impl/ChatOnlineUserServiceImpl.java"),
    Path("xiaou-community/src/main/java/com/xiaou/community/service/impl/CommunityCacheServiceImpl.java"),
    Path("xiaou-community/src/main/java/com/xiaou/community/service/impl/CommunityHotPostServiceImpl.java"),
    Path("xiaou-moment/src/main/java/com/xiaou/moment/task/HotMomentCalculateTask.java"),
    Path("xiaou-points/src/main/java/com/xiaou/points/chain/impl/RateLimitCheckHandler.java"),
    Path("xiaou-points/src/main/java/com/xiaou/points/service/impl/LotteryServiceImpl.java"),
    Path("xiaou-points/src/main/java/com/xiaou/points/service/impl/LotteryStockServiceImpl.java"),
}
RESILIENT_AGGREGATORS = {
    Path("xiaou-application/src/main/java/com/xiaou/web/growthcoach/service/GrowthCoachBriefingService.java"),
    Path("xiaou-application/src/main/java/com/xiaou/web/home/service/UserHomeOverviewService.java"),
    Path("xiaou-application/src/main/java/com/xiaou/web/learning/service/LearningCockpitService.java"),
    Path("xiaou-system/src/main/java/com/xiaou/system/service/impl/SysDashboardServiceImpl.java"),
}
DUPLICATE_RESILIENCE_IMPLEMENTATION = re.compile(
    r"CompletableFuture\.supplyAsync|\.orTimeout\s*\(|record\s+SourceResult|class\s+TimedResult|"
    r"private\s+<T>\s+T\s+safeCall\s*\("
)
FRONTEND_APPS = ("vue3-user-front", "vue3-admin-front")
FRONTEND_ROUTE_SLICES = {
    "vue3-user-front": {"core", "learning", "career", "community", "productivity", "fallback"},
    "vue3-admin-front": {"core", "learning", "community", "operations", "system", "fallback"},
}
DUPLICATE_RESPONSE_UNWRAPPING = re.compile(
    r"\bdata\.code\b|(?<!\.)\bcode\s*===\s*(?:701|702|703|704)|\bresponseData\b"
)


def parse_pom(path: Path) -> element_tree.Element:
    return element_tree.parse(path).getroot()


def text(root: element_tree.Element, query: str) -> str | None:
    node = root.find(query, MAVEN_NAMESPACE)
    return None if node is None else node.text


def artifact_id(root: element_tree.Element) -> str:
    value = text(root, "m:artifactId")
    if not value:
        raise ValueError("POM has no artifactId")
    return value.strip()


def dependency_artifacts(root: element_tree.Element) -> set[str]:
    return {
        node.text.strip()
        for node in root.findall("m:dependencies/m:dependency/m:artifactId", MAVEN_NAMESPACE)
        if node.text
    }


def plugin_artifacts(root: element_tree.Element) -> set[str]:
    return {
        node.text.strip()
        for node in root.findall("m:build/m:plugins/m:plugin/m:artifactId", MAVEN_NAMESPACE)
        if node.text
    }


def module_poms() -> dict[str, Path]:
    root = parse_pom(REPO_ROOT / "pom.xml")
    result: dict[str, Path] = {}
    for node in root.findall("m:modules/m:module", MAVEN_NAMESPACE):
        if not node.text:
            continue
        path = REPO_ROOT / node.text.strip() / "pom.xml"
        parsed = parse_pom(path)
        result[artifact_id(parsed)] = path
    return result


def validate() -> list[str]:
    errors: list[str] = []
    poms = module_poms()

    for module, path in sorted(poms.items()):
        root = parse_pom(path)
        dependencies = dependency_artifacts(root)
        plugins = plugin_artifacts(root)

        if "spring-boot-maven-plugin" in plugins and module != "xiaou-bootstrap":
            errors.append(f"{module}: executable Spring Boot packaging belongs in xiaou-bootstrap")

        if "xiaou-common" in dependencies and module not in LEGACY_COMMON_CONSUMERS:
            errors.append(f"{module}: new dependencies on legacy xiaou-common are forbidden")

        if module == "xiaou-common-core":
            internal = {item for item in dependencies if item.startswith("xiaou-")}
            if internal:
                errors.append(f"xiaou-common-core: must not depend on internal modules: {sorted(internal)}")

        if module in FOUNDATION_MODULES - {"xiaou-common-core"}:
            internal = {item for item in dependencies if item.startswith("xiaou-")}
            if internal - {"xiaou-common-core"}:
                errors.append(f"{module}: foundation dependency must point only to xiaou-common-core")

        if module == "xiaou-sre" and dependencies & {"xiaou-ai", "xiaou-system"}:
            errors.append("xiaou-sre: domain module must not depend on xiaou-ai or xiaou-system")

        if module == "xiaou-ai" and "xiaou-sre" not in dependencies:
            errors.append("xiaou-ai: SRE model adapters require the xiaou-sre port dependency")

        if module == "xiaou-resilience":
            internal = {item for item in dependencies if item.startswith("xiaou-")}
            if internal:
                errors.append(
                    f"xiaou-resilience: execution policy must not depend on domain modules: {sorted(internal)}"
                )

        if module in {"xiaou-application", "xiaou-system"} and "xiaou-resilience" not in dependencies:
            errors.append(f"{module}: aggregate queries require xiaou-resilience")

    bootstrap = dependency_artifacts(parse_pom(poms["xiaou-bootstrap"]))
    required_bootstrap = {"xiaou-application"} | (FOUNDATION_MODULES - {"xiaou-common-core"})
    missing = required_bootstrap - bootstrap
    if missing:
        errors.append(f"xiaou-bootstrap: missing composition dependencies: {sorted(missing)}")

    application_root = REPO_ROOT / "xiaou-application" / "src" / "main"
    runtime_yaml = [
        path
        for path in application_root.rglob("application*.yml")
        if path.name != "application-sec.yml"
    ]
    if runtime_yaml:
        errors.append("xiaou-application: runtime application YAML belongs in xiaou-bootstrap")
    for java_file in application_root.rglob("*.java"):
        if "@SpringBootApplication" in java_file.read_text(encoding="utf-8"):
            errors.append(f"{java_file.relative_to(REPO_ROOT)}: boot entry point belongs in xiaou-bootstrap")

    application_java = application_root / "java"
    used_domain_import_allowlist: set[Path] = set()
    for java_file in application_java.rglob("*.java"):
        source = java_file.read_text(encoding="utf-8")
        if not EXTERNAL_PERSISTENCE_IMPORT.search(source):
            continue
        relative_file = java_file.relative_to(REPO_ROOT)
        if "adapter" in java_file.relative_to(application_java).parts:
            continue
        if relative_file in APPLICATION_DOMAIN_IMPORT_ALLOWLIST:
            used_domain_import_allowlist.add(relative_file)
            continue
        errors.append(
            f"{relative_file}: cross-module mapper/domain imports belong behind an application adapter"
        )

    stale_domain_import_allowlist = APPLICATION_DOMAIN_IMPORT_ALLOWLIST - used_domain_import_allowlist
    if stale_domain_import_allowlist:
        errors.append(
            "application domain import allowlist can be reduced: "
            f"{sorted(str(path) for path in stale_domain_import_allowlist)}"
        )

    sre_java = REPO_ROOT / "xiaou-sre" / "src" / "main" / "java"
    for java_file in sre_java.rglob("*.java"):
        source = java_file.read_text(encoding="utf-8")
        relative_file = java_file.relative_to(REPO_ROOT)
        if SRE_REVERSE_DEPENDENCY_IMPORT.search(source):
            errors.append(f"{relative_file}: xiaou-sre must not import xiaou-ai or xiaou-system")
        if SRE_TRANSPORT_DTO_IMPORT.search(source):
            relative_source = java_file.relative_to(sre_java)
            if relative_source.parts[:5] != ("com", "xiaou", "sre", "controller", "internal"):
                errors.append(
                    f"{relative_file}: Alertmanager transport DTOs belong in the internal controller adapter"
                )

    ai_java = REPO_ROOT / "xiaou-ai" / "src" / "main" / "java"
    for java_file in ai_java.rglob("*.java"):
        source = java_file.read_text(encoding="utf-8")
        if not AI_SRE_IMPORT.search(source):
            continue
        relative_source = java_file.relative_to(ai_java)
        if relative_source.parts[:4] != ("com", "xiaou", "ai", "sre"):
            errors.append(
                f"{java_file.relative_to(REPO_ROOT)}: SRE dependencies in xiaou-ai belong in its SRE adapter package"
            )

    system_java = REPO_ROOT / "xiaou-system" / "src" / "main" / "java"
    used_system_sre_allowlist: set[Path] = set()
    for java_file in system_java.rglob("Sre*.java"):
        relative_file = java_file.relative_to(REPO_ROOT)
        if relative_file in SYSTEM_SRE_ADAPTER_ALLOWLIST:
            used_system_sre_allowlist.add(relative_file)
            continue
        errors.append(f"{relative_file}: SRE production ownership belongs in xiaou-sre or an adapter module")

    stale_system_sre_allowlist = SYSTEM_SRE_ADAPTER_ALLOWLIST - used_system_sre_allowlist
    if stale_system_sre_allowlist:
        errors.append(
            "system SRE adapter allowlist can be reduced: "
            f"{sorted(str(path) for path in stale_system_sre_allowlist)}"
        )

    legacy_notification_types = sorted(
        path.relative_to(REPO_ROOT)
        for path in (REPO_ROOT / "xiaou-common" / "src" / "main").rglob("*Notification*")
    )
    if legacy_notification_types:
        errors.append(
            "xiaou-common: notification ownership belongs in xiaou-notification: "
            f"{[str(path) for path in legacy_notification_types]}"
        )

    used_redisson_adapter_allowlist: set[Path] = set()
    for module, pom_path in sorted(poms.items()):
        if module == "xiaou-notification":
            continue
        java_root = pom_path.parent / "src" / "main" / "java"
        if not java_root.exists():
            continue
        for java_file in java_root.rglob("*.java"):
            if NOTIFICATION_IMPLEMENTATION_IMPORT.search(java_file.read_text(encoding="utf-8")):
                errors.append(
                    f"{java_file.relative_to(REPO_ROOT)}: notification callers may import only "
                    "com.xiaou.notification.api"
                )

    for module, pom_path in sorted(poms.items()):
        if module == "xiaou-common-cache":
            continue
        java_root = pom_path.parent / "src" / "main" / "java"
        if not java_root.exists():
            continue
        for java_file in java_root.rglob("*.java"):
            source = java_file.read_text(encoding="utf-8")
            relative_file = java_file.relative_to(REPO_ROOT)
            if CACHE_IMPLEMENTATION_IMPORT.search(source):
                errors.append(
                    f"{relative_file}: cache callers must depend on CacheStore or TextStateStore"
                )
            if SPRING_REDIS_IMPORT.search(source):
                errors.append(
                    f"{relative_file}: generic Redis value access belongs in xiaou-common-cache"
                )
            if not REDISSON_API_IMPORT.search(source):
                continue
            if relative_file in REDISSON_DOMAIN_ADAPTER_ALLOWLIST:
                used_redisson_adapter_allowlist.add(relative_file)
                continue
            errors.append(
                f"{relative_file}: direct Redisson access requires an explicit domain adapter"
            )

    stale_redisson_adapter_allowlist = (
        REDISSON_DOMAIN_ADAPTER_ALLOWLIST - used_redisson_adapter_allowlist
    )
    if stale_redisson_adapter_allowlist:
        errors.append(
            "Redisson domain adapter allowlist can be reduced: "
            f"{sorted(str(path) for path in stale_redisson_adapter_allowlist)}"
        )

    for relative_file in sorted(RESILIENT_AGGREGATORS):
        source = (REPO_ROOT / relative_file).read_text(encoding="utf-8")
        if "import com.xiaou.resilience.ResilientExecutor;" not in source:
            errors.append(f"{relative_file}: aggregate query must use ResilientExecutor")
        if DUPLICATE_RESILIENCE_IMPLEMENTATION.search(source):
            errors.append(
                f"{relative_file}: timeout and fallback execution belongs in xiaou-resilience"
            )

    api_contract_root = REPO_ROOT / "code-nest-api-contract"
    required_contract_files = {
        api_contract_root / "package.json",
        api_contract_root / "src" / "index.js",
        api_contract_root / "src" / "index.d.ts",
    }
    missing_contract_files = sorted(
        str(path.relative_to(REPO_ROOT)) for path in required_contract_files if not path.exists()
    )
    if missing_contract_files:
        errors.append(f"shared API contract is incomplete: {missing_contract_files}")

    for frontend in FRONTEND_APPS:
        package_path = REPO_ROOT / frontend / "package.json"
        package = json.loads(package_path.read_text(encoding="utf-8"))
        dependency = package.get("dependencies", {}).get("@code-nest/api-contract")
        if dependency != "file:../code-nest-api-contract":
            errors.append(f"{frontend}: must depend on the shared @code-nest/api-contract package")

        request_file = REPO_ROOT / frontend / "src" / "utils" / "request.js"
        request_source = request_file.read_text(encoding="utf-8")
        if "from '@code-nest/api-contract'" not in request_source:
            errors.append(f"{request_file.relative_to(REPO_ROOT)}: response handling belongs in the shared contract")
        if DUPLICATE_RESPONSE_UNWRAPPING.search(request_source):
            errors.append(
                f"{request_file.relative_to(REPO_ROOT)}: duplicate response-code handling is forbidden"
            )

        router_entry = REPO_ROOT / frontend / "src" / "router" / "index.js"
        router_lines = router_entry.read_text(encoding="utf-8").splitlines()
        if len(router_lines) >= 120:
            errors.append(f"{router_entry.relative_to(REPO_ROOT)}: route registration belongs in feature slices")
        route_directory = router_entry.parent / "routes"
        actual_slices = {path.stem for path in route_directory.glob("*.js")}
        missing_slices = FRONTEND_ROUTE_SLICES[frontend] - actual_slices
        if missing_slices:
            errors.append(f"{frontend}: missing route slices: {sorted(missing_slices)}")

        notification_page = REPO_ROOT / frontend / "src" / "views" / "notification" / "index.vue"
        notification_source = notification_page.read_text(encoding="utf-8")
        if ".createTime" in notification_source:
            errors.append(
                f"{notification_page.relative_to(REPO_ROOT)}: notification timestamps use createdTime"
            )
        if "NotificationRecord" not in notification_source:
            errors.append(
                f"{notification_page.relative_to(REPO_ROOT)}: notification DTOs belong in the shared contract"
            )

    notification_controllers = (
        REPO_ROOT / "xiaou-notification" / "src" / "main" / "java" / "com" / "xiaou"
        / "notification" / "controller"
    )
    for controller in notification_controllers.glob("*.java"):
        source = controller.read_text(encoding="utf-8")
        if "import com.xiaou.notification.domain." in source:
            errors.append(
                f"{controller.relative_to(REPO_ROOT)}: web contracts must not expose notification persistence entities"
            )

    exception_handler = (
        REPO_ROOT / "xiaou-common-web" / "src" / "main" / "java" / "com" / "xiaou"
        / "common" / "exception" / "GlobalExceptionHandler.java"
    )
    if "@ResponseStatus(HttpStatus.OK)" in exception_handler.read_text(encoding="utf-8"):
        errors.append("GlobalExceptionHandler: failures must use semantic HTTP statuses")

    status_advice = (
        REPO_ROOT / "xiaou-common-web" / "src" / "main" / "java" / "com" / "xiaou"
        / "common" / "web" / "ResultHttpStatusAdvice.java"
    )
    if not status_advice.exists():
        errors.append("xiaou-common-web: Result errors require centralized HTTP status mapping")

    actual_legacy_consumers = {
        module
        for module, path in poms.items()
        if "xiaou-common" in dependency_artifacts(parse_pom(path))
    }
    stale_allowlist = LEGACY_COMMON_CONSUMERS - actual_legacy_consumers
    if stale_allowlist:
        errors.append(f"legacy xiaou-common allowlist can be reduced: {sorted(stale_allowlist)}")

    return errors


def main() -> int:
    errors = validate()
    if errors:
        for error in errors:
            print(f"architecture violation: {error}", file=sys.stderr)
        return 1
    print("architecture contracts passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
