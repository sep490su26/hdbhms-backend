# Main Stabilization Migration Plan

## Status and scope

- Branch under test: `stabilization/main-recovery-20260716`, based on backend `origin/main` at `038a95f`.
- Active development/staging Flyway location: `classpath:migration/dev`.
- The current main chain is V1-V31. No existing SQL migration has been edited during stabilization.
- `migration/devOld` is an archived pre-reconciliation chain and `migration/prod` contains supplemental seed scripts. Neither is part of the active V1-V31 location.
- Clean install is verified on a disposable MySQL 8.0 container. Legacy upgrade remains **BLOCKED** pending real read-only history and schema evidence.
- The new `prod` Spring profile must not be deployed until its intended Flyway location and the target database history have been approved. Falling back to Flyway's default location would not run the active chain.

## Inventory method

The table compares exact Git blobs from `origin/dev2`, `origin/dev`, and `origin/main`. A matching 12-character blob ID proves byte-identical SQL content; a different ID means the SQL changed, even when the logical filename is the same.

| Logical migration | dev2 filename (blob) | dev filename (blob) | main filename (blob) | Exact content | Note |
|---|---|---|---|---|---|
| initial schema after merge | V0 (fb6331bfa153) | V1 (fb6331bfa153) | V1 (fb6331bfa153) | Yes | Renumbered only |
| seed initial property and rules | V1 (bd59df988eee) | V2 (bd59df988eee) | V2 (bd59df988eee) | Yes | Renumbered only |
| reuse deleted floor/room codes | V2 (ae01bc235578) | V3 (ae01bc235578) | V3 (ae01bc235578) | Yes | Renumbered only |
| align floor plan items schema | V3 (0d7d1fd80a77) | V4 (0553a176be56) | V4 (0553a176be56) | **No** | Main replaces stored-procedure logic with conditional prepared SQL |
| add lease contract signed file | V4 (54942eba556f) | V5 (54942eba556f) | V5 (54942eba556f) | Yes | Renumbered only |
| create debt tracking tasks | V5 (786c9cbbecd1) | V6 (786c9cbbecd1) | V6 (786c9cbbecd1) | Yes | Renumbered only |
| seed room sample images | V6 (5b9cf33c069a) | V7 (5b9cf33c069a) | V7 (5b9cf33c069a) | Yes | Renumbered only |
| add positive difference settlement type | V7 (794983092803) | V8 (4570642e8f5f) | V8 (4570642e8f5f) | **No** | Main makes the DDL conditional/idempotent |
| add positive difference type to transfer request | V8 (c9fb88c9d595) | V9 (d7587ddcf0ca) | V9 (d7587ddcf0ca) | **No** | Main makes the DDL conditional/idempotent |
| align transfer request status enum | V9 (84ad41b1d1d8) | V10 (84ad41b1d1d8) | V10 (84ad41b1d1d8) | Yes | Renumbered only |
| add waiting transfer date status | V10 (f752dfe1202d) | V11 (f752dfe1202d) | V11 (f752dfe1202d) | Yes | Renumbered only |
| expand notification template channels | V11 (eeaf44f30a4b) | V12 (eeaf44f30a4b) | V12 (eeaf44f30a4b) | Yes | Renumbered only |
| seed holder nomination templates | V12 (878f4ff3ef4e) | V13 (878f4ff3ef4e) | V13 (878f4ff3ef4e) | Yes | Renumbered only |
| add transfer replacement old contract | V13 (08c6b3055643) | V14 (08c6b3055643) | V14 (08c6b3055643) | Yes | Renumbered only |
| add notification outbox read time | V14 (e604d0ca0169) | V15 (e604d0ca0169) | V15 (e604d0ca0169) | Yes | Renumbered only |
| add tenant profile access request | V15 (e8427365d663) | V16 (e8427365d663) | V16 (e8427365d663) | Yes | Renumbered only |
| merge permission requests into change requests | V16 (7a81bca68d94) | V17 (7a81bca68d94) | V17 (7a81bca68d94) | Yes | Renumbered only |
| add permission grants | V17 (f477bbc98ef4) | V18 (f477bbc98ef4) | V18 (f477bbc98ef4) | Yes | Renumbered only |
| add new request type | - | V19 (bc564ee775b8) | V19 (bc564ee775b8) | Yes | Added after dev2 |
| add deposit contact history | V18 (f932dc0adc75) | V20 (f932dc0adc75) | V20 (f932dc0adc75) | Yes | Renumbered only |
| seed floor 4/5 complete demo | V19 (eca551c61f17) | V21 (6bc5245f5bf6) | V21 (6bc5245f5bf6) | **No** | Main changes procedure naming/lifecycle and leaves it installed |
| normalize maintenance cost responsibility | - | V22 (66eb9e43c9a6) | V22 (66eb9e43c9a6) | Yes | Added after dev2 |
| deduplicate meter reading batches | - | V23 (e3ad54e7d642) | V23 (e3ad54e7d642) | Yes | Added after dev2 |
| add draft property/room status | - | V24 (582967bbf420) | V24 (582967bbf420) | Yes | Added after dev2 |
| add utility billing runs | - | V25 (b2b887d46627) | V25 (b2b887d46627) | Yes | Added after dev2 |
| normalize demo deposit codes | V20 (c2bcca757747) | V26 (c2bcca757747) | V26 (c2bcca757747) | Yes | Renumbered only |
| seed floor 4/5 demo scenarios | - | V27 (07d0790ef4a3) | V27 (07d0790ef4a3) | Yes | Added after dev2 |
| normalize meter reading period | V21 (f653e05f9db2) | V28 (f653e05f9db2) | V28 (f653e05f9db2) | Yes | Renumbered only |
| add expense approval flow | - | V29 (b180256d578d) | V29 (b180256d578d) | Yes | Added after dev2 |
| add room deposit failures | - | - | V30 (9ffacd17c2bb) | Yes | Main-only addition |
| seed current utility/payment flow | - | - | V31 (2a27742d630b) | Yes | Main-only addition |

