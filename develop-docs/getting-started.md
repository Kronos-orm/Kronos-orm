# Kronos-ORM Download, Build and Run Guide (English)

This guide helps you clone the repository locally, use the correct JDK and Gradle to build the project, and run tests across all modules (including integration tests).

> Prerequisites
> - JDK 17 or newer is required to build and run with Gradle (CI uses JDK 21).
> - Before running integration tests in the `kronos-testing` module, prepare local databases and set required environment variables:
>   - MySQL: set `MYSQL_USERNAME` and `MYSQL_PASSWORD`
>   - PostgreSQL: set `POSTGRES_USERNAME` and (recommended) `POSTGRES_PASSWORD`
>
> Note: Kronos-ORM supports multiple databases, including MySQL, PostgreSQL, SQLite, Oracle, Microsoft SQL Server, DB2, Sybase, H2, OceanBase, DM8, and GaussDB. The integration test module currently demonstrates MySQL and PostgreSQL.

---

## 1. Clone the project

- Using Git:
  ```bash
  git clone https://github.com/<your-org-or-username>/Kronos-orm.git
  cd Kronos-orm
  ```
- Or download the Zip from GitHub/Gitee and extract it to the project root.

## 2. Install and configure JDK (17+)

- We recommend JDK 17, 21 or newer. You can use:
  - SDKMAN (macOS/Linux): `sdk install java 21.0.x-tem` or `sdk install java 17.0.x-tem`
  - Homebrew (macOS): `brew install openjdk@21`
  - Windows: download and install from Adoptium/Oracle.

- Verify on command line:
  ```bash
  java -version
  # Should print 17 or newer, such as openjdk version "21..."
  ```

- Ensure Gradle Wrapper uses that JDK (wrapper usually inherits current JAVA_HOME):
  - Ensure `JAVA_HOME` points to 17+:
    - macOS/Linux: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` (or 17)
    - Windows PowerShell: `$env:JAVA_HOME = 'C:\\Program Files\\Java\\jdk-21'`
  - In IntelliJ IDEA:
    - File > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK: choose 17+.

## 3. Build with Gradle

The project ships with Gradle Wrapper (`./gradlew`). You don’t need a local Gradle installation.

- Build all modules:
  ```bash
  ./gradlew clean build
  ```

- Run unit tests (all modules):
  ```bash
  ./gradlew test
  ```

- Build a single module (examples):
  ```bash
  ./gradlew :kronos-core:build
  ./gradlew :kronos-logging:build
  ```

> If your network is in mainland China, the first build may be slow. Consider configuring mirror repos in `gradle.properties` or your local Gradle init script to speed up dependency resolution.

## 4. Run all tests in the project

- With JDK 17+ in place, run:
  ```bash
  ./gradlew test
  ```
- To include full checks (compile, test, verification):
  ```bash
  ./gradlew build --info --stacktrace
  ```

## 5. Run `kronos-testing` integration tests

The `kronos-testing` module contains integration tests requiring a real database. Before running:

1) Prepare databases (locally installed or via Docker)

- MySQL 8.x (Docker example):
  ```bash
  docker run --name kronos-mysql -e MYSQL_ROOT_PASSWORD=root \
    -e MYSQL_DATABASE=kronos_testing \
    -e MYSQL_USER=kronos -e MYSQL_PASSWORD=your_mysql_pwd \
    -p 3306:3306 -d mysql:8.0
  ```

- PostgreSQL 17 (Docker example):
  ```bash
  docker run --name kronos-postgres \
    -e POSTGRES_PASSWORD=your_pg_pwd \
    -e POSTGRES_DB=kronos_testing \
    -e POSTGRES_USER=kronos \
    -p 5432:5432 -d postgres:17
  ```

> You can also use locally installed databases. Just ensure there is a user named `kronos` and a database named `kronos_testing`, or adjust to match your actual configuration.

2) Set required environment variables (for the test process to access DB)

- bash/zsh (macOS/Linux):
  ```bash
  export MYSQL_USERNAME=kronos
  export MYSQL_PASSWORD=your_mysql_pwd
  export POSTGRES_USERNAME=kronos
  export POSTGRES_PASSWORD=your_pg_pwd   # recommended
  ```

- PowerShell (Windows):
  ```powershell
  $env:MYSQL_USERNAME = 'kronos'
  $env:MYSQL_PASSWORD = 'your_mysql_pwd'
  $env:POSTGRES_USERNAME = 'kronos'
  $env:POSTGRES_PASSWORD = 'your_pg_pwd'  # recommended
  ```

3) Run integration tests

```bash
./gradlew :kronos-testing:test --info --stacktrace
```

> In CI, we export `MYSQL_USERNAME=kronos` and `POSTGRES_USERNAME=kronos` then run the same command; locally you should provide corresponding password variables to connect.

## 6. FAQ

- Issue: Gradle reports JDK version too low or mismatch.
  - Fix: Ensure `java -version` >= 17 and Gradle JDK in your IDE is set to 17 or newer.
- Issue: Cannot connect to database / authentication failed.
  - Fix: Ensure containers or local services are up, ports open (MySQL 3306 / PostgreSQL 5432), and environment variables are set correctly.
- Issue: Port already in use.
  - Fix: Check if another DB instance is running on the same port. Change Docker mapped ports or stop the conflicting process.
- Issue: First build is slow.
  - Fix: Configure mirror repos to speed up dependency downloads.

## 7. References

- CI snippet: use JDK 21, export `MYSQL_USERNAME` and `POSTGRES_USERNAME`, then run `:kronos-testing:test`.
- Related modules: `kronos-core`, `kronos-logging`, `kronos-jdbc-wrapper`, `kronos-codegen`, `kronos-compiler-plugin`, `kronos-testing`.
