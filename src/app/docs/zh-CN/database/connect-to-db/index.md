# {{ NgDocPage.title }}

Kronos访问数据库通过`KronosDataSourceWrapper`实现。

`KronosDataSourceWrapper`是一个接口，是对数据库操作的封装，它不关心具体的数据库连接细节，与平台无关，只关心数据库操作的逻辑：

- `dbType`：数据库类型
- `url`：数据库连接地址
- `username`：数据库用户名
- `query`：执行查询
  - `List<Map<String, Any>>`：返回查询结果
  - `List<T>`：返回查询结果的第一列
  - `Map<String, Any>`：返回查询结果的第一行
  - `T`：返回查询结果的第一行的第一列
- `execute`：执行更新
- `batch`：批量执行更新
- `transaction`：事务

> **Note**
> **KronosDataSourceWrapper**以扩展的形式在core中引入，这使得**支持多平台**、**新数据库扩展**和**第三方框架集成**成为可能。

## 使用示例

官方提供了jvm平台的基于JDBC的数据库连接插件，可以通过以下方式引入：

```kotlin group="import" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm.kronos-jvm-driver-wrapper:2.0.0")
}
```

```groovy group="import" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:2.0.0'
}
```

```xml group="import" name="maven" icon="maven"
<project>
  <dependencies>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jvm-driver-wrapper</artifactId>
      <version>2.0.0</version>
    </dependency>
  </dependencies>
</project>
```

除此之外，可以通过如`kronos-spring-data-wrapper`、`kronos-jdbi-wrapper`、`kronos-mybatis-wrapper`等插件实现数据库连接，与Spring Data、JDBI、MyBatis等框架集成。

以下是对于`kronos-jvm-driver-wrapper`的使用示例

> **Note**
> **BasicDataSource**是Apache Commons DBCP的一个简单的数据源实现，您可以更换为其他数据源实现。

### 1. Mysql数据库连接配置

```kotlin group="Mysql" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:latest.release")
    implementation("com.mysql:mysql-connector-j:latest.release")
    implementation("com.kotlinorm.kronos-jvm-driver-wrapper:2.0.0")
}
```

```groovy group="Mysql" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:latest.release'
    implementation 'com.mysql:mysql-connector-j:latest.release'
    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:2.0.0'
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
      <artifactId>kronos-jvm-driver-wrapper</artifactId>
      <version>2.0.0</version>
    </dependency>
  </dependencies>
</project>
```

```kotlin group="Mysql" name="MysqlKronosConfig.kt"
import com.kotlinorm.Kronos
Kronos.apply {
  dataSource = {
    BasicDataSource().apply {
        // if your database version is 8.0 or later, you need to add the following configuration
        driverClassName = "com.mysql.cj.jdbc.Driver"
        // else you can use the following configuration
        // driverClassName = "com.mysql.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/kronos?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC"
        username = "root"
        password = "******"
    }
  }
}
```

### 2. PostgreSQL数据库连接配置

```kotlin group="PostgreSQL" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:latest.release")
    implementation("org.postgresql:postgresql:latest.release")
    implementation("com.kotlinorm.kronos-jvm-driver-wrapper:2.0.0")
}
```

```groovy group="PostgreSQL" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:latest.release'
    implementation 'org.postgresql:postgresql:latest.release'
    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:2.0.0'
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
      <artifactId>kronos-jvm-driver-wrapper</artifactId>
      <version>2.0.0</version>
    </dependency>
  </dependencies>
</project>
```

```kotlin group="PostgreSQL" name="PostgreSQLKronosConfig.kt"
import com.kotlinorm.Kronos
Kronos.apply {
  dataSource = {
    BasicDataSource().apply {
        driverClassName = "org.postgresql.Driver"
        url = "jdbc:postgresql://localhost:5432/kronos"
        username = "postgres"
        password = "******"
    }
  }
}
```

### 3. Oracle数据库连接配置

```kotlin group="Oracle" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:latest.release")
    implementation("com.oracle.database.jdbc:ojdbc8:latest.release")
    implementation("com.kotlinorm.kronos-jvm-driver-wrapper:2.0.0")
}
```

```groovy group="Oracle" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:latest.release'
    implementation 'com.oracle.database.jdbc:ojdbc8:latest.release'
    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:2.0.0'
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
      <artifactId>kronos-jvm-driver-wrapper</artifactId>
      <version>2.0.0</version>
    </dependency>
  </dependencies>
</project>
```

```kotlin group="Oracle" name="OracleKronosConfig.kt"
import com.kotlinorm.Kronos
Kronos.apply {
  dataSource = {
    BasicDataSource().apply {
        driverClassName = "oracle.jdbc.OracleDriver"
        // replaece the following with your Oracle database connection information
        url = "jdbc:oracle:thin:@localhost:1521:FREEPDB1"
        username = "system"
        password = "******"
    }
  }
}
```

### 4. SQL Server数据库连接配置

```kotlin group="SQL Server" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:latest.release")
    implementation("com.microsoft.sqlserver:mssql-jdbc:12.7.0.jre8-preview")
    implementation("com.kotlinorm.kronos-jvm-driver-wrapper:2.0.0")
}
```

```groovy group="SQL Server" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:latest.release'
    implementation 'com.microsoft.sqlserver:mssql-jdbc:12.7.0.jre8-preview'
    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:2.0.0'
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
      <artifactId>kronos-jvm-driver-wrapper</artifactId>
      <version>2.0.0</version>
    </dependency>
  </dependencies>
</project>
```

```kotlin group="SQL Server" name="SQLServerKronosConfig.kt"
import com.kotlinorm.Kronos
Kronos.apply {
  dataSource = {
    BasicDataSource().apply {
        driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
        url = "jdbc:sqlserver://localhost:1433;databaseName=kronos;encrypt=true;trustServerCertificate=true"
        username = "sa"
        password = "******"
    }
  }
}
```

### 5. SQLite数据库连接配置

```kotlin group="SQLite" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:latest.release")
    implementation("org.xerial:sqlite-jdbc:latest.release")
    implementation("com.kotlinorm:kronos-jvm-driver-wrapper:2.0.0")
}
```

```groovy group="SQLite" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:latest.release'
    implementation 'org.xerial:sqlite-jdbc:latest.release'
    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:2.0.0'
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
      <artifactId>kronos-jvm-driver-wrapper</artifactId>
      <version>2.0.0</version>
    </dependency>
  </dependencies>
</project>
```

```kotlin group="SQLite" name="SQLiteKronosConfig.kt"
import com.kotlinorm.Kronos
Kronos.apply {
  dataSource = {
    BasicDataSource().apply {
        driverClassName = "org.sqlite.JDBC"
        url = "jdbc:sqlite:/path/to/your/database.db"
    }
  }
}
```