## Clean-install path

Evidence captured on 2026-07-16:

1. Docker started a disposable `mysql:8.0` container with a new `hdbhms` database.
2. Flyway validated all 31 migrations.
3. The test migrated the empty schema to V8, inserted controlled legacy-shaped rows, then migrated V9-V31.
4. Repository/schema assertions passed: 3 tests, 0 failures, 0 errors, 0 skipped.
5. The container was removed by the test; no shared or production database was contacted.

Command:

```powershell
.\mvnw.cmd -Dtest=IdentityAccessNativeQueryRepositoryTest test
```

Result: **PASS for the main V1-V31 clean path and a main V8-to-V31 incremental path**. This is not evidence for a database whose history was created by dev2 V0-V21.

## Legacy-upgrade path (blocked)

Do not run current main migrations against a dev2-era database. Dev2 V0 maps to main V1, subsequent versions are shifted, and four logical scripts have different content. Flyway will see version/checksum mismatches; `repair`, history edits, or rewriting old SQL are prohibited.

Required read-only evidence from each legacy environment:

```sql
SELECT installed_rank, version, description, type, script, checksum,
       installed_by, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Also export a schema fingerprint for tables, columns, indexes, foreign keys, unique constraints, and enum/check constraints. Record environment identity and the commit/image that last migrated it. Secrets and row data are not required.

After evidence is supplied:

1. Restore a private snapshot into an isolated MySQL container/instance.
2. Compare every installed script/version/checksum to the inventory above.
3. Classify the physical schema, especially floor-plan columns, settlement-type columns, request enums, permissions, meter periods, and deposit locks.
4. Design a new reconciliation migration at an unused version greater than the approved deployed maximum. It must inspect actual schema state and make only additive/forward corrections.
5. Keep any legacy-only Flyway location separate from the clean V1-V31 location and guard it with an explicit deployment procedure; never place colliding versions in one location.
6. Test both a snapshot upgrade and a second startup for validation/idempotency before deployment approval.

Current legacy result: **BLOCKED - no authoritative `flyway_schema_history` export or schema fingerprint has been provided**.

## Preconditions and deployment controls

- Take a verified logical and physical backup before any legacy test or deployment.
- Confirm the exact application commit, Spring profile, Flyway location, JDBC target, current history, and available disk space.
- Require a maintenance window and stop concurrent writers for migration DDL.
- Run `flyway validate` before `migrate`; never run `repair`.
- Capture history, schema fingerprint, row-count sanity checks, and application health before and after.
- Start the backend with Hibernate schema validation and run critical API smoke tests after migration.

## Validation targets

- All Flyway rows are successful and checksums match the approved artifacts.
- Entity mappings match tables, columns, enum values, foreign keys, and uniqueness rules.
- Seed references are valid for property, floor, room, tenant, contract, invoice, debt, and meter-reading data.
- Deposit/room locking, transfer, permission grants, billing runs, notification outbox, and payment reconciliation retain expected constraints.
- Re-running application startup performs validation only and does not mutate data unexpectedly.

## Rollback strategy

MySQL DDL rollback is restore-based, not down-migration based:

1. Stop the application and preserve failure logs/history.
2. Do not edit `flyway_schema_history` and do not run `repair`.
3. Restore the pre-migration database backup into a new instance or restore the approved snapshot according to the DBA runbook.
4. Point the prior application release at the restored instance only after schema/history verification.
5. Preserve the failed database privately for root-cause analysis.

No production command is approved by this document. The final migration command, location, and reconciliation SQL remain subject to DBA/user review after legacy evidence is available.
