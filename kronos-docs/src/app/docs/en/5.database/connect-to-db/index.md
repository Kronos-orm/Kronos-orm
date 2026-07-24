{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos executes database operations through {{ $.keyword("database/datasource-wrapper", ["Data source wrapper"]) }}. Use `Kronos.connect(...)` for the default connection.

## Connect with a JDBC URL

Call `Kronos.connect(...)` during application startup to configure the default data source.

```kotlin group="Direct JDBC connection" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.connect

Kronos.connect(
    url = "jdbc:mysql://localhost:3306/kronos?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC",
    userName = "root",
    password = "******",
    driverClassName = "com.mysql.cj.jdbc.Driver"
)
```

The call returns the registered `KronosJdbcWrapper`.

## Add JDBC dependencies

Add `kronos-jdbc-wrapper` and the JDBC driver for the database.

```kotlin group="JDBC dependencies" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

```groovy group="JDBC dependencies" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}'
    implementation 'com.mysql:mysql-connector-j:<latest-stable>'
}
```

```xml group="JDBC dependencies" name="maven" icon="maven"
<dependencies>
    <dependency>
        <groupId>com.kotlinorm</groupId>
        <artifactId>kronos-jdbc-wrapper</artifactId>
        <version>{{ $.kronosVersion() }}</version>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>${mysql-connector-j.version}</version>
    </dependency>
</dependencies>
```

## Use a connection pool

For application services, add a connection pool and the JDBC driver that match the database server and JDK.

```kotlin group="Driver" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:<latest-stable>")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

```xml group="Driver" name="maven" icon="maven"
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
    <version>${commons-dbcp2.version}</version>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>${mysql-connector-j.version}</version>
</dependency>
```

> **Note**
> Replace `<latest-stable>` or the Maven properties with the current stable version from the connection pool and driver maintainers.

## Production connection checklist

Before assigning the wrapper to `Kronos.dataSource`, configure the `DataSource` with the same production settings your application uses for direct JDBC access.

| Item | What to check |
|------|---------------|
| Pool size | Set the initial, idle, and max pool size for the application's request concurrency and database limits. |
| Timeouts | Configure connection, socket or network, query, and idle timeout values in the pool or JDBC driver. |
| Validation | Enable a validation query or JDBC validation method, and test both borrow-time and idle validation settings. |
| SSL and certificates | Use the JDBC URL or driver properties required by the server, including TLS mode, trust store, and certificate validation. |
| Secrets | Load username, password, and certificate paths from the deployment secret manager or environment. |
| Time zone and encoding | Set JDBC URL options such as server time zone, Unicode, and character encoding where the driver requires them. |
| Driver version | Pick the stable driver line that supports your database server version, JDK, and enabled authentication or TLS features. |

## Configure MySQL

Create a `BasicDataSource`, wrap it with `KronosJdbcWrapper`, and assign it to `Kronos.dataSource`.

```kotlin group="MySQL" name="MysqlKronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper by lazy {
    KronosJdbcWrapper(
        BasicDataSource().apply {
            driverClassName = "com.mysql.cj.jdbc.Driver"
            url = "jdbc:mysql://localhost:3306/kronos?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC"
            username = "root"
            password = "******"
        }
    )
}

Kronos.dataSource = { wrapper }
```

```kotlin group="MySQL" name="Driver coordinate" icon="gradlekts"
dependencies {
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

## Configure PostgreSQL

Use the PostgreSQL JDBC driver class and a PostgreSQL JDBC URL.

```kotlin group="PostgreSQL" name="PostgreSQLKronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper by lazy {
    KronosJdbcWrapper(
        BasicDataSource().apply {
            driverClassName = "org.postgresql.Driver"
            url = "jdbc:postgresql://localhost:5432/kronos"
            username = "postgres"
            password = "******"
        }
    )
}

