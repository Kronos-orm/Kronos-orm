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

data class NodeInfo(
    val updateReferenceValue: Boolean = false,
    val parent: NodeOfKPojo? = null,
    val fieldOfParent: Field? = null
)

data class NodeOfKPojo(
    val kPojo: KPojo,
    val data: NodeInfo? = null,
    val onInit: (NodeOfKPojo.() -> Unit)? = null
) {
    internal val dataMap = kPojo.toDataMap()
    private val validRefs = findValidRefs(kPojo.kronosColumns(), KOperationType.DELETE)
    val children: MutableList<NodeOfKPojo> = mutableListOf()

    init {
        patchFromParent()
        onInit?.invoke(this)
        validRefs.forEach { ref ->
            val value = dataMap[ref.field.name]
            if (value != null) {
                if (value is Collection<*>) {
                    value.forEach { child ->
                        if (child is KPojo) {
                            children.add(
                                child.toTreeNode(
                                    NodeInfo(
                                        data?.updateReferenceValue == true,
                                        this,
                                        ref.field
                                    ),
                                    onInit
                                )
                            )
                        }
                    }
                } else if (value is KPojo) {
                    children.add(
                        value.toTreeNode(
                            NodeInfo(data?.updateReferenceValue == true, this, ref.field),
                            onInit
                        )
                    )
                }
            }
        }
    }

    companion object {
        internal fun KPojo.toTreeNode(
            data: NodeInfo? = null,
            onInit: (NodeOfKPojo.() -> Unit)? = null
        ): NodeOfKPojo {
            return NodeOfKPojo(this, data, onInit)
        }
    }

    private fun patchFromParent() {
        if (data == null || !data.updateReferenceValue || data.parent == null) return
        val validRef = data.parent.validRefs.find { it.field == data.fieldOfParent } ?: return
        val listOfPair = validRef.reference.targetFields.mapNotNull {
            val targetColumnValue = data.parent.dataMap[it] ?: return@mapNotNull null
            val originalColumn = validRef.reference.referenceFields[validRef.reference.targetFields.indexOf(it)]
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