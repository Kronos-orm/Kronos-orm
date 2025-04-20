{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos access to the database is achieved through {{ $.keyword("concept/kronos-data-source-wrapper", ["concept", "data-source-wrapper"]) }}, `KronosDataSourceWrapper` is an interface that is an encapsulation of the database operation, which does not care about the specific database connection details, independent of the platform, and only cares about the logic of database operations.

> **Note**
> Multiple data sources, multiple databases, dynamic data sources, etc. can be realized in the same project by defining multiple `KronosDataSourceWrapper` instances.

## Example

Official JDBC-based database connection plug-ins are provided and can be introduced in the following ways:

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

In addition to this, database connectivity can be achieved through plugins such as `kronos-spring-data-wrapper`, `kronos-jdbi-wrapper`, `kronos-mybatis-wrapper`, etc. Integration with frameworks such as Spring Data, JDBI, and MyBatis.

The following is an example of creating a database connection using `kronos-jdbc-wrapper`, `Apache Commons DBCP2`:

> **Note**
> **BasicDataSource** is a simple data source implementation of Apache Commons DBCP that you can replace with other data source implementations.

### 1. Mysql Database Connection Configuration

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
Kronos.init {
  dataSource = {
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
}
```

### 2. PostgreSQL Database Connection Configuration

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
Kronos.init {
  dataSource = {
    KronosBasicDataSourceWrapper(
        BasicDataSource().apply {
            driverClassName = "org.postgresql.Driver"
            url = "jdbc:postgresql://localhost:5432/kronos"
            username = "postgres"
            password = "******"
        }
    )
  }
}
```

### 3. Oracle Database Connection Configuration

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
Kronos.init {
  dataSource = {
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
}
```

### 4. SQL Server Database Connection Configuration

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
Kronos.init {
  dataSource = {
    KronosBasicDataSourceWrapper(
        BasicDataSource().apply {
            driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
            url = "jdbc:sqlserver://localhost:1433;databaseName=kronos;encrypt=true;trustServerCertificate=true"
            username = "sa"
            password = "******"
        }
    )
  }
}
```

### 5. SQLite Database Connection Configuration

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
Kronos.init {
  dataSource = {
    KronosBasicDataSourceWrapper(
        BasicDataSource().apply {
            driverClassName = "org.sqlite.JDBC"
            url = "jdbc:sqlite:/path/to/your/database.db"
        }
    }
  }
}
```
