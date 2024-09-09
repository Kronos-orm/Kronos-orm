Kronos can be easily used with third-party frameworks by customizing the wrapper class that inherits
the `KronosDataSourceWrapper` interface.

## Spring Example

The following is an example using Springboot + Kronos + JDK 17 + Maven + Kotlin 2.0.0 to demonstrate how to use Kronos
with the Spring framework.

It includes how to create a wrapper class based on `spring-data-jdbc`, so that there is no need to introduce additional
dependencies such as `kronos-jdbc-wrapper`, and the database operation function can be realized only
through `kronos-core`.

> [https://github.com/Kronos-orm/kronos-spring-demo](https://github.com/Kronos-orm/kronos-spring-demo)

### 1. Dependencies

Introduce `spring` related dependencies and `kronos-core` dependencies (
see [Quick Start](/documentation/en/getting-started/quick-start) for the introduction of `compiler-plugin` plug-in))

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

### 2.KronosDataSourceWrapper implementation

#### 1. Initialize connection information and JDBC template

```kotlin
// Initialize connection information
init {
    val conn = dataSource.connection
    _metaUrl = conn.metaData.url
    _metaDbType = DBType.fromName(conn.metaData.databaseProductName)
    _userName = conn.metaData.userName ?: ""
    conn.close()
}

// NamedParameterJdbcTemplate is the Spring Data JDBC support for JDBC commands, which supports named parameters
private val namedJdbc: NamedParameterJdbcTemplate by lazy {
    NamedParameterJdbcTemplate(dataSource)
}

```

#### 2.overload the SpringDataWrapper

```kotlin
//1.query Map<String, Any> list
override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
    return namedJdbc.queryForList(task.sql, task.paramMap)
}

//2.query object list
override fun forList(task: KAtomicQueryTask, kClass: KClass<*>): List<Any> {
    return if (KPojo::class.isSuperclassOf(kClass)) namedJdbc.query(
        task.sql,
        task.paramMap,
        DataClassRowMapper(kClass.java)
    )
    else namedJdbc.queryForList(task.sql, task.paramMap, kClass.java)
}

//3.query map, if query result is empty, return null
override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
    return try {
        namedJdbc.queryForMap(task.sql, task.paramMap)
    } catch (e: DataAccessException) {
        null
    }
}

//4.query object, if query result is empty, return null
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

//5.execute update, return the number of rows affected
override fun update(task: KAtomicActionTask): Int {
    return namedJdbc.update(task.sql, task.paramMap)
}

//6.execute batch update, return the number of rows affected
override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
    return namedJdbc.batchUpdate(task.sql, task.paramMapArr ?: emptyArray())
}

//7.transaction
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

#### 3.Some other things

```kotlin
companion object {
    //extension function to wrap JdbcTemplate in SpringDataWrapper
    fun JdbcTemplate.wrapper(): SpringDataWrapper {
        return SpringDataWrapper(this.dataSource!!)
    }

    //extension function to wrap NamedParameterJdbcTemplate in SpringDataWrapper
    fun NamedParameterJdbcTemplate.wrapper(): SpringDataWrapper {
        return SpringDataWrapper(this.jdbcTemplate.dataSource!!)
    }
}
```

All code can be found in:
[SpringDataWrapper.kt](https://github.com/Kronos-orm/kronos-spring-demo/blob/main/src/main/kotlin/com/kotlinorm/kronosSpringDemo/controller/SpringDataWrapper.kt)

## Other Frameworks

For frameworks that support named parameters, the syntax is almost the same as SpringDataWrapper.

For other frameworks that only support sequential parameters, you can get the parsed SQL statement through `KAtomicQueryTask.parsed()` or `KAtomicActionyTask.parsed()` or `KronosAtomicBatchTask.parsedArr()`, which contains the parameter name and parameter value array.

The subsequent process is the same as SpringDataWrapper, please refer to [KronosBasicWrapper.kt](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-jdbc-wrapper/src/main/kotlin/com/kotlinorm/KronosBasicWrapper.kt).