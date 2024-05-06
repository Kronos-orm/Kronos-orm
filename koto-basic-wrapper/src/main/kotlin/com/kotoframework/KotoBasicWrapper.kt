package com.kotoframework

import com.kotoframework.beans.UnsupportedTypeException
import com.kotoframework.beans.task.KotoAtomicBatchTask
import com.kotoframework.beans.task.KotoAtomicTask
import com.kotoframework.enums.DBType
import com.kotoframework.interfaces.KPojo
import com.kotoframework.interfaces.KotoDataSourceWrapper
import com.kotoframework.utils.Extensions.toKPojo
import javax.sql.DataSource
import kotlin.reflect.KClass

class KotoBasicWrapper(private val dataSource: DataSource) : KotoDataSourceWrapper {
    private var _metaUrl: String
    private var _metaDbType: DBType

    override val url: String
        get() = _metaUrl

    override val dbType: DBType
        get() = _metaDbType

    init {
        val conn = dataSource.connection
        _metaUrl = conn.metaData.url
        _metaDbType = DBType.fromName(conn.metaData.databaseProductName)
            ?: throw UnsupportedTypeException("Unknown database type")
        conn.close()
    }

    override fun forList(task: KotoAtomicTask): List<Map<String, Any>> {
        val (sql, paramList) = task.parsed()
        val conn = dataSource.connection
        val ps = conn.prepareStatement(sql)
        paramList.forEachIndexed { index, any ->
            ps.setObject(index + 1, any)
        }
        val rs = ps.executeQuery()
        val metaData = rs.metaData
        val columnCount = metaData.columnCount
        val list = mutableListOf<Map<String, Any>>()
        while (rs.next()) {
            val map = mutableMapOf<String, Any>()
            for (i in 1..columnCount) {
                if (rs.getObject(i) != null) {
                    map[metaData.getColumnName(i)] = rs.getObject(i)
                }
            }
            list.add(map)
        }
        rs.close()
        ps.close()
        conn.close()
        return list
    }

    override fun forList(task: KotoAtomicTask, kClass: KClass<*>): List<Any> {
        return if (kClass.java.isAssignableFrom(KPojo::class.java)) {
            forList(task).map { it.toKPojo(kClass) }
        } else {
            val (sql, paramList) = task.parsed()
            val conn = dataSource.connection
            val ps = conn.prepareStatement(sql)
            paramList.forEachIndexed { index, any ->
                ps.setObject(index + 1, any)
            }
            val rs = ps.executeQuery()
            val list = mutableListOf<Any>()
            while (rs.next()) {
                list.add(rs.getObject(1, kClass.java))
            }
            rs.close()
            ps.close()
            conn.close()
            return list
        }
    }

    override fun forMap(task: KotoAtomicTask): Map<String, Any>? {
        val (sql, paramList) = task.parsed()
        val conn = dataSource.connection
        val ps = conn.prepareStatement(sql)
        paramList.forEachIndexed { index, any ->
            ps.setObject(index + 1, any)
        }
        val rs = ps.executeQuery()
        val metaData = rs.metaData
        val columnCount = metaData.columnCount
        val list = mutableListOf<Map<String, Any>>()
        while (rs.next()) {
            val map = mutableMapOf<String, Any>()
            for (i in 1..columnCount) {
                map[metaData.getColumnName(i)] = rs.getObject(i)
            }
            list.add(map)
        }
        rs.close()
        ps.close()
        conn.close()
        return list.firstOrNull()
    }

    override fun forObject(task: KotoAtomicTask, kClass: KClass<*>): Any? {
        val map = forMap(task)
        val clazz = kClass.java
        return if (String::class.java == kClass.java) {
            map?.values?.firstOrNull()?.toString()
        } else if (KPojo::class.java.isAssignableFrom(kClass.java)) {
            map?.toKPojo(clazz)
        } else if (clazz.name == "java.lang.Integer") {
            map?.values?.firstOrNull()?.toString()?.toInt()
        } else if (clazz.name == "java.lang.Long") {
            map?.values?.firstOrNull()?.toString()?.toLong()
        } else if (clazz.name == "java.lang.Double") {
            map?.values?.firstOrNull()?.toString()?.toDouble()
        } else if (clazz.name == "java.lang.Float") {
            map?.values?.firstOrNull()?.toString()?.toFloat()
        } else if (clazz.name == "java.lang.Boolean") {
            map?.values?.firstOrNull()?.toString()?.toBoolean()
        } else if (clazz.name == "java.lang.Short") {
            map?.values?.firstOrNull()?.toString()?.toShort()
        } else if (clazz.name == "java.lang.Byte") {
            map?.values?.firstOrNull()?.toString()?.toByte()
        } else if (clazz.name == "java.lang.String") {
            map?.values?.firstOrNull()?.toString()
        } else if (clazz.name == "java.util.Date") {
            map?.values?.firstOrNull()?.toString()?.toLong()?.let { java.util.Date(it) }
        } else {
            try {
                map?.values?.firstOrNull()?.toString()?.let { clazz.cast(it) }
            } catch (e: Exception) {
                throw UnsupportedTypeException("Unsupported type: ${clazz.name}")
            }
        }
    }

    override fun update(task: KotoAtomicTask): Int {
        val (sql, paramList) = task.parsed()
        val conn = dataSource.connection
        val ps = conn.prepareStatement(sql)
        paramList.forEachIndexed { index, any ->
            ps.setObject(index + 1, any)
        }
        val result = ps.executeUpdate()
        ps.close()
        conn.close()
        return result
    }

    override fun batchUpdate(task: KotoAtomicBatchTask): IntArray {
        val (sql, paramList) = task.parsed()
        val conn = dataSource.connection
        val ps = conn.prepareStatement(sql)
        paramList.forEach { paramMap ->
            paramMap.forEachIndexed { index, any ->
                ps.setObject(index + 1, any)
            }
            ps.addBatch()
        }
        val result = ps.executeBatch()
        ps.close()
        conn.close()
        return result
    }

    companion object {
        inline fun <reified T> transact(dataSource: DataSource, block: (DataSource) -> T): T {
            val res: T?
            val conn = dataSource.connection
            conn.autoCommit = false
            try {
                res = block(dataSource)
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.close()
            }
            return res!!
        }
    }
}