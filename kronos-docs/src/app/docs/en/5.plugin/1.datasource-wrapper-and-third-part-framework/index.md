{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Spring-data-jdbc integration example

The following example using Springboot + Kronos + JDK 17 + Maven + Kotlin 2.0.0 demonstrates how to use Kronos in conjunction with the Spring Framework.

This includes how to create a wrapper class based on `spring-data-jdbc`, so that database manipulation can be achieved with `kronos-core` alone, without having to introduce additional dependencies such as `kronos-jdbc-wrapper`.

> [kronos-example-spring-boot/SpringDataWrapper](https://github.com/Kronos-orm/kronos-example-spring-boot/blob/main/src/main/kotlin/com/kotlinorm/example/springboot/common/SpringDataWrapper.kt)

### 1.dependency

Introducing `spring` related dependencies and `kronos-core` dependencies (the `compiler-plugin` plugin is introduced as described in ({{ $.keyword("/getting-started/quick-start", ["Getting Started"]) }}))

```xml

<dependencies>
    <dependency>
        <groupId>org.springframework.data</groupId>
        <artifactId>spring-data-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-dbcp2</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kotlinorm</groupId>
        <artifactId>kronos-core</artifactId>
        <version>${kronos.version}</version>
    </dependency>
</dependencies>
```

### 2.KronosDataSourceWrapper Implementation

#### 1.Initialize connection information and JDBC templates

```kotlin
// Connection information
init {
    val conn = dataSource.connection
    _metaUrl = conn.metaData.url
    _metaDbType = DBType.fromName(conn.metaData.databaseProductName)
    _userName = conn.metaData.userName ?: ""
    conn.close()
}

// NamedParameterJdbcTemplate is an implementation of JdbcTemplate provided by spring-data-jdbc that supports named parameters for executing JDBC commands
private val namedJdbc: NamedParameterJdbcTemplate by lazy {
    NamedParameterJdbcTemplate(dataSource)
}

```

#### 2.Override database operations in KronosDataSourceWrapper

```kotlin
//1.Querying a Map<String, Any> List
override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
    return namedJdbc.queryForList(task.sql, task.paramMap)
}

//2.Query object list
override fun forList(task: KAtomicQueryTask, kClass: KClass<*>): List<Any> {
    return if (KPojo::class.isSuperclassOf(kClass)) namedJdbc.query(
        task.sql,
        task.paramMap,
        DataClassRowMapper(kClass.java)
    )
    else namedJdbc.queryForList(task.sql, task.paramMap, kClass.java)
}

//3.Query a Map<String, Any>, return null if the query result is empty
override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
    return try {
        namedJdbc.queryForMap(task.sql, task.paramMap)
    } catch (e: DataAccessException) {
        null
    }
}

//4.Query object, return null if the query result is empty
override fun forObject(task: KAtomicQueryTask, kClass: KClass<*>): Any? {
    return try {
        if (KPojo::class.isSuperclassOf(kClass)) namedJdbc.queryForObject(
            task.sql,
            task.paramMap,
            DataClassRowMapper(kClass.java)
        )
        else namedJdbc.queryForObject(task.sql, task.paramMap, kClass.java)
    } catch (e: DataAccessException) {
        null
    }
}

//5.Execute update, return the number of affected rows
override fun update(task: KAtomicActionTask): Int {
    return namedJdbc.update(task.sql, task.paramMap)
}

//6.Execute batch update, return the number of affected rows
override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
    return namedJdbc.batchUpdate(task.sql, task.paramMapArr ?: emptyArray())
}

//7.Transaction
override fun transact(block: (DataSource) -> Any?): Any? {
    val transactionManager = DataSourceTransactionManager(dataSource)
    val transactionTemplate = TransactionTemplate(transactionManager)

    var res: Any? = null

    transactionTemplate.execute {
        try {
            res = block(dataSource)
        } catch (e: Exception) {
            throw e
        }
    }

    return res
}
```

#### 3.Using KronosDataSourceWrapper

```kotlin
companion object {
    //Wrap JdbcTemplate as an extension function of SpringDataWrapper
    fun JdbcTemplate.wrapper(): SpringDataWrapper {
        return SpringDataWrapper(this.dataSource!!)
    }

    //Wrap NamedParameterJdbcTemplate as an extension function of SpringDataWrapper
    fun NamedParameterJdbcTemplate.wrapper(): SpringDataWrapper {
        return SpringDataWrapper(this.jdbcTemplate.dataSource!!)
    }
}
```

All code can be referenced to:
[SpringDataWrapper.kt](https://github.com/Kronos-orm/kronos-spring-demo/blob/main/src/main/kotlin/com/kotlinorm/kronosSpringDemo/controller/SpringDataWrapper.kt)

## Other frameworks

For `JDBI` and other frameworks that support named parameters, the writing method is almost identical to SpringDataWrapper, and only needs to be replaced according to the specific implementation of different frameworks.

For other frameworks that only support sequential parameters, the parsed SQL statement can be obtained via `KAtomicQueryTask.parsed()` or `KAtomicActionyTask.parsed()` or `KronosAtomicBatchTask.parsedArr()`, a property that contains an array of parameter names and parameter values.

The subsequent process is similar to that of SpringDataWrapper. You can refer to [KronosBasicWrapper.kt](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-jdbc-wrapper/src/main/kotlin/com/kotlinorm/KronosBasicWrapper.kt) for guidance.