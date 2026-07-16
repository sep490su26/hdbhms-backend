# Semantic Reconciliation

## Method

- Frontend merge `6c9facb`: parent 1 is the dev line at `6e1fe59`; parent 2 is dev2 at `8957d9d`.
- Backend merge `52e9868`: parent 1 is the dev line at `2fa808a`; parent 2 is dev2 at `f57acc8`.
- The authoritative conflict list below comes from each merge commit message. `git show --cc --name-only` is also reviewed, but that command shows combined deltas, not the original conflict list.
- Each result was compared by Git blob. Frontend resolved 14 files exactly to dev2 and 8 exactly to dev; backend resolved all four listed conflicts exactly to dev. Later main commits and current stabilization fixes were then reviewed.

## Frontend conflict files from `6c9facb`

| File | Logic dev | Logic dev2 | Main hiện tại | Logic còn thiếu / quyết định | Forward-fix | Test/evidence |
|---|---|---|---|---|---|---|
| `app/dashboard/accounts/page.jsx` | Older account entry wrapper | Newer account-management screen | dev2 | No unique business operation from dev is absent | None | Full frontend test/build gate |
| `app/dashboard/billing/history/page.jsx` | Earlier billing history | Newer paged history | dev2 plus `3c8e174` | No missing API operation found | None | Billing/service tests, build |
| `app/dashboard/billing/page.jsx` | Earlier billing screen | Newer invoice workflow | dev2 plus `3c8e174` | No missing API operation found | None | Billing tests, build |
| `ContractActivationFlow.jsx` | Older handover visibility state | Newer integrated workflow/handover view | dev2 | Older `showHandover` is superseded by `activeView`; no flow loss | None | `contractActivationPopup.test.mjs` |
| `ContractHandoverSection.jsx` | Local fixed asset template | Newer normalized handover asset helpers | dev2 | Template behavior is represented by shared asset-state code | None | `contractHandoverAssets.test.mjs` |
| `ContractPrintWizard.jsx` | Older generic input helper | Newer print/upload workflow | dev2 | No contract field/API operation lost | None | Contract workflow tests, build |
| `ContractWorkflowStepper.jsx` | Older six-step presentation | Newer two-stage activation workflow | dev2 | Older visual state machine is superseded; signed handover remains optional by current requirement/test | None | Activation/workflow/document-state tests |
| `contract-template/page.jsx` | Mixed deposit/lease table and older stepper flags | Newer lease activation management | dev2 plus `8c4d867` | Stale test still expected deposit type column after page became lease-only | Updated regression to assert lease-only semantic and nine columns | Focused test 9 assertions group PASS |
| `app/dashboard/debt/page.jsx` | Current debt page | Alternate branch version | dev | No unique named behavior from dev2 absent | None | Full test/build gate |
| `app/dashboard/deposits/page.jsx` | Earlier client-side agreement filtering | Newer server-backed deposit management | dev2 plus `3c8e174` | Old client pagination helpers are superseded | None | Deposit service/page tests |
| `FacilityFloorPlanDesigner.jsx` | Older canvas included local undo/redo/zoom/pan helpers | `53de87d` newer expanded canvas/data model | dev2 plus `8c4d867` | Business layout/create/update/save flow is present. Older undo/redo/zoom/pan UX was not carried into the newer design and has no PRD/SRS acceptance rule; track as residual UX, not silently reinsert | None pending UX decision | Floor-plan canvas tests and build |
| `app/dashboard/finance/page.jsx` | Current finance page | Older local filtered cost presentation | dev plus `3c8e174` | No missing finance API operation found | None | Finance tests, build |
| `app/dashboard/lease-contracts/page.jsx` | Misplaced/duplicated deposit-management implementation | Actual lease-contract screen | dev2 | Choosing dev2 prevents deposit UI from replacing lease UI | None | Lease/contract tests, build |
| `app/dashboard/maintenance/page.jsx` | Current maintenance workflow | Alternate branch page | dev plus `3c8e174` | No unique API operation from dev2 absent | None | Maintenance tests, build |
| `app/dashboard/meter-readings/batch/page.jsx` | Real batch API workflow | Version containing sample/mock photos | dev plus stabilization photo fix | Mock photos deliberately rejected; authenticated file blobs retained | `a58b8c6` | Meter photo component/service tests |
| `app/dashboard/meter-readings/page.jsx` | Current real meter history | Alternate navigation/history presentation | dev plus `3c8e174` | No meter API operation from dev2 absent | None | Meter service/history tests |
| `app/dashboard/page.jsx` | Current dashboard data flow | Alternate summary UI | dev | No unique backend operation absent | None | Dashboard build/tests |
| `app/dashboard/requests/page.jsx` | Earlier request page | Newer request workflow | dev2 plus `3c8e174` | No request action from dev absent after follow-up | None | Request/service tests, build |
| `app/dashboard/rooms/page.jsx` | Current room management | Alternate branch page | dev plus `8c4d867` | No room operation from dev2 absent | None | Room service tests, build |
| `app/dashboard/tenants/page.jsx` | Earlier tenant screen | Newer permission-aware tenant screen | dev2 plus `3c8e174` | UI collected access reason but service discarded it | Forwarded trimmed `reason` in access-request body | `tenantProfilesService.test.mjs` PASS |
| `ViewingCustomersClient.jsx` | Version still carrying mock viewing records | Real viewing-customer service workflow | dev2 plus `3c8e174` | Mock data deliberately rejected | None | Service usage review, build |
| `DashboardPagination.jsx` | Older pagination calculation | Newer standardized dashboard pagination | dev2 | Current callers/tests align with shared one-based contract | None | Pagination tests |

