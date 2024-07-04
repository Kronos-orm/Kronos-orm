import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KReference
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.KronosQueryTask.Companion.toKronosQueryTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.QueryType.QueryList
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.full.createInstance

/**
 * 用于构建级联选择子句的对象。
 * 该对象提供了一种方式来生成针对KPojo对象的级联查询任务。
 */

object CascadeSelectClause {
    data class ValidRef(
        val field: Field, val references: List<KReference>, val refPojo: List<KPojo>
    )

    fun <T : KPojo> build(pojo: T, rootTask: KronosAtomicQueryTask): KronosQueryTask {
        return generateQueryTask(pojo, pojo.kronosColumns(), rootTask)
    }

    /**
     * Finds valid references in a list of fields.
     *
     * This function iterates over the provided list of fields and for each field, it creates a ValidRef object.
     * A ValidRef object contains a list of references and a referenced POJO.
     * If the field has a cascade mapper, the list of references contains only the reference of the field.
     * Otherwise, the list of references contains all the references in the columns of the referenced POJO that have a cascade mapper for the table of the field.
     * The referenced POJO is created from the class name specified in the field.
     *
     * @param columns List<Field> the list of fields for which to find valid references.
     * @return List<ValidRef> returns a list of ValidRef objects representing the valid references in the fields.
     * @throws UnsupportedOperationException if the reference class is not supported.
     */
    private fun findValidRefs(columns: List<Field>, dataMap: Map<String, Any?>): List<ValidRef> {
        //columns 为的非数据库列、有关联注解且用于删除操作的Field
        return columns.filter { !it.isColumn }.map { col ->
            val ref = Class.forName(
                col.referenceKClassName ?: throw UnsupportedOperationException("The reference class is not supported!")
            ).kotlin.createInstance() as KPojo // 通过反射创建引用的类的POJO，支持类型为KPojo/Collections<KPojo>

            val references = ref.kronosColumns()
                .filter { it.cascadeMapperBy(col.tableName) && it.refUseFor(KOperationType.SELECT) }
                .map { it.reference!! } // 若没有级联映射，返回引用的所有关于本表级联映射

            val refColumns = ref.kronosColumns()
            val listOfRef = mutableListOf<KPojo>()

            references.forEach { reference ->
                val temp = mutableMapOf<String, Any?>()
                reference.targetColumns.forEachIndexed { index, targetColumn ->
                    temp[
                        refColumns.first { it.columnName == reference.referenceColumns[index] }.name
                    ] = dataMap[
                        columns.first { it.columnName == targetColumn }.name
                    ] // 从dataMap中获取引用的列名和值
                }
                listOfRef.add(temp.mapperTo(ref::class) as KPojo)
            }

            ValidRef(col, if (col.cascadeMapperBy() && col.refUseFor(KOperationType.SELECT)) {
                listOf(col.reference!!) // 若有级联映射，返回引用
            } else {
                ref.kronosColumns().filter { it.cascadeMapperBy(col.tableName) && it.refUseFor(KOperationType.SELECT) }
                    .map { it.reference!! } // 若没有级联映射，返回引用的所有关于本表级联映射
            }, listOfRef)
        }
    }

    private fun generateQueryTask(
        pojo: KPojo,
        columns: List<Field>,
        prevTask: KronosAtomicQueryTask
    ): KronosQueryTask {
        val validReferences =
            findValidRefs(
                columns, pojo.toDataMap()
            ) // 获取所有的非数据库列、有关联注解且用于删除操作
                .filter { it.refPojo.isNotEmpty() }
        if (validReferences.isEmpty()) {
            // 若没有关联信息，返回空（在deleteClause的build中，有对null值的判断和默认值处理）
            // 为何不直接返回deleteTask: 因为此处的deleteTask构建sql语句时带有表名，而普通的deleteTask不带表名，因此需要重新构建
            return prevTask.toKronosQueryTask()
        } else {
            return prevTask.toKronosQueryTask().doAfterQuery { queryType, wrapper ->
                validReferences.forEach { validRef ->
                    validRef.refPojo.forEach { refPojo ->
                        when (queryType) {
                            QueryList -> {
                                val listOfPojo = refPojo.select().where().queryList(wrapper)
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}