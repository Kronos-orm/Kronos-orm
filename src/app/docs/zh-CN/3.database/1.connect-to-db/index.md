{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos访问数据库通过{{ $.keyword("concept/kronos-data-source-wrapper", ["概念", "数据源包装器"]) }}实现，`KronosDataSourceWrapper`是一个接口，是对数据库操作的封装，它不关心具体的数据库连接细节，与平台无关，只关心数据库操作的逻辑。

> **Note**
> 同一个项目中可以通过定义多个`KronosDataSourceWrapper`实例，实现多数据源、多数据库、动态数据源等功能。

## 使用示例

官方提供了基于JDBC的数据库连接插件，可以通过以下方式引入：

```kotlin group="import" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm.kronos-jdbc-wrapper:0.0.2")
}
```

```groovy group="import" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'com.kotlinorm:kronos-jdbc-wrapper:0.0.2'
}
```

```xml group="import" name="maven" icon="maven"
<project>
  <dependencies>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jdbc-wrapper</artifactId>
      <version>0.0.2</version>
    </dependency>
  </dependencies>
</project>
```

除此之外，可以通过如`kronos-spring-data-wrapper`、`kronos-jdbi-wrapper`、`kronos-mybatis-wrapper`等插件实现数据库连接，与Spring Data、JDBI、MyBatis等框架集成。

以下是使用`kronos-jdbc-wrapper`、`Apache Commons DBCP2`创建数据库连接的示例：

> **Note**
> **BasicDataSource**是Apache Commons DBCP的一个简单的数据源实现，您可以更换为其他数据源实现。

### 1. Mysql数据库连接配置

```kotlin group="Mysql" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:latest.release")
    implementation("com.mysql:mysql-connector-j:latest.release")
    implementation("com.kotlinorm.kronos-jdbc-wrapper:0.0.2")
}
```

```groovy group="Mysql" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:latest.release'
    implementation 'com.mysql:mysql-connector-j:latest.release'
    implementation 'com.kotlinorm:kronos-jdbc-wrapper:0.0.2'
}
```

```xml group="Mysql" name="maven" icon="maven"
<project>
  <dependencies>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-dbcp2</artifactId>
      <version>latest.release</version>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <version>latest.release</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jdbc-wrapper</artifactId>
      <version>0.0.2</version>
    </dependency>
  </dependencies>
</project>
```

```kotlin group="Mysql" name="MysqlKronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
val wrapper by lazy {
    KronosBasicDataSourceWrapper(
        BasicDataSource().apply {
                // if your database version is 8.0 or later, you need to add the following configuration
                driverClassName = "com.mysql.cj.jdbc.Driver"
                // else you can use the following configuration
                // driverClassName = "com.mysql.jdbc.Driver"
                url = "jdbc:mysql://localhost:3306/kronos?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC"
                username = "root"
                password = "******"
        }
    )
}
Kronos.init {
  dataSource = { wrapper }
}
```

### 2. PostgreSQL数据库连接配置

```kotlin group="PostgreSQL" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:latest.release")
    implementation("org.postgresql:postgresql:latest.release")
    implementation("com.kotlinorm.kronos-jdbc-wrapper:0.0.2")
}
```

```groovy group="PostgreSQL" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:latest.release'
    implementation 'org.postgresql:postgresql:latest.release'
    implementation 'com.kotlinorm:kronos-jdbc-wrapper:0.0.2'
}
```

```xml group="PostgreSQL" name="maven" icon="maven"

<project>
  <dependencies>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-dbcp2</artifactId>
      <version>latest.release</version>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>latest.release</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jdbc-wrapper</artifactId>
      <version>0.0.2</version>
    </dependency>
  </dependencies>
</project>
```

```kotlin group="PostgreSQL" name="PostgreSQLKronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos

val wrapper by lazy {
    KronosBasicDataSourceWrapper(
        BasicDataSource().apply {
            driverClassName = "org.postgresql.Driver"
            url = "jdbc:postgresql://localhost:5432/kronos"
            username = "postgres"
            password = "******"
        }
    )
}

Kronos.init {
  dataSource = { wrapper }
}
```

### 3. Oracle数据库连接配置

```kotlin group="Oracle" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:latest.release")
    implementation("com.oracle.database.jdbc:ojdbc8:latest.release")
    implementation("com.kotlinorm.kronos-jdbc-wrapper:0.0.2")
}
```

```groovy group="Oracle" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:latest.release'
    implementation 'com.oracle.database.jdbc:ojdbc8:latest.release'
    implementation 'com.kotlinorm:kronos-jdbc-wrapper:0.0.2'
}
```

```xml group="Oracle" name="maven" icon="maven"
<project>
  <dependencies>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-dbcp2</artifactId>
      <version>latest.release</version>
    </dependency>
    <dependency>
      <groupId>com.oracle.database.jdbc</groupId>
      <artifactId>ojdbc8</artifactId>
      <version>latest.release</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jdbc-wrapper</artifactId>
      <version>0.0.2</version>
    </dependency>
  </dependencies>
</project>
```

```kotlin group="Oracle" name="OracleKronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
val wrapper by lazy {
    KronosBasicDataSourceWrapper(
        BasicDataSource().apply {
            driverClassName = "oracle.jdbc.OracleDriver"
            // replaece the following with your Oracle database connection information
            url = "jdbc:oracle:thin:@localhost:1521:FREEPDB1"
            username = "system"
            password = "******"
        }
    )
}
Kronos.init {
  dataSource = { wrapper }
}
```

### 4. SQL Server数据库连接配置

```kotlin group="SQL Server" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:latest.release")
    implementation("com.microsoft.sqlserver:mssql-jdbc:12.7.0.jre8-preview")
    implementation("com.kotlinorm.kronos-jdbc-wrapper:0.0.2")
}
```

```groovy group="SQL Server" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:latest.release'
    implementation 'com.microsoft.sqlserver:mssql-jdbc:12.7.0.jre8-preview'
    implementation 'com.kotlinorm:kronos-jdbc-wrapper:0.0.2'
}
```

```xml group="SQL Server" name="maven" icon="maven"
<project>
  <dependencies>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-dbcp2</artifactId>
      <version>latest.release</version>
    </dependency>
    <dependency>
      <groupId>com.microsoft.sqlserver</groupId>
      <artifactId>mssql-jdbc</artifactId>
      <version>12.7.0.jre8-preview</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jdbc-wrapper</artifactId>
      <version>0.0.2</version>
    </dependency>
  </dependencies>
</project>
```

```kotlin group="SQL Server" name="SQLServerKronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
val wrapper by lazy {
    KronosBasicDataSourceWrapper(
        BasicDataSource().apply {
            driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
            url = "jdbc:sqlserver://localhost:1433;databaseName=kronos;encrypt=true;trustServerCertificate=true"
            username = "sa"
            password = "******"
        }
    )
}
Kronos.init {
  dataSource = { wrapper }
}
```

### 5. SQLite数据库连接配置

```kotlin group="SQLite" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:latest.release")
    implementation("org.xerial:sqlite-jdbc:latest.release")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:0.0.2")
}
```

```groovy group="SQLite" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:latest.release'
    implementation 'org.xerial:sqlite-jdbc:latest.release'
    implementation 'com.kotlinorm:kronos-jdbc-wrapper:0.0.2'
}
```

```xml group="SQLite" name="maven" icon="maven"

<project>
  <dependencies>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-dbcp2</artifactId>
      <version>latest.release</version>
    </dependency>
    <dependency>
      <groupId>org.xerial</groupId>
      <artifactId>sqlite-jdbc</artifactId>
      <version>latest.release</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jdbc-wrapper</artifactId>
      <version>0.0.2</version>
    </dependency>
  </dependencies>
</project>
```

```kotlin group="SQLite" name="SQLiteKronosConfig.kt" icon="kotlin"
import com.kotlinorm.Kronos
val wrapper by lazy {
    KronosBasicDataSourceWrapper(
        BasicDataSource().apply {
            driverClassName = "org.sqlite.JDBC"
            url = "jdbc:sqlite:/path/to/your/database.db"
        }
    )
}
Kronos.init {
  dataSource = { wrapper }
}
```
