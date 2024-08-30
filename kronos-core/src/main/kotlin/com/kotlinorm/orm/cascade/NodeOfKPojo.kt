/**
 * Copyright 2022-2024 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import kotlin.reflect.jvm.javaField

/**
 * Holds information about a node in the ORM cascade operation tree.
 *
 * This data class is used to store contextual information about a node within the cascade operation tree,
 * including whether to update cascade values, the parent node, the field in the parent node that references this node,
 * and the depth of this node in the tree. The depth is calculated based on the parent's depth, allowing for easy traversal
 * and management of the cascade operation hierarchy.
 *
 * 保存有关 ORM 级联操作树中节点的信息。
 *
 * 此数据类用于存储有关级联操作树中节点的上下文信息，
 * 包括是否更新引用值、父节点、父节点中引用此节点的字段，
 * 以及此节点在树中的深度。深度是根据父节点的深度计算的，以便于遍历和管理级联操作层次结构。
 *
 * @property updateReferenceValue Indicates if the cascade value should be updated during the cascade operation.
 * @property parent The parent node in the cascade operation tree, or null if this is a root node.
 * @property fieldOfParent The field in the parent node that references this node, or null if this is a root node.
 */
data class NodeInfo(
    val updateReferenceValue: Boolean = false,
    val parent: NodeOfKPojo? = null,
    val fieldOfParent: Field? = null
)

/**
 * Represents a node within an ORM cascade operation tree, encapsulating the logic for managing a single entity and its relationships.
 *
 * This data class is central to handling cascade operations within an ORM framework, allowing for the representation
 * of an entity (KPojo) within a tree structure. It supports operations such as patching data from parent nodes,
 * initializing custom logic, and dynamically building child nodes based on the operation type and depth constraints.
 *
 * 表示 ORM 级联操作树中的节点，封装用于管理单个实体及其关系的逻辑。
 *
 * 此数据类是处理 ORM 框架内的级联操作的核心，允许在树结构中表示实体 (KPojo)。它支持从父节点获取并更新关联的数据项、初始化自定义逻辑以及根据操作类型和深度约束动态构建子节点等操作。
 *
 * @property kPojo The entity (KPojo) this node represents.
 * @property data Additional contextual information about this node, such as its parent, the field linking to the parent, and depth in the tree.
 * @property cascadeAllowed The maximum depth to which the tree should be built. A value of -1 indicates no limit.
 * @property operationType The type of ORM operation (e.g., DELETE) being performed, influencing how child nodes are built.
 * @property updateParams A map of parameters to be updated in the entity, used when patching data from the parent.
 * @property onInit An optional initialization block that can be executed upon node creation, allowing for custom logic.
 */
