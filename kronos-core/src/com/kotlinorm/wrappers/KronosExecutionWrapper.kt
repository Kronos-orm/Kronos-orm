@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.wrappers

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.ResultColumnMetadata
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.beans.task.executionTransactionScope
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KronosExecutionDataSource
import com.kotlinorm.interfaces.KronosRowMappingDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosResultColumnMetadata
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KronosQueryExecutionPlan
import com.kotlinorm.interfaces.KronosPhysicalRow
import com.kotlinorm.interfaces.KronosPhysicalValue
import com.kotlinorm.interfaces.KronosExecutionFirstResult
import com.kotlinorm.interfaces.KronosActionExecutionPlan
import com.kotlinorm.interfaces.KronosGeneratedKeyRequest
import com.kotlinorm.interfaces.KronosBatchExecutionPlan
import com.kotlinorm.interfaces.KronosTransactionOptions
import com.kotlinorm.interfaces.KronosRow
import com.kotlinorm.interfaces.KronosRowFirstResult
import com.kotlinorm.utils.hasRegisteredKPojoFactory
import com.kotlinorm.utils.createKPojo
import com.kotlinorm.utils.decodeDatabaseValue
import com.kotlinorm.utils.isKPojoType
import com.kotlinorm.utils.resolveRuntimeMetadata
import kotlin.reflect.KType

/** Adapts the shared ORM planner to an application-owned physical execution adapter. */
class KronosExecutionWrapper(val execution: KronosExecutionDataSource) : KronosRowMappingDataSourceWrapper {
    override val url: String get() = execution.identity.url ?: execution.identity.databaseName
    override val userName: String get() = execution.identity.userName.orEmpty()
    override val dbType get() = execution.identity.dbType
    override val capturesGeneratedKeysAtomically: Boolean get() = true

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        val mapper = ResultMapper(task)
        return execution.toList(task.executionPlan(), mapper::map)
    }

    override fun first(task: KAtomicQueryTask): Any? {
        val mapper = ResultMapper(task)
        return when (val result = execution.first(task.executionPlan(), mapper::map)) {
            KronosExecutionFirstResult.Empty -> null
            is KronosExecutionFirstResult.Present -> result.value
        }
    }

    override fun <T> toList(task: KAtomicQueryTask, mapper: (KronosRow) -> T): List<T> =
        execution.toList(task.executionPlan()) { physical -> mapRow(physical, task, mapper) }

    override fun <T> first(task: KAtomicQueryTask, mapper: (KronosRow) -> T): KronosRowFirstResult<T> =
        when (val first = execution.first(task.executionPlan()) { physical -> mapRow(physical, task, mapper) }) {
            KronosExecutionFirstResult.Empty -> KronosRowFirstResult.Empty
            is KronosExecutionFirstResult.Present -> KronosRowFirstResult.Present(first.value)
        }

    private fun <T> mapRow(physical: KronosPhysicalRow, task: KAtomicQueryTask, mapper: (KronosRow) -> T): T {
        var active = true
        val row = object : KronosRow() {
            override val rowNumber: Int get() { check(active); physical.requireActive(); return physical.rowNumber }
            override fun get(position: Int, targetType: KType): Any? {
                check(active) { "KronosRow is only valid while its mapper is running" }
                physical.requireColumnPosition(position)
                val label = physical.columnLabel(position)
                val column = task.resultColumns.column(label)?.withTarget(targetType)
                    ?: ResultColumnMetadata(targetType, columnLabel = label)
                return decodeDatabaseValue(physical.get(position).runtimeValue(), column, sqlDialect, label)
            }
            override fun get(label: String, targetType: KType): Any? = get(physical.columnPosition(label), targetType)
        }
        return try { mapper(row) } finally { active = false }
    }

    override fun update(task: KAtomicActionTask): Int {
        val parsed = task.parsed()
        val names = parsed.jdbcParamList.indices.map { "__kronos_p$it" }
        val result = execution.execute(KronosActionExecutionPlan(
            parsed.jdbcSql, names.zip(parsed.jdbcParamList.toList()).toMap(), task.operationType,
            task.generatedKeyField?.let { KronosGeneratedKeyRequest(it.columnName) }, names
        ))
        task.generatedKeys.clear()
        task.generatedKeys.addAll(result.generatedKeys)
        task.lastInsertId = result.lastInsertId
        return result.affectedRows
    }

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        val parsed = task.parsedSqlArr()
        if (parsed.isEmpty()) return intArrayOf()
        val sql = parsed.first().jdbcSql
        val names = parsed.first().jdbcParamList.indices.map { "__kronos_p$it" }
        require(parsed.all { it.jdbcSql == sql && it.jdbcParamList.size == names.size }) {
            "Batch entries must have the same expanded SQL and binding shape"
        }
        val results = execution.executeBatch(KronosBatchExecutionPlan(
            sql, parsed.map { names.zip(it.jdbcParamList.toList()).toMap() }, task.operationType,
            task.generatedKeyField?.let { KronosGeneratedKeyRequest(it.columnName) }, names
        ))
        check(results.size == parsed.size) { "Execution adapter returned an incomplete batch result" }
        task.generatedKeys.clear()
        results.forEach { task.generatedKeys.addAll(it.generatedKeys) }
        task.lastInsertId = results.firstNotNullOfOrNull { it.lastInsertId }
        return results.map { it.affectedRows }.toIntArray()
    }

    override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? {
        require(isolation == null) { "Execution adapters do not expose JDBC isolation levels" }
        return execution.transact(KronosTransactionOptions(timeoutSeconds = timeout)) {
            executionTransactionScope(this).block()
        }
    }

    private fun KAtomicQueryTask.executionPlan(): KronosQueryExecutionPlan {
        val parsed = parsed()
        val names = parsed.jdbcParamList.indices.map { "__kronos_p$it" }
        return KronosQueryExecutionPlan(parsed.jdbcSql,
            names.zip(parsed.jdbcParamList.toList()).toMap(), operationType, targetTypeInfo,
            resultColumns.mapValues { (_, column) -> KronosResultColumnMetadata(
                column.typeInfo, column.storage, column.field?.name, column.columnLabel
            ) }, names)
    }

    private inner class ResultMapper(private val task: KAtomicQueryTask) {
        private val instances = com.kotlinorm.utils.IdentityHashMap<KPojo, Unit>()
        fun map(row: KronosPhysicalRow): Any? {
            row.requireActive()
            val target = task.targetType
            val classifier = target.classifier
            if (classifier == Map::class || classifier == MutableMap::class) {
                val valueType = target.arguments.getOrNull(1)?.type?.takeUnless { it.classifier == Any::class }
                return mapColumns(row) { label, position ->
                    val column = task.resultColumns.column(label)
                    val metadata = when {
                        valueType != null -> column?.withTarget(valueType) ?: ResultColumnMetadata(valueType, columnLabel = label)
                        else -> column
                    }
                    read(row, position, metadata)
                }
            }
            if (task.targetTypeInfo?.isKPojo == true || target.isKPojoType() || hasRegisteredKPojoFactory(target)) {
                val pojo = createKPojo(target)
                check(!instances.containsKey(pojo)) { "KPojo factory reused an instance across result rows" }
                instances[pojo] = Unit
                val fields = pojo.resolveRuntimeMetadata().allColumns
                val values = mapColumns(row) { label, position ->
                    val declared = task.resultColumns.column(label)
                    val field = fields.resultField(label, declared?.field?.name)
                    val column = declared?.let { metadata -> field?.kType?.let(metadata::withTarget) ?: metadata }
                        ?: field?.kType?.let {
                        ResultColumnMetadata(it, field, columnLabel = label)
                    }
                    read(row, position, column)
                }
                val byProperty = values.mapKeys { (label, _) ->
                    val declaredName = task.resultColumns.column(label)?.field?.name
                    fields.resultField(label, declaredName)?.name ?: label
                }
                return pojo.fromMapData<KPojo>(byProperty)
            }
            require(row.columnCount > 0) { "Scalar result has no columns" }
            val label = row.columnLabel(1)
            val column = task.resultColumns.column(label) ?: task.resultColumns.values.singleOrNull()
            return read(row, 1, when {
                target.classifier == Any::class -> column
                column != null -> column.withTarget(target).copy(typeInfo = task.targetTypeInfo ?: column.typeInfo.takeIf { column.type == target })
                else -> ResultColumnMetadata(target, columnLabel = label, typeInfo = task.targetTypeInfo)
            })
        }

        private fun read(row: KronosPhysicalRow, position: Int, column: ResultColumnMetadata?): Any? {
            val value = row.get(position).runtimeValue()
            return if (column == null) value else decodeDatabaseValue(value, column, sqlDialect, row.columnLabel(position))
        }
    }
}