Kronos.dataSource = { wrapper }
```

```kotlin group="PostgreSQL" name="Driver coordinate" icon="gradlekts"
dependencies {
    implementation("org.postgresql:postgresql:<latest-stable>")
}
```

## Configure SQLite

SQLite uses a file path in the JDBC URL and does not require a username or password.

```kotlin group="SQLite" name="SQLiteKronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper by lazy {
    KronosJdbcWrapper(
        BasicDataSource().apply {
            driverClassName = "org.sqlite.JDBC"
            url = "jdbc:sqlite:/path/to/kronos.db"
        }
    )
}

Kronos.dataSource = { wrapper }
```

```kotlin group="SQLite" name="Driver coordinate" icon="gradlekts"
dependencies {
    implementation("org.xerial:sqlite-jdbc:<latest-stable>")
}
```

## Configure H2

H2 can use an in-memory JDBC URL for local development and tests.

```kotlin group="H2" name="H2KronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper by lazy {
    KronosJdbcWrapper(
        BasicDataSource().apply {
            driverClassName = "org.h2.Driver"
            url = "jdbc:h2:mem:kronos;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
        }
    )
}

Kronos.dataSource = { wrapper }
```

```kotlin group="H2" name="Driver coordinate" icon="gradlekts"
dependencies {
    implementation("com.h2database:h2:<latest-stable>")
}
```

## Configure SQL Server

Use the SQL Server JDBC driver and include the encryption options required by your server.

```kotlin group="SQL Server" name="SQLServerKronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper by lazy {
    KronosJdbcWrapper(
        BasicDataSource().apply {
            driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
            url = "jdbc:sqlserver://localhost:1433;databaseName=kronos;encrypt=true;trustServerCertificate=true"
            username = "sa"
            password = "******"
        }
    )
}

Kronos.dataSource = { wrapper }
```

```kotlin group="SQL Server" name="Driver coordinate" icon="gradlekts"
dependencies {
    implementation("com.microsoft.sqlserver:mssql-jdbc:<latest-stable>")
}
```

## Configure Oracle

Use the Oracle JDBC driver and the service name or PDB URL used by your database.

```kotlin group="Oracle" name="OracleKronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper by lazy {
    KronosJdbcWrapper(
        BasicDataSource().apply {
            driverClassName = "oracle.jdbc.OracleDriver"
            url = "jdbc:oracle:thin:@localhost:1521/FREEPDB1"
            username = "system"
            password = "******"
        }
    )
}

Kronos.dataSource = { wrapper }
```

```kotlin group="Oracle" name="Driver coordinate" icon="gradlekts"
dependencies {
    implementation("com.oracle.database.jdbc:ojdbc8:<latest-stable>")
}
```

## Configure DM8 (Dameng)

Use the DM8 JDBC driver and `DBType.DM8`.

```kotlin group="DM8" name="Dm8KronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.enums.DBType
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper by lazy {
    KronosJdbcWrapper(
        dataSource = BasicDataSource().apply {
            driverClassName = "dm.jdbc.driver.DmDriver"
            url = "jdbc:dm://localhost:5236"
            username = "SYSDBA"
            password = "******"
        },
        databaseType = DBType.DM8
    )
}

Kronos.dataSource = { wrapper }
```

```kotlin group="DM8" name="Driver coordinate" icon="gradlekts"
dependencies {
    implementation("com.dameng:DmJdbcDriver8:<latest-stable>")
}
```

## Check the active database

`KronosJdbcWrapper` reads the JDBC metadata and exposes `dbType` and `sqlDialect`.

```kotlin group="Check" name="kotlin" icon="kotlin"
println(wrapper.url)
println(wrapper.userName)
println(wrapper.dbType)
println(wrapper.sqlDialect.family)
```

```text group="Check" name="output"
jdbc:mysql://localhost:3306/kronos
root@localhost
Mysql
MySql
```

For dialect behavior, SQL rendering, and database support examples, see {{ $.keyword("database/dialect-support", ["Database Support"]) }}.