data class NodeOfKPojo(
    val kPojo: KPojo,
    val data: NodeInfo? = null,
    val cascadeAllowed: Array<out KProperty<*>>,
    val operationType: KOperationType,
    val updateParams: MutableMap<String, String> = mutableMapOf(),
    val onInit: (NodeOfKPojo.() -> Unit)? = null
) {
    internal val dataMap by lazy { kPojo.toDataMap() }
    private val validRefs by lazy {
        findValidRefs(
            kPojo.kronosColumns(),
            operationType,
            cascadeAllowed.filterReceiver(kPojo::class).map { it.name }.toSet(),
            cascadeAllowed.isEmpty(),
        )
    }
    val children: MutableList<NodeOfKPojo> = mutableListOf()

    init {
        // Patches data from the parent node to this node. This includes updating fields and parameters
        // based on the parent's data, which is crucial for maintaining consistency in cascading operations.
        patchFromParent()

        // Executes an optional initialization block defined at the node creation. This allows for custom
        // logic to be executed right after the node's basic initialization, providing flexibility in node setup.
        onInit?.invoke(this)
        buildChildren()
    }

    companion object {
        /**
         * Converts a [KPojo] instance into a [NodeOfKPojo] for use in an ORM cascade operation tree.
         *
         * This extension function facilitates the transformation of a [KPojo] entity into a node within the cascade operation tree,
         * encapsulating the entity along with additional contextual information necessary for performing cascade operations.
         * The function allows specifying various parameters that influence the behavior and structure of the resulting node,
         * such as the depth limit for building the tree, the type of operation being performed, and any initialization logic.
         *
         * 将 [KPojo] 实例转换为 [NodeOfKPojo]，以用于 ORM 级联操作树。
         *
         * 此扩展函数有助于将 [KPojo] 实体转换为级联操作树中的节点，
         * 将实体与执行级联操作所需的其他上下文信息一起封装。
         * 该函数允许指定影响结果节点的行为和结构的各种参数，
         * 例如构建树的深度限制、正在执行的操作类型以及任何初始化逻辑。
         *
         * @param data Optional [NodeInfo] providing additional context about the node, such as its parent and depth in the tree.
         * @param cascadeAllowed The maximum depth to which the tree should be built. A value of -1 indicates no limit.
         * @param operationType The type of ORM operation (e.g., DELETE) being performed, influencing how child nodes are built.
         * @param updateParams A map of parameters to be updated in the entity, used when patching data from the parent.
         * @param onInit An optional initialization block that can be executed upon node creation, allowing for custom logic.
         * @return A [NodeOfKPojo] instance representing the [KPojo] within the cascade operation tree.
         */
        internal fun KPojo.toTreeNode(
            data: NodeInfo? = null,
            cascadeAllowed: Array<out KProperty<*>>,
            operationType: KOperationType,
            updateParams: MutableMap<String, String> = mutableMapOf(),
            onInit: (NodeOfKPojo.() -> Unit)? = null
        ): NodeOfKPojo {
            return NodeOfKPojo(this, data, cascadeAllowed, operationType, updateParams, onInit)
        }
    }

    /**
     * Updates the current node's properties based on its parent's corresponding values.
     *
     * This function is responsible for synchronizing the current node's properties with its parent's values,
     * specifically for the fields that are designated as valid references for cascade operations. It first checks
     * if the current node has a parent and if the updateReferenceValue flag is set to true, indicating that the
     * node's properties should be updated based on the parent's values. If these conditions are met, it proceeds
     * to find the corresponding valid cascade in the parent node that matches the fieldOfParent property of the
     * current node. For each target field in the valid cascade, it maps the target field's value from the parent's
     * data map to the corresponding cascade field in the current node. If the current node's property value differs
     * from the parent's, it updates the current node's property with the parent's value and also updates the
     * updateParams map with the new value, ensuring that the cascade operation carries the updated values down the tree.
     *
     * 根据父节点的相应值更新当前节点的属性。
     *
     * 此函数负责将当前节点的属性与其父节点的值同步，
     * 特别是针对指定为级联操作有效引用的字段。它首先检查当前节点是否有父节点，以及 updateReferenceValue 标志是否设置为 true，
     * 这表示应根据父节点的值更新节点的属性。如果满足这些条件，它将继续在父节点中查找与当前节点的 fieldOfParent 属性匹配的相应有效引用。
     * 对于有效引用中的每个目标字段，它将目标字段的值从父节点的数据映射映射到当前节点中的相应引用字段。
     * 如果当前节点的属性值与父节点的属性值不同，它将使用父节点的值更新当前节点的属性，并使用新值更新updateParams 映射，确保级联操作将更新的值传递到树的下方。
     */
    private fun patchFromParent() {
        if (data == null || !data.updateReferenceValue || data.parent == null) return
        val validRef = data.parent.validRefs.find { it.field == data.fieldOfParent } ?: return
        val listOfPair = validRef.kCascade.targetProperties.mapIndexedNotNull { index, it ->
            val targetColumnValue = data.parent.dataMap[it] ?: return@mapIndexedNotNull null
            val originalColumn = validRef.kCascade.properties[index]
            kPojo::class.findPropByName(originalColumn) to targetColumnValue
        }
        listOfPair.forEach { (prop, value) ->
            if (kPojo[prop] != value) {
                kPojo[prop] = value
                validRef.kCascade.targetProperties.forEachIndexed { index, field ->
                    if (data.parent.updateParams[field] != null) {
                        updateParams[validRef.kCascade.properties[index]] = data.parent.updateParams[field]!!
                    }
                }
            }
        }
    }

    /**
     * Builds child nodes for the current node based on valid references and operation type.
     *
     * This function iterates over the list of valid references (validRefs) to determine which references should
     * result in the creation of child nodes. The decision is based on the current operation type (e.g., DELETE),
     * whether the current node's data indicates that cascade values should be updated, and if the cascade's target fields
     * are included in the update parameters of the current node.
     *
     * For each valid cascade, the function checks if the associated value in the current node's data map is non-null.
     * If the value is a collection, it iterates over the collection, and for each item that is an instance of KPojo,
     * it creates a new child node with the appropriate context and adds it to the children list of the current node.
     * If the value is a single instance of KPojo, it directly creates a new child node and adds it to the children list.
     *
     * This process ensures that the ORM cascade operation tree is dynamically built according to the relationships
     * defined in the entity classes, allowing for operations like cascading deletes or updates to be performed correctly.
     *
     * 根据有效引用和操作类型为当前节点构建子节点。
     *
     * 此函数遍历有效引用 (validRefs) 列表，以确定哪些引用应导致创建子节点。该决定基于当前操作类型（例如 DELETE）、
     * 当前节点的数据是否指示应更新引用值，以及引用的目标字段是否包含在当前节点的更新参数中。
     *
     * 对于每个有效引用，该函数检查当前节点数据映射中的关联值是否为非空。
     * 如果值是一个集合，它会遍历该集合，对于每个作为 KPojo 实例的项目，
     * 它会创建一个具有适当上下文的新子节点，并将其添加到当前节点的子节点列表中。
     * 如果值是 KPojo 的单个实例，它会直接创建一个新的子节点并将其添加到子节点列表中。
     *
     * 此过程确保根据实体类中定义的关系动态构建 ORM 级联操作树，从而允许正确执行级联删除或更新等操作。
     */
    private fun buildChildren() {
        validRefs.filter { ref ->
            (operationType == KOperationType.DELETE ||
                    (null != data && data.updateReferenceValue) ||
                    ref.kCascade.targetProperties.any {
                        updateParams.keys.contains(it)
                    }) && (
                    cascadeAllowed.isEmpty() || cascadeAllowed.contains(
                        kPojo::class.findPropByName(ref.field.name)
                    )
            )
        }.forEach { ref ->
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
                                    cascadeAllowed,
                                    operationType,
                                    mutableMapOf(),
                                    onInit
                                )
                            )
                        }
                    }
                } else if (value is KPojo) {
                    children.add(
                        value.toTreeNode(
                            NodeInfo(
                                data?.updateReferenceValue == true,
                                this,
                                ref.field
                            ),
                            cascadeAllowed,
                            operationType,
                            mutableMapOf(),
                            onInit
                        )
                    )
                }
            }
        }
    }
}

