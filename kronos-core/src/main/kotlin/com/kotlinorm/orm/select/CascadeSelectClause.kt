import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.KronosQueryTask.Companion.merge
import com.kotlinorm.beans.task.KronosQueryTask.Companion.toKronosQueryTask
import com.kotlinorm.enums.KOperationType
import kotlin.reflect.full.createInstance

/**
 * 用于构建级联选择子句的对象。
 * 该对象提供了一种方式来生成针对KPojo对象的级联查询任务。
 */

object CascadeSelectClause {

    fun <T : KPojo> build(pojo: KPojo, rootTask: KronosAtomicQueryTask): KronosQueryTask {
        val columns = pojo.kronosColumns()
        return generateQueryTask(pojo, columns, rootTask)
    }

    private fun generateQueryTask(
        pojo: KPojo,
        columns: List<Field>,
        prevTask: KronosAtomicQueryTask
    ): KronosQueryTask {
        val selectTask = prevTask.toKronosQueryTask().doAfterQuery { // 在执行完前一个任务后进行操作
            val subTasks = columns.filter { !it.isColumn }.flatMap { col ->
                val refClassName = col.referenceKClassName
                    ?: throw IllegalArgumentException("Missing reference class name for column ${col.name}")
                val refPojoClass = Class.forName(refClassName).kotlin
                val refPojo = refPojoClass.createInstance() as KPojo
                val refColumns = refPojo.kronosColumns()

                // 构建连接条件和选择列
                val joinCondition = col.reference?.referenceColumns?.let { (sourceCols, targetCols) ->
                    sourceCols.zip(targetCols).map { (src, tgt) ->
                        "JOIN ${refPojo.kronosTableName()} ON ${pojo.kronosTableName()}.${src} = ${refPojo.kronosTableName()}.${tgt}"
                    }
                } ?: emptyList()

                // 递归处理关联对象的查询
                val nestedTasks = if (refColumns.any { it.cascadeMapperBy(pojo.kronosTableName()) }) {
                    listOf(generateQueryTask(refPojo, refColumns, prevTask))
                } else {
                    emptyList()
                }

                // 构建当前层级的查询任务
                val selectSql = buildSelectSql(pojo, joinCondition)
                val currentTask = KronosAtomicQueryTask(selectSql, pojo.toDataMap().filter { it.value != null }, KOperationType.SELECT)

                // 组合当前任务与子任务
                listOf(currentTask.toKronosQueryTask()) + nestedTasks
            }

            // 合并所有子任务
            subTasks.merge()
        }
        return selectTask
    }

    private fun buildSelectSql(pojo: KPojo, joinConditions: List<String>): String {
        val columnsToSelect = pojo.kronosColumns()
            .filter { it.isColumn }
            .joinToString { "${it.tableName}.`${it.columnName}` AS ${pojo.kronosTableName()}_${it.columnName}" }

        val fromAndJoins = listOf("${pojo.kronosTableName()}") + joinConditions

        return "SELECT $columnsToSelect FROM ${fromAndJoins.joinToString(" ")}"
    }
}