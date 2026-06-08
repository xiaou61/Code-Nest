# UI Theme Refactor Closeout And Package Readiness

> Date: 2026-06-06  
> Scope: `vue3-admin-front`, `vue3-user-front`  
> Reference: `AI-DOCS/Development/UI-theme-refactor-reference-and-plan.md`

## Current State

The local design-system route is complete for the current refactor stage.

| Area | Admin | User |
| --- | --- | --- |
| `src/views` design-system coverage | `True: 74`, `False: 0` | `True: 113`, `False: 0` |
| All non-design-system Vue coverage | `76/76` | `117/117` |
| Production build | Passes | Passes |
| Non-token/theme static style scan | Clean | Clean |
| Local `src/design-system` | Present | Present |
| Theme CSS for declared theme modes | Present | Present |
| Demo page | Present | Present |
| Global navigation components | `CnSidebar`, `CnTopNav`, `CnCommandPalette` present | `CnSidebar`, `CnTopNav`, `CnCommandPalette` present |

The remaining raw hex values are intentionally confined to token/theme source files. They are not present in page/shared implementation code under the configured scan.

## Implemented Foundation

- Theme tokens, theme CSS, Element Plus bridge, and `useTheme`.
- App/page components: `CnPage`, `CnPageHeader`, `CnSection`.
- Data/status components: `CnStatCard`, `CnStatusTag`, `CnEmptyState`, `CnToolbar`, `CnFilterForm`, `CnDataTable`.
- Theme controls: `CnThemeSwitch`, `CnThemeDrawer`.
- Navigation foundation: `CnSidebar`, `CnSidebarItem`, `CnTopNav`, `CnCommandPalette`.
- Composables: `useCnBreakpoints`, `useCnTable`.

## Phase 7 Readiness

The original Phase 7 trigger conditions are mostly satisfied:

- Both apps have far more than 10 pages using the design-system.
- The component API has stabilized across broad admin and user migrations.
- Theme tokens are structurally aligned and the non-token implementation scan is clean.

However, the repository is not currently configured as a frontend workspace:

- There is no root `package.json`.
- There is no `pnpm-workspace.yaml` or equivalent workspace manifest.
- `vue3-admin-front` and `vue3-user-front` are independent frontend packages.
- Extracting `packages/code-nest-ui` would require package-manager, Vite alias, build output, type declaration, and import-path changes across both apps.

Recommendation: do not extract the shared package in the same slice as the migration closeout. Treat package extraction as a separate infrastructure change.

## If Phase 7 Starts

Use a separate branch/slice and keep it mechanical:

1. Add an explicit frontend workspace configuration at the repo root.
2. Create `packages/code-nest-ui` with the current admin/user design-system as the source of truth.
3. Add package build outputs for CSS and TypeScript declarations.
4. Replace app-local imports incrementally through a compatibility alias.
5. Keep app-specific shell consumers, such as admin layout menu data and user global navigation config, outside the package.
6. Build both frontends after each import-boundary change.

## Verification Snapshot

Last verified in this continuation:

- `npm run build` in `vue3-admin-front`: passed.
- `npm run build` in `vue3-user-front`: passed.
- Non-token/theme/plugin static scan: zero configured residual matches in both apps.
- View design-system coverage: admin `74/74`, user `113/113`.
- All non-design-system Vue coverage: admin `76/76`, user `117/117`.
- Navigation component equivalence: admin/user `CnTopNav.vue` and `CnCommandPalette.vue` are identical.
- User app shell: global top navigation and mobile drawer now render through `CnTopNav`.

No screenshot or browser QA was run because the user explicitly requested code/build-only continuation.

## 2026-06-07 Final Recheck

- All non-design-system Vue coverage remains complete: admin `76/76`, user `117/117`.
- Non-token/theme/plugin static style scan reports zero configured residual files in both apps.
- Cleaned the last two scan leftovers:
  - `vue3-admin-front/src/layout/index.vue`: replaced legacy `--cn-primary*` references with semantic brand tokens.
  - `vue3-user-front/src/views/interview/QuestionDetail.vue`: removed page-level Element Plus CSS variable override and used token-driven switch color.
- `npm run build` in both frontend apps passed after the cleanup.
