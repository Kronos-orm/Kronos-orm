package com.kotlinorm.kronosWrapper.springDataWrapper

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.Extensions.safeMapperTo
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf


/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/7/29 14:31
 **/
@Suppress("SqlSourceToSinkFlow")
class SpringDataWrapper(private val dataSource: DataSource) : KronosDataSourceWrapper {
    private var _metaUrl: String
    private var _userName: String
    private var _metaDbType: DBType

    override val url: String
        get() = _metaUrl

    override val userName: String
        get() = _userName

    override val dbType: DBType
        get() = _metaDbType

    init {
        val conn = dataSource.connection
        _metaUrl = conn.metaData.url
        _metaDbType = DBType.fromName(conn.metaData.databaseProductName)
        _userName = conn.metaData.userName ?: ""
        conn.close()
    }

    private val namedJdbc: NamedParameterJdbcTemplate by lazy {
        NamedParameterJdbcTemplate(dataSource)
    }

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
        return namedJdbc.queryForList(task.sql, task.paramMap)
    }

    @Suppress("UNCHECKED_CAST")
    override fun forList(task: KAtomicQueryTask, kClass: KClass<*>): List<Any> {
        return if (KPojo::class.isSuperclassOf(kClass)) namedJdbc.queryForList(
            task.sql,
            task.paramMap
        ).map {
            it.safeMapperTo(kClass as KClass<KPojo>)
        }
        else namedJdbc.queryForList(task.sql, task.paramMap, kClass.java)
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
        return try {
            namedJdbc.queryForMap(task.sql, task.paramMap)
        } catch (e: DataAccessException) {
            null
        } catch (e: Exception) {
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun forObject(task: KAtomicQueryTask, kClass: KClass<*>): Any? {
        return try {
            if (KPojo::class.isSuperclassOf(kClass)) namedJdbc.queryForMap(
                task.sql,
                task.paramMap
            ).apply {
                safeMapperTo(kClass as KClass<KPojo>)
            }
            else namedJdbc.queryForObject(task.sql, task.paramMap, kClass.java)
        } catch (e: DataAccessException) {
            null
        } catch (e: Exception) {
            throw e
        }
    }

    override fun update(task: KAtomicActionTask): Int {
        return namedJdbc.update(task.sql, task.paramMap)
    }

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        return namedJdbc.batchUpdate(task.sql, task.paramMapArr ?: emptyArray())
    }

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

    companion object {
        fun JdbcTemplate.wrapper(): SpringDataWrapper {
            return SpringDataWrapper(this.dataSource!!)
        }

        fun NamedParameterJdbcTemplate.wrapper(): SpringDataWrapper {
            return SpringDataWrapper(this.jdbcTemplate.dataSource!!)
        }
    }
}