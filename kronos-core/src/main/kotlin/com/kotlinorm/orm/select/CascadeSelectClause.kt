import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KReference
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.KronosQueryTask.Companion.toKronosQueryTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.QueryType.*
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.patchTo
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

/**
 * 用于构建级联选择子句的对象。
 * 该对象提供了一种方式来生成针对KPojo对象的级联查询任务。
 */

object CascadeSelectClause {
    data class ValidRef(
        val field: Field, val reference: KReference, val refPojo: KPojo
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

            if (col.cascadeMapperBy() && col.refUseFor(KOperationType.SELECT)) {
                listOf(col.reference!!) // 若有级联映射，返回引用
            } else {
                ref.kronosColumns()
                    .filter { it.cascadeMapperBy(col.tableName) && it.refUseFor(KOperationType.SELECT) }
                    .map { it.reference!! } // 若没有级联映射，返回引用的所有关于本表级联映射
            }.map { reference ->
                ValidRef(col, reference, ref)
            }
        }.flatten()
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateQueryTask(
        pojo: KPojo,
        columns: List<Field>,
        prevTask: KronosAtomicQueryTask
    ): KronosQueryTask {
        val validReferences =
            findValidRefs(
                columns, pojo.toDataMap()
            ) // 获取所有的非数据库列、有关联注解且用于删除操作
        if (validReferences.isEmpty()) {
            // 若没有关联信息，返回空（在deleteClause的build中，有对null值的判断和默认值处理）
            // 为何不直接返回deleteTask: 因为此处的deleteTask构建sql语句时带有表名，而普通的deleteTask不带表名，因此需要重新构建
            return prevTask.toKronosQueryTask()
        } else {
            return prevTask.toKronosQueryTask().doAfterQuery { queryType, wrapper ->
                validReferences.forEach { validRef ->
                    val prop =
                        pojo::class.findPropByName(validRef.field.name) // 获取级联字段的属性如：GroupClass.students

                    when (queryType) {
                        QueryList -> { // 若是查询KPojo列表
                            val lastStepResult = this as List<KPojo> // this为主表查询的结果
                            lastStepResult.forEach rowMapper@{
                                setValues(it, prop, validRef, columns, wrapper)
                            }
                        }

                        QueryOne, QueryOneOrNull -> {
                            setValues(this as KPojo, prop, validRef, columns, wrapper)
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun setValues(
        pojo: KPojo,
        prop: KMutableProperty<*>,
        validRef: ValidRef,
        columns: List<Field>,
        wrapper: KronosDataSourceWrapper
    ) { // 将KPojo转为Map，该map将用于级联查询
        val dataMap = pojo.toDataMap()
        val listOfPair = mutableListOf<Pair<String, Any?>>()
        validRef.reference.targetColumns.forEachIndexed { index, targetColumn ->
            val targetColumnValue =
                dataMap[columns.first { col -> col.columnName == targetColumn }.name]
            if (targetColumnValue == null) return
            val originalColumn =
                validRef.refPojo.kronosColumns()
                    .first { col -> col.columnName == validRef.reference.referenceColumns[index] }.name
            listOfPair.add(originalColumn to targetColumnValue)
        }
        val refPojo = validRef.refPojo.patchTo(
            validRef.refPojo::class,
            *listOfPair.toTypedArray()
        ) // 通过反射创建引用的类的POJO，支持类型为KPojo/Collections<KPojo>，将级联需要用到的字段填充

        if (prop.isIterable) { // 判断属性是否为集合
            pojo[prop] = refPojo.select().queryList(wrapper) // 查询级联的POJO
        } else {
            pojo[prop] = refPojo.select().queryOneOrNull(wrapper) // 查询级联的POJO
        }

    }

    private val mapOfProp = mutableMapOf<KClass<out KPojo>, KMutableProperty<*>>() // 用于存储级联字段的属性
    private fun KClass<out KPojo>.findPropByName(name: String): KMutableProperty<*> { // 通过反射获取级联字段的属性
        return mapOfProp.getOrPut(this) {
            this.memberProperties.find { prop -> prop.name == name && prop is KMutableProperty<*> } as KMutableProperty<*>?
                ?: throw UnsupportedOperationException("The property[${this::class.qualifiedName}.$this.$name] to cascade select is not mutable.")
        }
    }

    private val KProperty<*>.isIterable
        get(): Boolean { // 判断属性是否为集合
            return this.returnType.classifier?.starProjectedType?.isSubtypeOf(Iterable::class.starProjectedType) == true
        }

    private operator fun KPojo.set(prop: KMutableProperty<*>, value: Any?) { // 通过反射设置属性值
        prop.setter.call(this, value)
    }
}