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
import com.kotlinorm.beans.dsl.KCascade
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo

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
    var parent: NodeOfKPojo? = null,
    var fieldOfParent: Field? = null,
    var kCascade: KCascade? = null
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
    val cascadeAllowed: Set<Field>?,
    val operationType: KOperationType,
    val updateParams: MutableMap<String, String> = mutableMapOf(),
    val children: MutableList<NodeOfKPojo> = mutableListOf(),
    val onInit: (NodeOfKPojo.() -> Unit)? = null
) {
    var insertIgnore = false // 该字段用于判断是否忽略插入
    internal val dataMap = kPojo.toDataMap()
    internal val validCascades by lazy {
        val tableName = kPojo.kronosTableName()
        findValidRefs(
            kPojo::class,
            kPojo.kronosColumns(),
            operationType,
            cascadeAllowed?.filter { it.tableName == tableName }?.map { it.name }?.toSet(),
            cascadeAllowed.isNullOrEmpty(),
        )
    }
    val tableName by lazy { kPojo.kronosTableName() }

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
            data: NodeInfo? = null, cascadeAllowed: Set<Field>?,
            operationType: KOperationType,
            updateParams: MutableMap<String, String> = mutableMapOf(),
            children: MutableList<NodeOfKPojo> = mutableListOf(),
            onInit: (NodeOfKPojo.() -> Unit)? = null
        ): NodeOfKPojo {
            return NodeOfKPojo(this, data, cascadeAllowed, operationType, updateParams, children, onInit)
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
        if (data == null || !data.updateReferenceValue || data.parent?.insertIgnore != false) return
        val validRef = data.parent!!.validCascades.find { it.field == data.fieldOfParent } ?: return
        val listOfPair = validRef.kCascade.targetProperties.mapIndexedNotNull { index, it ->
            if (tableName == validRef.tableName) {
                validRef.kCascade.properties[index] to (data.parent!!.dataMap[it]
                    ?: return@mapIndexedNotNull null)
            } else {
                it to (data.parent!!.dataMap[it] ?: return@mapIndexedNotNull null)
            }
        }
        listOfPair.forEach { (prop, value) ->
            if (kPojo[prop] != value) {
                kPojo[prop] = value
                dataMap[prop] = value
                validRef.kCascade.targetProperties.forEachIndexed { index, field ->
                    if (data.parent!!.updateParams[field] != null) {
                        updateParams[validRef.kCascade.properties[index]] = data.parent!!.updateParams[field]!!
                    }
                }
            }
        }
        if (validCascades.filter { it.mapperByThis }.groupBy { it.field.tableName }
                .values
                .any { it.size > 1 }
        ) {
            this.insertIgnore = true
        }
    }

    /**
     * Builds child nodes for the current node based on valid references and operation type.
     *
     * This function iterates over the list of valid references (validCascades) to determine which references should
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
     * 此函数遍历有效引用 (validCascades) 列表，以确定哪些引用应导致创建子节点。该决定基于当前操作类型（例如 DELETE）、
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
        children.forEach {
            it.data?.parent = this
            it.data?.fieldOfParent = validCascades.find { cascade -> cascade.field.tableName == tableName }?.field
            it.patchFromParent()
            it.insertIgnore = false
            it.children.clear()
            it.onInit?.invoke(it)
        }
        val cascades = validCascades.filter { ref ->
            (operationType == KOperationType.DELETE ||
                    (null != data && data.updateReferenceValue) ||
                    ref.kCascade.targetProperties.any {
                        updateParams.keys.contains(it)
                    }) && (
                    cascadeAllowed.isNullOrEmpty() || cascadeAllowed.contains(ref.field)
                    )
        }

        cascades.forEach { cascade ->
            val value = dataMap[cascade.field.name]

            if (value != null) {
                if (value is Collection<*>) {
                    value.forEach { child ->
                        if (child is KPojo) {
                            val node =
                                child.toTreeNode(
                                    NodeInfo(
                                        data?.updateReferenceValue == true,
                                        this,
                                        cascade.field,
                                        cascade.kCascade
                                    ),
                                    cascadeAllowed,
                                    operationType,
                                    mutableMapOf(),
                                    if (insertIgnore) mutableListOf(this) else mutableListOf(),
                                    onInit
                                )
                            children.add(node)
                        }
                    }
                } else if (value is KPojo) {
                    val node =
                        value.toTreeNode(
                            NodeInfo(
                                data?.updateReferenceValue == true,
                                this,
                                cascade.field
                            ),
                            cascadeAllowed,
                            operationType,
                            mutableMapOf(),
                            if (insertIgnore) mutableListOf(this) else mutableListOf(),
                            onInit
                        )
                    children.add(node)
                }
            }
        }
    }
}

internal operator fun KPojo.set(propName: String, value: Any?) {
    safeFromMapData<KPojo>(toDataMap().apply { set(propName, value) })
}

internal operator fun KPojo.get(propName: String) = toDataMap()[propName]