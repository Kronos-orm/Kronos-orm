{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos通过{{ $.keyword("database/datasource-wrapper", ["数据源包装器"]) }}执行数据库操作。默认连接直接使用`Kronos.connect(...)`配置。

## 使用JDBC URL连接

在应用启动时调用`Kronos.connect(...)`配置默认数据源。

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

该调用返回已注册的`KronosJdbcWrapper`。

## 添加JDBC依赖

添加`kronos-jdbc-wrapper`和数据库对应的JDBC Driver。

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

## 使用连接池

应用服务使用连接池时，添加与数据库服务端、JDK匹配的连接池和JDBC Driver。

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
> 将`<latest-stable>`或Maven property替换为连接池和Driver维护者发布的最新稳定版。

## 生产连接检查表

把 wrapper 赋值给 `Kronos.dataSource` 前，先按应用直接使用 JDBC 时的生产要求配置 `DataSource`。

| 项目 | 检查内容 |
|------|----------|
| 连接池大小 | 按应用并发和数据库连接上限设置初始、空闲和最大连接数。 |
| 超时 | 在连接池或 JDBC Driver 中配置连接、socket 或 network、查询和空闲超时。 |
| 连接校验 | 启用 validation query 或 JDBC validation method，并检查借出连接和空闲连接校验配置。 |
| SSL 和证书 | 按数据库服务端要求设置 JDBC URL 或 driver property，包括 TLS 模式、trust store 和证书校验。 |
| 密钥 | 从部署环境的 secret manager 或环境变量读取用户名、密码和证书路径。 |
| 时区和编码 | 在 driver 需要时设置 server time zone、Unicode 和 character encoding 等 JDBC URL 参数。 |
| Driver 版本 | 选择支持当前数据库服务端版本、JDK、认证方式和 TLS 能力的稳定 driver 分支。 |

## 配置MySQL

创建`BasicDataSource`，用`KronosJdbcWrapper`包装，并赋值给`Kronos.dataSource`。

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

## 配置PostgreSQL

使用PostgreSQL JDBC Driver类名和PostgreSQL JDBC URL。

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

## 配置SQLite

SQLite在JDBC URL中使用文件路径。

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

## 配置H2

H2 可以使用内存 JDBC URL 进行本地开发和测试。

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

## 配置SQL Server

使用SQL Server JDBC Driver，并按数据库服务端要求设置加密参数。

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

## 配置Oracle

使用Oracle JDBC Driver，并填写数据库使用的service name或PDB URL。

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

## 配置DM8（达梦）

使用 DM8 JDBC Driver 和 `DBType.DM8`。

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

## 检查当前数据库

`KronosJdbcWrapper`读取JDBC元信息，并提供`dbType`和`sqlDialect`。

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

方言行为、SQL渲染和数据库支持示例见{{ $.keyword("database/dialect-support", ["数据库支持"]) }}。