private fun Map<String, ResultColumnMetadata>.column(label: String): ResultColumnMetadata? =
    this[label] ?: entries.filter { it.key.equals(label, ignoreCase = true) }.singleOrNull()?.value

private inline fun mapColumns(row: KronosPhysicalRow, read: (String, Int) -> Any?): Map<String, Any?> {
    val result = linkedMapOf<String, Any?>()
    for (position in 1..row.columnCount) {
        val label = row.columnLabel(position)
        require(label !in result) { "Ambiguous row column label '$label'; use a physical row mapper" }
        result[label] = read(label, position)
    }
    return result
}

private fun KronosPhysicalValue.runtimeValue(): Any? = when (this) {
    KronosPhysicalValue.Null -> null
    is KronosPhysicalValue.Integer -> value
    is KronosPhysicalValue.Real -> value
    is KronosPhysicalValue.Text -> value
    is KronosPhysicalValue.Blob -> bytes
}

private fun ResultColumnMetadata.withTarget(target: KType): ResultColumnMetadata =
    copy(type = target, typeInfo = typeInfo.takeIf { type == target })

private fun List<com.kotlinorm.beans.dsl.Field>.resultField(label: String, declaredName: String?): com.kotlinorm.beans.dsl.Field? {
    firstOrNull { it.name == label || it.columnName == label }?.let { return it }
    val matches = filter { it.name.equals(label, ignoreCase = true) || it.columnName.equals(label, ignoreCase = true) }
    require(matches.size < 2) { "Ambiguous result field '$label'" }
    return matches.singleOrNull() ?: firstOrNull { it.name == declaredName }
}