private val lruCacheOfProp = LRUCache<Pair<KClass<out KPojo>, String>, KProperty<*>>(128)

/**
 * Finds and returns a mutable property of a [KPojo] class by its name, utilizing a cache for improved performance.
 *
 * This function leverages Kotlin reflection to find a mutable property (`KMutableProperty`) within a [KPojo] class
 * based on the property's name. It uses an LRUCache to cache and quickly retrieve properties, reducing the overhead
 * of reflection. This is particularly useful for operations that require frequent access to the same properties,
 * such as cascading updates or deletes in an ORM context.
 *
 * 根据名称查找并返回 [KPojo] 类的可变属性，利用缓存来提高性能。
 *
 * 此函数利用 Kotlin 反射根据属性名称在 [KPojo] 类中查找可变属性（[KMutableProperty]）。
 * 它使用 [LRUCache] 来缓存和快速检索属性，从而减少反射的开销。这对于需要频繁访问相同属性的操作特别有用，例如在 ORM 上下文中级联更新或删除。
 *
 * @param name The name of the property to find within the [KPojo] class.
 * @return The [KMutableProperty] corresponding to the specified name.
 * @throws UnsupportedOperationException If the property is not found or is not mutable, indicating that it cannot
 *         be used for cascading operations.
 */
internal fun KClass<out KPojo>.findPropByName(name: String): KProperty<*> { // 通过反射获取级联字段的属性
    return lruCacheOfProp.getOrPut(this to name) {
        this.memberProperties.find { prop -> prop.name == name } as KProperty<*>
    }
}

