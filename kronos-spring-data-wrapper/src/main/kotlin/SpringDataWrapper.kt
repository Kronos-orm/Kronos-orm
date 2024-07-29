package com.kotlinorm

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.NoDataSourceException
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.TransactionManager
import org.springframework.transaction.support.TransactionTemplate
import sun.rmi.runtime.Log
import java.sql.DataTruncation
import javax.sql.DataSource
import kotlin.reflect.KClass

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/7/29 14:31
 **/
class SpringDataWrapper() : KronosDataSourceWrapper {

    private var namedJdbc: NamedParameterJdbcTemplate? = null
    private var dynamic: (() -> NamedParameterJdbcTemplate)? = null

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

    fun getNamedJdbc(jdbc: NamedParameterJdbcTemplate? = null): NamedParameterJdbcTemplate {
        return jdbc ?: namedJdbc ?: dynamic?.invoke()
        ?: throw NoDataSourceException("NamedParameterJdbcTemplate is null")
    }

    private val dataSource: DataSource
        get() = (namedJdbc ?: dynamic?.invoke())?.jdbcTemplate?.dataSource
            ?: throw NoDataSourceException("dataSource is null")

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
        return getNamedJdbc().queryForList(task.sql, task.paramMap) ?: emptyList()
    }

    override fun forList(task: KAtomicQueryTask, kClass: KClass<*>): List<Any> {
        return getNamedJdbc().queryForList(task.sql , task.paramMap , kClass.java) ?: emptyList()
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
        return getNamedJdbc().queryForMap(task.sql, task.paramMap)
    }

    override fun forObject(task: KAtomicQueryTask, kClass: KClass<*>): Any? {
        return getNamedJdbc().queryForObject(task.sql, task.paramMap, kClass.java)
    }

    override fun update(task: KAtomicActionTask): Int {
        return getNamedJdbc().update(task.sql, task.paramMap)
    }

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        return getNamedJdbc().batchUpdate(task.sql, task.paramMapArr)
    }

    override fun transact(block: (DataSource) -> Any?): Any? {
        val transactianManager = DataSourceTransactionManager(dataSource)
        val transactionTemplate = TransactionTemplate(transactianManager)

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
}