## Frontend combined-delta files

These files were changed relative to both parents even though they are not all in the original conflict-message list.

| File | Reconciled behavior | Missing behavior found | Action/test |
|---|---|---|---|
| `app/dashboard/_lib/rbac.js` | Facilities remain Owner-only; finance is Owner/Accountant according to later feedback | None | RBAC source/tests and build |
| `app/rules/RulesClient.jsx` | Public active property rules catalog | Error-state retry was dropped | Restored selected-property retry; focused source test PASS |
| `services/contractHandoverService.test.mjs` | Handover service contract retained | None | Full node test gate |
| `services/depositContractsService.js` | Management and guest paid-contract flows retained | Numeric guest resource access needed a capability | Capability header added; test PASS |
| `services/identityAccessService.js` | Auth/refresh/role behavior from both lines retained | None | Identity service tests |
| `services/leaseContractsService.js` | Lease activation/handover endpoints retained | None | Lease service tests |
| `services/meterReadingService.js` | Batch/read/upload/photo methods retained | None | Meter service tests |
| `services/tenantProfilesService.js` | Permission request flow retained | Reason payload missing | Fixed and tested |

## Backend conflict files from `52e9868`

All four conflict-message files were resolved to dev. A method-level comparison found no method that existed only in dev2 and is absent from current main.

| File | Logic dev | Logic dev2 | Main hiện tại | Logic còn thiếu | Forward-fix/test |
|---|---|---|---|---|---|
| `GetBatchMeterReadingsService.java` | Batch selection, period dedupe, property/tariff data and warnings | Simpler batch retrieval | dev | No dev2-only method absent; dev has additional dedupe/warning behavior | Meter batch mapper/service tests and full backend gate |
| `GetMeterReadingsService.java` | Current list/latest mapping | Alternate branch implementation | dev plus `0beea80` | No dev2-only method absent | Meter tests/full gate |
| `MeterReadingPeriod.java` | Accepts month-year, slash and legacy year-month; emits `MM-yyyy` | Equivalent input formats with different helper names | dev plus `0beea80` | None | `MeterReadingPeriodTest` |
| `SubmitMeterReadingService.java` | Progressive save, anomaly detection, previous usage and room validation | Simpler submission path | dev | No dev2-only method absent; dev contains additional safeguards | Meter service tests/full gate |

## Backend combined-delta files

| File | Reconciled behavior | Stabilization result |
|---|---|---|
| `SecurityConfig.java` | Merge selected public API wildcards, overriding protected behavior | Forward-fixed in `e7dcda2`; explicit public routes, method roles, fail-closed CORS, and deposit capabilities now guard guest follow-up operations |
| `MeterReadingBatchEntity.java` | Unique property/period constraint retained | Kept; canonical period normalization prevents duplicate logical periods |
| `JpaMeterReadingRepository.java` | Both branch query families retained | Later `3fe0fca` safely replaced latest-reading query while preserving callers |
| `MeterReadingController.java` | Lease-overlap batch semantics and `MM-yyyy` retained | No missing operation found; verified by meter tests/full gate |

## Residual semantic controls

- The generic Owner room update can still assign lifecycle-managed status directly; this is documented as BRD-08 and must not be silently decided in stabilization.
- Legacy database compatibility is not inferred from code reconciliation. It remains blocked by the migration plan until authoritative history/schema evidence exists.
