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

Integration defaults live in `scripts/common.main.kts`. Kotlin build and CI commands load them automatically. Set local overrides in the ignored `kronos-testing/env.local.properties`; existing environment variables take precedence.
The bundled DM8 service uses `DM_USERNAME` and `DM_PASSWORD` for the test connection. `DM_SYSAUDITOR_PASSWORD` configures the service's SYSAUDITOR account. Override these values together with `DM_JDBC_URL` when connecting to an externally managed DM8 instance.

## Running

Run from the repository root:

```sh
# Start the database services, execute integration tests and stop the services
scripts/run ci integration

# Use database services that are already running
scripts/run build test -m kronos-testing -p jvm

# SQLite tests need no external database service
scripts/run build test -m kronos-testing -p jvm --include-classes='com.kotlinorm.integration.Sqlite*'
```

External database suites require their matching database service. SQLite and H2 run in-process.
The `integration` command uses `docker-compose.integration.yml` and prepares the SQL Server
integration database before running the tests.

## CI

`kronos-testing.yml` runs MySQL, PostgreSQL, SQL Server, Oracle, and DM8 in one required integration gate. SQLite and H2 run in the test JVM.
