# kronos-testing

Integration test module for Kronos ORM. Default tests cover external database instances and in-process JDBC databases through `com.kotlinorm.integration.*`.

## Supported Databases

- MySQL 8.0
- PostgreSQL 17
- SQL Server 2022
- Oracle Free
- DM8 (Dameng)
- H2 through JDBC (in-memory by default)
- SQLite through JDBC

## Environment Variables

| Variable | Default |
|----------|---------|
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/kronos_testing?...` |
| `MYSQL_USERNAME` | `kronos` |
| `MYSQL_PASSWORD` | `kronos` through local Docker, empty in CI |
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/kronos_testing` |
| `POSTGRES_USERNAME` | `kronos` |
| `POSTGRES_PASSWORD` | empty |
| `SQLSERVER_JDBC_URL` | `jdbc:sqlserver://localhost:1433;databaseName=kronos_testing;encrypt=true;trustServerCertificate=true` |
| `SQLSERVER_USERNAME` | `SA` |
| `SQLSERVER_PASSWORD` | `YourStrong!Passw0rd` |
| `ORACLE_JDBC_URL` | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |
| `ORACLE_USERNAME` | `kronos` |
| `ORACLE_PASSWORD` | `KronosPassw0rd1` |
| `DM_JDBC_URL` | `jdbc:dm://localhost:5237` |
| `DM_USERNAME` | `SYSDBA` |
| `DM_PASSWORD` | `DMdba_123` |
| `DM_SYSAUDITOR_PASSWORD` | `DMAuditor_123` |
| `H2_JDBC_URL` | `jdbc:h2:mem:kronos_testing;DB_CLOSE_DELAY=-1` |
| `H2_USERNAME` | `sa` |
| `H2_PASSWORD` | empty |
| `SQLITE_URL` | Temp-file SQLite database |

Local defaults live in the tracked `envsetup.defaults` file. `envsetup.sh` and `envsetup.bat` load that file and keep any environment variable you set before calling them. Put local overrides in the ignored `envsetup.local.properties`, or set them in the environment before loading the defaults.

The bundled DM8 service uses `DM_USERNAME` and `DM_PASSWORD` for the test connection. `DM_SYSAUDITOR_PASSWORD` configures the service's SYSAUDITOR account. Override these values together with `DM_JDBC_URL` when connecting to an externally managed DM8 instance.

## Running

```bash
# Set up env vars
source envsetup.sh

# Run all integration tests
./gradlew :kronos-testing:test --info --stacktrace

# Or use the test script (checks DB connectivity first)
./test.sh
```

External database suites require their matching database service. SQLite and H2 run in-process and require no Compose service.

For fast local feedback without external services:

```bash
./gradlew :kronos-testing:test --tests "com.kotlinorm.integration.SqliteIntegrationTest" --info --stacktrace
```

To start local database services with Docker:

```bash
source envsetup.sh
docker compose -f kronos-testing/docker-compose.integration.yml up -d --wait
docker exec kronos-testing-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U "${SQLSERVER_USERNAME}" -P "${SQLSERVER_PASSWORD}" -C -Q "IF DB_ID('${SQLSERVER_DATABASE}') IS NULL CREATE DATABASE ${SQLSERVER_DATABASE}"
./gradlew :kronos-testing:test --info --stacktrace
docker compose -f kronos-testing/docker-compose.integration.yml down
```

`./test.sh` and `test.bat` run the Docker compose environment automatically, then run integration, core, syntax, codegen, and compiler-plugin tests.

## CI

`kronos-testing.yml` runs MySQL, PostgreSQL, SQL Server, Oracle, and DM8 in one required integration gate. SQLite and H2 run in the test JVM.