/**
 * Extension property to determine if a [KProperty] represents an iterable collection.
 *
 * Utilizes Kotlin reflection to check if the property's return type is a subtype of [Iterable].
 * This is particularly useful for identifying properties that represent collections, such as lists or sets,
 * which may require special handling in operations like cascading updates or deletes in an ORM context.
 *
 * 用于确定 [KProperty] 是否表示可迭代集合。
 *
 * 利用 Kotlin 反射检查属性的返回类型是否为 [Iterable] 的子类型。
 * 这对于识别表示集合（例如列表或集合）的属性特别有用， 这可能需要在 ORM 上下文中的级联更新或删除等操作中进行特殊处理。
 *
 * @return `true` if the property is of a type that implements [Iterable], `false` otherwise.
 */
internal val KProperty<*>.isIterable
    get(): Boolean { // 判断属性是否为集合
        return this.returnType.classifier?.starProjectedType?.isSubtypeOf(Iterable::class.starProjectedType) == true
    }

/**
 * Sets the value of a specified property on a [KPojo] instance using reflection.
 *
 * This function attempts to set the value of a [KMutableProperty] on the [KPojo] instance. It uses Kotlin reflection
 * to invoke the property's setter method with the provided value. If the property does not exist or is not mutable
 * (i.e., does not have a setter), Java reflection is used to set the property's value.
 *
 *  使用反射设置 [KPojo] 实例上指定属性的值。
 *
 * 此函数尝试设置 [KPojo] 实例上 [KMutableProperty] 的值。它使用 Kotlin 反射使用提供的值调用属性的 setter 方法
 * 如果属性不存在或不可变（即没有 setter），则使用java反射调用属性getDeclaredField，修改isAccessible为true并调用set方法
 *
 * @param prop The [KProperty] whose value is to be set.
 * @param value The new value to assign to the property. Can be `null` if the property allows null values.
 */
internal operator fun KPojo.set(prop: KProperty<*>, value: Any?) {
    if (prop is KMutableProperty<*>) { // 若属性为可变属性
        prop.setter.call(this, value) // 通过setter方法设置属性值
    } else { // 若属性为不可变属性
        val field = KPojo::class.java.getDeclaredField(prop.name) // 获取java属性
        field.isAccessible = true // 设置为可访问
        field.set(this, value) // 通过set方法设置属性值
    }
}

/**
 * Retrieves the value of a specified property on a [KPojo] instance using reflection.
 *
 * This function attempts to retrieve the value of a [KMutableProperty] from the [KPojo] instance. It uses Kotlin reflection
 * to invoke the property's getter method.
 *
 * 使用反射检索 [KPojo] 实例上指定属性的值。
 *
 * 此函数尝试从 [KPojo] 实例中检索 [KMutableProperty] 的值。它使用 Kotlin 反射调用属性的 getter 方法。
 *
 * @param prop The [KMutableProperty] whose value is to be retrieved.
 */
internal operator fun KPojo.get(prop: KProperty<*>) = prop.getter.call(this)

internal fun <T : KPojo> Array<out KProperty<*>>.filterReceiver(receiver: KClass<out T>) =
    filter { it.javaField!!.declaringClass.kotlin == receiver }