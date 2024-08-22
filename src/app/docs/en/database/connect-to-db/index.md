# {{ NgDocPage.title }}

Kronos accesses the database through `KronosDataSourceWrapper`.

`KronosDataSourceWrapper` is an interface that encapsulates database operations. It does not care about the specific database connection details and is independent of the platform. It only cares about the logic of database operations:

- `dbType`: database type
- `url`: database connection address
- `username`: database username
- `query`: execute query
- `List<Map<String, Any>>`: return query results
- `List<T>`: return the first column of the query results
- `Map<String, Any>`: return the first row of the query results
- `T`: return the first column of the first row of the query results
- `execute`: execute update
- `batch`: batch update
- `transaction`: transaction

> **Note**
> **KronosDataSourceWrapper** is introduced in the core as an extension, which makes it possible to **support multiple platforms**, **new database extensions** and **third-party framework integration**.

## Usage example

Officially provides a JDBC-based database connection plug-in for the jvm platform, which can be introduced in the following ways:

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

In addition, you can use plugins such as `kronos-spring-data-wrapper`, `kronos-jdbi-wrapper`, `kronos-mybatis-wrapper` to connect to the database and integrate with Spring Data, JDBI, MyBatis and other frameworks.

The following is an example of using `kronos-jvm-driver-wrapper`

> **Note**
> **BasicDataSource** is a simple data source implementation of Apache Commons DBCP. You can replace it with other data source implementations.

### 1. Mysql database connection configuration

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

### 2. PostgreSQL database connection configuration

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

### 3. Oracle database connection configuration

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

### 4. SQL Server database connection configuration

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

### 5. SQLite database connection configuration

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
