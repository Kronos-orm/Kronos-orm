# DM8 Native-Mode Dialect Support

## Confirmed Behavior

- In DM8 native mode (`COMPATIBLE_MODE=0`), `GENERATED ALWAYS AS IDENTITY` is rejected. Render `@PrimaryKey(identity = true)` as `INT IDENTITY(1,1)` or `BIGINT IDENTITY(1,1)` instead.
- DM8 creates generated system indexes named with the `INDEX` prefix. Filter generated indexes from the Oracle-compatible index metadata query, while retaining generated indexes that back a `UNIQUE` constraint so codegen and schema sync keep the logical constraint.
- DM8 JDBC drivers may return `0` for `prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)`. Request identity keys with `prepareStatement(sql, arrayOf(columnName))` for DM8 inserts.
- Query, pagination, `MERGE`, functions, and metadata use the Oracle-compatible dialect. DM8 retains a dedicated `DatabaseStatements` implementation for native identity DDL and metadata adjustments.
- DM8 follows the regular external integration-test path: Gradle discovers the suite without a DM8-specific exclusion, Compose starts it without a profile, and CI uses the public Docker-image defaults. A local installation can override the tracked defaults through ignored local environment settings.

## Verification

- `DatabaseStatementsTest` covers the `DBType.DM8` route and native identity DDL.
- `KronosArgumentsTest` covers the DM8 generated-column JDBC overload.
- The local DM8 native-mode integration run passes all 97 tests, including cascade identity and codegen metadata.
- DM8 codegen integration cleanup must close the dynamically created JDBC pool before dropping the metadata fixture table.
- The Compose healthcheck passes the connection string as a `disql` argument and checks the returned `1`. The multi-architecture `liuys36/dameng` image accepts its configured SYSDBA password and removes the QEMU dependency from the Linux CI runner.
