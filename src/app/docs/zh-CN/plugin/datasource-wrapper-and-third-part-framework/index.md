Kronos通过自定义创建继承`KronosDataSourceWrapper`接口的包装类，可以轻松与第三方框架结合使用。

## Spring示例

以下是一个使用Springboot + Kronos + JDK 17 + Maven + Kotlin 2.0.0 的示例，演示了如何将Kronos与Spring框架结合使用。

其中包含如何创建一个基于`spring-data-jdbc`的包装类，从而无需引入`kronos-jvm-driver-wrapper`等额外依赖，仅通过`kronos-core`
即可实现数据库操作的功能。

> [https://github.com/Kronos-orm/kronos-spring-demo](https://github.com/Kronos-orm/kronos-spring-demo)

### 1.依赖项

引入`spring`相关依赖项及`kronos-core`依赖项（`compiler-plugin`
插件的引入方式见（[快速上手](/documentation/zh-CN/getting-started/quick-start)））

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

### 2.KronosDataSourceWrapper实现

#### 1.初始化连接信息和JDBC模版

```kotlin
// 连接信息
init {
    val conn = dataSource.connection
    _metaUrl = conn.metaData.url
    _metaDbType = DBType.fromName(conn.metaData.databaseProductName)
    _userName = conn.metaData.userName ?: ""
    conn.close()
}

// NamedParameterJdbcTemplate是spring-data-jdbc提供的JdbcTemplate的支持命名参数的实现，用于执行JDBC命令
private val namedJdbc: NamedParameterJdbcTemplate by lazy {
    NamedParameterJdbcTemplate(dataSource)
}

```

#### 2.重载KronosDataSourceWrapper中的数据库操作

```kotlin
//1.查询Map<String, Any>列表
override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
    return namedJdbc.queryForList(task.sql, task.paramMap)
}

//2.查询对象列表
override fun forList(task: KAtomicQueryTask, kClass: KClass<*>): List<Any> {
    return if (KPojo::class.isSuperclassOf(kClass)) namedJdbc.query(
        task.sql,
        task.paramMap,
        DataClassRowMapper(kClass.java)
    )
    else namedJdbc.queryForList(task.sql, task.paramMap, kClass.java)
}

//3.查询Map<String, Any>，如果查询结果为空则返回null
override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
    return try {
        namedJdbc.queryForMap(task.sql, task.paramMap)
    } catch (e: DataAccessException) {
        null
    }
}

//4.查询对象，如果查询结果为空则返回null
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

//5.执行更新，返回受影响的行数
override fun update(task: KAtomicActionTask): Int {
    return namedJdbc.update(task.sql, task.paramMap)
}

//6.执行批量更新，返回受影响的行数
override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
    return namedJdbc.batchUpdate(task.sql, task.paramMapArr ?: emptyArray())
}

//7.事务操作
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

#### 3.其他

```kotlin
companion object {
    //将JdbcTemplate包装为SpringDataWrapper的扩展函数
    fun JdbcTemplate.wrapper(): SpringDataWrapper {
        return SpringDataWrapper(this.dataSource!!)
    }

    //将NamedParameterJdbcTemplate包装为SpringDataWrapper的扩展函数
    fun NamedParameterJdbcTemplate.wrapper(): SpringDataWrapper {
        return SpringDataWrapper(this.jdbcTemplate.dataSource!!)
    }
}
```

全部代码可参考：
[SpringDataWrapper.kt](https://github.com/Kronos-orm/kronos-spring-demo/blob/main/src/main/kotlin/com/kotlinorm/kronosSpringDemo/controller/SpringDataWrapper.kt)

## 其他框架

对于JDBI等支持命名参数的框架，写法与SpringDataWrapper几乎完全相同，只需根据不同框架的具体实现进行替换即可。

对于其他仅支持顺序参数的框架，可以通过`KAtomicQueryTask.parsed()`或`KAtomicActionyTask.parsed()`或`KronosAtomicBatchTask.parsedArr()`获取解析后的SQL语句，这个属性中包含了参数名和参数值数组。

后续大致流程与SpringDataWrapper相同，可参考[KronosBasicWrapper.kt](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-jvm-driver-wrapper/src/main/kotlin/com/kotlinorm/KronosBasicWrapper.kt)。