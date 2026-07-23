# DM8 Native-Mode Dialect Support

## Confirmed Behavior

- In DM8 native mode (`COMPATIBLE_MODE=0`), `GENERATED ALWAYS AS IDENTITY` is rejected. Render `@PrimaryKey(identity = true)` as `INT IDENTITY(1,1)` or `BIGINT IDENTITY(1,1)` instead.
- DM8 creates system indexes named with the `INDEX` prefix. Exclude `INDEX%` from the Oracle-compatible index metadata query so schema sync does not treat them as user indexes.
- Query, pagination, `MERGE`, functions, and metadata use the Oracle-compatible dialect. DM8 retains a dedicated `DatabaseStatements` implementation for native identity DDL and metadata adjustments.
- DM8 follows the regular external integration-test path: Gradle discovers the suite without a DM8-specific exclusion, Compose starts it without a profile, and CI uses the public Docker-image defaults. A local installation can override the tracked defaults through ignored local environment settings.

## Verification

- `DatabaseStatementsTest` covers the `DBType.DM8` route and native identity DDL.
- The local DM8 native-mode integration run completed 20 test classes and 85 tests.
