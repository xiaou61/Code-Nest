# Code Nest v2.1.2 Optimization Audit

Date: 2026-05-02

## Completed In This Round

1. User app global navigation was refactored out of `App.vue` into a dedicated config module for better maintainability.
2. A command palette (`Ctrl+K`) was added to the user app for faster cross-module navigation.
3. A structured mobile drawer navigation was added to replace the previous compressed header-only mobile experience.
4. User and admin Vite configs now use manual chunk splitting to separate visualization, markdown, editor, Element Plus, and Vue-related bundles.
5. User app command history was extracted into a shared utility so navigation and homepage can reuse the same recent-workspace source.
6. Homepage now exposes a "recently continue" section to shorten return-to-task time.
7. Homepage realtime refresh now skips hidden-tab polling and refreshes on visibility return.
8. Interview preload was deferred to browser idle time and limited to homepage / interview entry contexts.
9. Full-package Element Plus icon registration was removed from both app entry files; components now rely on explicit local imports only.
10. Both markdown render utilities were changed from full `highlight.js` import to `highlight.js/lib/core` with only commonly used languages registered.
11. Knowledge map `MindMap` components in both fronts now load through `defineAsyncComponent`, moving G6 view code behind route-level demand.
12. Admin router `Layout` was switched from eager import to async route loading to reduce startup-path parsing pressure.

## High-Priority Optimization Backlog

### A. User Experience / UI

1. Unify navigation metadata across `router`, `App.vue`, homepage quick entries, and permission menus to eliminate duplicated route definitions.
2. Add a real unread notification source instead of the current local placeholder in the global header.
3. Add route-level loading skeletons for heavy pages such as OJ, knowledge map, SQL workbench, and learning cockpit.
4. Add a persistent "recently used modules" widget on the homepage backed by the same command-palette history.
5. Add a user-configurable dashboard mode so homepage cards can be rearranged by role: learner, creator, operator.
6. Replace several dense list blocks on the homepage with summary cards plus drill-down actions to reduce visual fatigue.
7. Add global empty-state and error-state components so page messaging stays visually and behaviorally consistent.
8. Add keyboard navigation hints to the command palette and the OJ / editor heavy pages.

### B. Frontend Performance

9. Move heavy markdown rendering dependencies behind dynamic import instead of loading them into common runtime paths.
10. Split G6, Monaco, and charting views at route boundaries with `defineAsyncComponent` plus loading fallbacks.
11. Audit Element Plus usage and switch to on-demand style/component loading to shrink `vendor-element`.
12. Audit shared utility imports to prevent large feature-only helpers from leaking into homepage bundles.
13. Add bundle analysis in CI so size regressions are visible before merge.
14. Add `prefetch` or user-intent based preload only for the top 3 most-used routes instead of broad eager warmup.
15. Revisit `interviewStore.preloadData()` in `main.js`; this may front-load work before users actually enter interview flows.

### C. Frontend Architecture

16. Break the massive user router file into domain routers and compose them in one root router.
17. Add a typed navigation schema layer so routes, menus, command palette, and breadcrumbs all read from one source.
18. Extract a common `page shell` component for header cards, search cards, toolbars, and paginated tables.
19. Standardize data-fetch composables for loading, retry, stale state, and permission-aware error handling.
20. Replace scattered inline gradients and one-off style blocks with semantic design tokens and surface variants.
21. Create a reusable list-card / metric-card / action-card component set shared by user and admin fronts.

### D. Backend / API

22. Review the multi-module API aggregation boundaries in `xiaou-application`; some domain modules appear broad enough to justify clearer API facades.
23. Add endpoint latency and failure-rate dashboards per module, not only platform-level health.
24. Standardize paginated query DTOs and response wrappers across modules to reduce front-end adapter code.
25. Add stricter contract tests around chat, lottery, OJ, and learning cockpit aggregation APIs.
26. Audit scheduler and async task modules for idempotency, retry behavior, and distributed lock coverage.

### E. Data / Search / Domain Workflows

27. Add homepage and cockpit caching tiers with explicit stale times instead of relying on page reload behavior.
28. Add search indexing or lightweight keyword aggregation for community, blog, codepen, and knowledge assets.
29. Add cross-module entity IDs / event tracing so growth cockpit data can be debugged end-to-end.

### F. Security / Reliability / Ops

30. Add bundle-level sourcemap strategy for non-prod debugging while keeping production exposure controlled.
31. Upgrade Sass integration away from the legacy JS API deprecation path shown during current builds.
32. Add a build matrix in CI for `vue3-user-front`, `vue3-admin-front`, and major backend modules separately.
33. Add smoke tests for login, homepage, community, chat, and OJ entry flows before release tagging.
34. Add artifact retention and versioned release notes tied to `v2.1.2` and onward for frontend bundles too.

## Evidence From This Audit

- User app build passes after the UI refactor and chunk split.
- Admin app build passes after chunk split.
- User app `vendor-element` dropped from about `900 kB` to about `792 kB` after removing full icon registration.
- Admin app `vendor-element` dropped from about `900 kB` to about `777 kB` after removing full icon registration.
- User app `vendor-markdown` dropped to about `138 kB` after replacing full `highlight.js` with core + selected languages.
- Admin app `vendor-markdown` dropped to about `139 kB` after the same markdown optimization.
- Knowledge map pages are now split into dedicated `MindMap` chunks instead of coupling the async component code directly into the page modules.
- Both apps still emit large chunk warnings, which means the next optimization wave should focus on dependency-level lazy loading and Element Plus / visualization trimming rather than superficial CSS work.

## Suggested Next Execution Order

1. Route and navigation schema unification
2. Element Plus on-demand optimization
3. Visualization / chart libraries (`echarts`, G6 editing surfaces) deeper split and demand loading
4. Homepage and cockpit loading skeletons
5. CI bundle analysis and smoke tests
