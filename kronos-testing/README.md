# kronos-testing

Integration test module for Kronos ORM. Tests run against real database instances.

## Supported Databases

- MySQL 8.0
- PostgreSQL 17
- SQLite
- Oracle
- SQL Server 2022

## Environment Variables

| Variable | Default (CI) |
|----------|-------------|
| `MYSQL_USERNAME` | `kronos` |
| `MYSQL_PASSWORD` | (empty) |
| `POSTGRES_USERNAME` | `postgres` |
| `POSTGRES_PASSWORD` | (empty) |

## Running

```bash
# Set up env vars
source envsetup.sh

# Run all integration tests
./gradlew :kronos-testing:test --info --stacktrace

# Or use the test script (checks DB connectivity first)
./test.sh
```

## CI

`kronos-testing.yml` workflow spins up MySQL, PostgreSQL, and SQL Server via `ankane/setup-*` GitHub Actions.
