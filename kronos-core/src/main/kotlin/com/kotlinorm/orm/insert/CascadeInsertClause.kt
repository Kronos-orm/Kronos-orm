package com.kotlinorm.orm.insert

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.orm.insert.InsertClause.Companion.build
import kotlin.reflect.full.createInstance

object CascadeInsertClause {
    fun <T : KPojo> build(pojo: T): Array<KronosAtomicActionTask> {
        return generateTask(pojo.toDataMap(), pojo.kronosColumns().filter { !it.isColumn }).toTypedArray()
    }

    /**
     * Generates a list of KronosAtomicActionTask for each column in the provided list.
     *
     * This function iterates over the provided list of columns and for each column, it checks if the data value associated with the column name in the data map is null or empty.
     * If the data value is not null or empty, it creates a list of data from the data value.
     * Then, it creates a referenced POJO from the class name specified in the column.
     * If the column has a cascade mapper or any of the columns in the referenced POJO has a cascade mapper for the table of the column, and the column is used for insert operations,
     * it generates a list of KronosAtomicActionTask for the list of data and adds it to the list of tasks.
     *
     * @param dataMap Map<String, Any?> the map of data values associated with the column names.
     * @param columns List<Field> the list of columns for which to generate tasks.
     * @return List<KronosAtomicActionTask> returns a list of KronosAtomicActionTask objects representing the tasks for the columns.
     * @throws UnsupportedOperationException if the reference class is not supported.
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateTask(dataMap: Map<String, Any?>, columns: List<Field>): List<KronosAtomicActionTask> {
        return columns.mapNotNull { col ->
            val dataVal = dataMap[col.name]
            if (dataVal == null || (dataVal is Collection<*> && dataVal.isEmpty())) return@mapNotNull null
            val listOfData = if (dataVal is Collection<*>) {
                dataVal.toList()
            } else {
                listOf(dataVal)
            } as List<KPojo>
            val ref = Class.forName(
                col.referenceKClassName ?: throw UnsupportedOperationException("The reference class is not supported!")
            ).kotlin.createInstance() as KPojo

            val cascadeMB = col.cascadeMapperBy()
            val refCascadeMB = ref.kronosColumns().any { it.cascadeMapperBy(col.tableName) }
            val refUseForInsert = ref.kronosColumns().any { it.refUseFor(KOperationType.INSERT) }

            if ((col.cascadeMapperBy() || ref.kronosColumns()
                    .any { it.cascadeMapperBy(col.tableName) }) && col.refUseFor(
                    KOperationType.INSERT
                )
            ) {
                listOfData.insert().build().component3()
            } else {
                null
            }
        }.flatten()
    }
}