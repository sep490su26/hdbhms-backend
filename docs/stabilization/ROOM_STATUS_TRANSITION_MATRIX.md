# Room Status Transition Matrix

## Current enum and meaning

| Status | Meaning in current code | Public catalog | Lifecycle owner |
|---|---|---|---|
| `DRAFT` | Room/property setup is not operational yet. | No | Property/room setup |
| `VACANT` | Operational and available without an active commitment. | Yes | Property activation, deposit release, transfer/liquidation |
| `ON_HOLD` | Short payment-processing hold. | No | Deposit checkout/expiry |
| `RESERVED` | Deposit is confirmed or another deposit lifecycle commitment exists. | No | Deposit/payment lifecycle |
| `RESERVED_FOR_TRANSFER` | Capacity is reserved for an approved/in-progress transfer. | No | Transfer lifecycle |
| `OCCUPIED` | Active lease/occupancy. | No | Lease activation/renewal/transfer |
| `SOON_VACANT` | Active occupant intends to move/transfer; expected vacant date exists. | Yes, subject to date validation | Tenant intention/lease lifecycle |
| `MAINTENANCE` | Room is blocked for repair. | No | Owner operational action; exact manual rule needs BRD-08 |
| `EXPIRED` | Lease ended while the room has not completed liquidation/move-out. | No | Lease expiry lifecycle |

PRD calls the final state “Hết hạn HĐ”; current enum uses `EXPIRED`. `ON_HOLD` and `RESERVED_FOR_TRANSFER` are technical/business sub-states not shown in the simplified PRD color table.

## Implemented automatic transitions

| From | To | Trigger | Guard/invariant | Implementation evidence | Regression evidence |
|---|---|---|---|---|---|
| new room | `DRAFT` | Room creation | Entity/domain default | `Room`, `RoomEntity` | Property activation tests |
| `DRAFT` | `VACANT` | Property changes to active | Only DRAFT rooms without active lease/deposit commitments; idempotent | `PropertyController.updatePropertyStatus` explicit query | `PropertyActivationRoomStatusTest` 4/4 |
| `VACANT` or valid `SOON_VACANT` | `ON_HOLD` | Single/batch deposit checkout | Availability/date validation and active-lock uniqueness | `BookRoomService`, `DepositBatchCheckoutService` | Deposit locking/lifecycle suite in full gate |
| `ON_HOLD` | `RESERVED` | Successful reconciled deposit payment | Compare-and-set; hold confirmation and idempotent deposit completion | `DepositCompletionAdapter`, `DepositBatchCompletionAdapter` | Deposit completion tests in full gate |
| `ON_HOLD` | `VACANT` or `SOON_VACANT` | Cancel/expiry/payment failure | Compare-and-set; expected-vacant commitment decides restored status | `DepositController`, `DepositPaymentExpiryService`, `DepositBatchCheckoutService` | Deposit lifecycle tests in full gate |
| deposit-related state | `RESERVED` | Active deposit status update | Non-refunded/non-forfeited/non-converted agreement | `DepositAgreementController.updateRoomStatusForDepositStatus` | Deposit lifecycle tests |
| `RESERVED`/`VACANT`/`ON_HOLD` | `OCCUPIED` | Lease activation | Signed/activation conditions; renewal has separate guards | `LeaseContractManagementService.activate` | Contract activation tests in full gate |
| `OCCUPIED` | `SOON_VACANT` | Tenant records `MOVE_OUT` or `TRANSFER` intention | Authenticated owner/primary tenant, active contract, valid date/reason, duplicate rejection | `LeaseContractManagementService.recordTenantIntention` | `LeaseContractTerminationTest` 6/6 |
| `SOON_VACANT` | `OCCUPIED` | Tenant chooses renewal/renewed contract activates | Renewal blockers checked | `LeaseContractManagementService` | Renewal tests in full gate |
| `OCCUPIED` | `EXPIRED` | Contract end date passes without valid renewal | Only current `OCCUPIED` is changed | `LeaseContractLifecycleService` | Lifecycle tests in full gate |
| `EXPIRED` or `OCCUPIED` | `OCCUPIED` | Valid renewal activation | Linked renewal contract and activation guards | `LeaseContractManagementService` | Renewal/activation tests |
| lifecycle room state | `VACANT` | Completed lease liquidation | Debt/handover/liquidation workflow completes | `LeaseContractManagementService.liquidate` | Liquidation tests in full gate |
| `VACANT` | `RESERVED_FOR_TRANSFER` | Transfer reserves target capacity | Target validation, capacity and reservation expiry | `RoomTransferService.reserveTargetCapacity` | Transfer tests in full gate |
| `RESERVED_FOR_TRANSFER` | `VACANT` | Transfer reservation expires/cancels with no other reserved slots/occupants | Re-count active reservations and occupants | `RoomTransferService.release*Reservation*` | Transfer tests in full gate |
| source lifecycle state | `VACANT` | All occupants transfer out | No remaining occupants | `RoomTransferService.execute*Transfer` | Transfer tests in full gate |
| target allowed state | `OCCUPIED` | Transfer-in execution completes | Signed/new or existing contract rules and handover | `RoomTransferService.execute*Transfer` | Transfer tests in full gate |

## Property activation safety rule

Property activation must never infer vacancy merely from “no active contract.” The stabilization query changes only `DRAFT` rooms to `VACANT`, and excludes rooms with active lease or deposit commitments. `VACANT`, `ON_HOLD`, `RESERVED`, `RESERVED_FOR_TRANSFER`, `OCCUPIED`, `SOON_VACANT`, `MAINTENANCE`, and `EXPIRED` are preserved.

## Known gap requiring product decision

`PUT /api/v1/rooms/{roomId}` is Owner-only but currently accepts `currentStatus` and can bypass the workflow matrix. The technical stabilization did not invent a manual transition policy. BRD-08 must be approved, then the generic update should reject lifecycle-managed transitions and route allowed status changes through a guarded, audited operation.
