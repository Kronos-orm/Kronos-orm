package com.kotlinorm.orm.cascade

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.utils.LRUCache
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

data class NodeOfKPojo(
    val kPojo: KPojo,
    val updateReferenceValue: Boolean = false,
    val parent: NodeOfKPojo? = null,
    val field: Field? = null
) {
    private val columns = kPojo.kronosColumns()
    private val dataMap = kPojo.toDataMap()
    private val validRefs = findValidRefs(kPojo.kronosColumns(), KOperationType.DELETE)
    val children: MutableList<NodeOfKPojo> = mutableListOf()

    init {
        patchFromParent()
        validRefs.forEach { ref ->
            val value = dataMap[ref.field.name]
            if (value != null) {
                if (value is Collection<*>) {
                    value.forEach { child ->
                        if (child is KPojo) {
                            children.add(child.toTreeNode(updateReferenceValue, this, ref.field))
                        }
                    }
                } else if (value is KPojo) {
                    children.add(value.toTreeNode(updateReferenceValue, this, ref.field))
                }
            }
        }
    }

    companion object {
        internal fun KPojo.toTreeNode(
            updateReferenceValue: Boolean = false,
            parent: NodeOfKPojo? = null,
            field: Field? = null
        ): NodeOfKPojo {
            return NodeOfKPojo(this, updateReferenceValue, parent, field)
        }
    }

    private fun patchFromParent() {
        if (!updateReferenceValue || parent == null) return
        val validRef = parent.validRefs.find { it.field == field } ?: return
        val listOfPair = validRef.reference.targetColumns.mapNotNull {
            val targetColumnValue = parent.dataMap[parent.columns.first { col -> col.columnName == it }.name]
            if (targetColumnValue == null) return@mapNotNull null
            val originalColumn = columns.first { col ->
                col.columnName == validRef.reference.referenceColumns[validRef.reference.targetColumns.indexOf(it)]
            }.name
            kPojo::class.findPropByName(originalColumn) to targetColumnValue
        }
        listOfPair.forEach { (prop, value) ->
            kPojo[prop] = value
        }
    }
}

private val lruCacheOfProp = LRUCache<Pair<KClass<out KPojo>, String>, KMutableProperty<*>>(100)
internal fun KClass<out KPojo>.findPropByName(name: String): KMutableProperty<*> { // 通过反射获取级联字段的属性
    return lruCacheOfProp.getOrPut(this to name) {
        this.memberProperties.find { prop -> prop.name == name && prop is KMutableProperty<*> } as KMutableProperty<*>?
            ?: throw UnsupportedOperationException("The property[${this::class.qualifiedName}.$this.$name] to cascade select is not mutable.")
    }
}

internal val KProperty<*>.isIterable
    get(): Boolean { // 判断属性是否为集合
        return this.returnType.classifier?.starProjectedType?.isSubtypeOf(Iterable::class.starProjectedType) == true
    }

internal operator fun KPojo.set(prop: KMutableProperty<*>, value: Any?) { // 通过反射设置属性值
    try {
        prop.setter.call(this, value)
    } catch (e: IllegalArgumentException) {
        throw UnsupportedOperationException("The property[${this::class.qualifiedName}.$this.${prop.name}] to cascade select is not mutable.")
    }
}