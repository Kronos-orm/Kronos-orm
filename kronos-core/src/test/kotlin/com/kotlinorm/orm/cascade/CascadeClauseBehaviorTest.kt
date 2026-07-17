package com.kotlinorm.orm.cascade

import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.generator.customIdGenerator
import com.kotlinorm.beans.task.GeneratedKeyRequest
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.CascadeDeleteAction
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KIdGenerator
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.NodeOfKPojo.Companion.toTreeNode
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.utils.LinkedHashSet
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Table("cascade_select_parent")
data class CascadeSelectBehaviorParent(
    @PrimaryKey
    var id: Int? = null,
    var childId: Int? = null,
    @Cascade(["childId"], ["id"])
    var child: CascadeSelectBehaviorChild? = null,
    @Cascade(["id"], ["parentId"])
    var children: List<CascadeSelectBehaviorChild>? = null
) : KPojo

@Table("cascade_select_child")
data class CascadeSelectBehaviorChild(
    @PrimaryKey
    var id: Int? = null,
    var parentId: Int? = null,
    var name: String? = null
) : KPojo

@Table("cascade_composite_parent")
data class CascadeCompositeParent(
    @PrimaryKey
    var id1: Int? = null,
    @PrimaryKey
    var id2: Int? = null,
    @Cascade(["id1", "id2"], ["parentId1", "parentId2"])
    var children: List<CascadeCompositeChild>? = null
) : KPojo

@Table("cascade_composite_child")
data class CascadeCompositeChild(
    @PrimaryKey
    var id: Int? = null,
    var parentId1: Int? = null,
    var parentId2: Int? = null,
    var name: String? = null
) : KPojo

@Table("cascade_delete_parent")
data class CascadeDeleteBehaviorParent(
    @PrimaryKey
    var id: Int? = null,
    var children: List<CascadeDeleteBehaviorChild>? = null
) : KPojo

@Table("cascade_delete_child")
data class CascadeDeleteBehaviorChild(
    @PrimaryKey
    var id: Int? = null,
    var parentId: Int? = null,
    @Cascade(["parentId"], ["id"], onDelete = CascadeDeleteAction.CASCADE)
    var parent: CascadeDeleteBehaviorParent? = null
) : KPojo

@Table("cascade_action_parent")
data class CascadeActionParent(
    @PrimaryKey
    var id: Int? = null,
    var children: List<CascadeActionChild>? = null
) : KPojo

@Table("cascade_action_child")
data class CascadeActionChild(
    @PrimaryKey
    var id: Int? = null,
    var parentId: Int? = null,
    @Cascade(["parentId"], ["id"], onDelete = CascadeDeleteAction.CASCADE)
    var parent: CascadeActionParent? = null
) : KPojo

@Table("cascade_physical_delete_parent")
data class CascadePhysicalDeleteParent(
    @PrimaryKey
    var id: Int? = null,
    var children: List<CascadePhysicalDeleteChild>? = null
) : KPojo

@Table("cascade_physical_delete_child")
data class CascadePhysicalDeleteChild(
    @PrimaryKey
    var id: Int? = null,
    var parentId: Int? = null,
    @LogicDelete
    var deleted: Boolean? = null,
    @Cascade(["parentId"], ["id"], onDelete = CascadeDeleteAction.CASCADE)
    var parent: CascadePhysicalDeleteParent? = null
) : KPojo

@Table("cascade_logic_delete_parent")
data class CascadeLogicDeleteParent(
    @PrimaryKey
    var id: Int? = null,
    @LogicDelete
    var deleted: Boolean? = null,
    var children: List<CascadeLogicDeleteChild>? = null
) : KPojo

@Table("cascade_logic_delete_child")
data class CascadeLogicDeleteChild(
    @PrimaryKey
    var id: Int? = null,
    var parentId: Int? = null,
    @Cascade(["parentId"], ["id"], onDelete = CascadeDeleteAction.CASCADE)
    var parent: CascadeLogicDeleteParent? = null
) : KPojo

@Table("cascade_set_null_parent")
data class CascadeSetNullParent(
    @PrimaryKey
    var id: Int? = null,
    var children: List<CascadeSetNullChild>? = null
) : KPojo

@Table("cascade_set_null_child")
data class CascadeSetNullChild(
    @PrimaryKey
    var id: Int? = null,
    var parentId: Int? = null,
    @Cascade(["parentId"], ["id"], onDelete = CascadeDeleteAction.SET_NULL)
    var parent: CascadeSetNullParent? = null
) : KPojo

@Table("cascade_single_set_null_parent")
data class CascadeSingleSetNullParent(
    @PrimaryKey
    var id: Int? = null,
    var child: CascadeSingleSetNullChild? = null
) : KPojo

@Table("cascade_single_set_null_child")
data class CascadeSingleSetNullChild(
    @PrimaryKey
    var id: Int? = null,
    var parentId: Int? = null,
    @Cascade(["parentId"], ["id"], onDelete = CascadeDeleteAction.SET_NULL)
    var parent: CascadeSingleSetNullParent? = null
) : KPojo

@Table("cascade_set_default_parent")
data class CascadeSetDefaultParent(
    @PrimaryKey
    var id: Int? = null,
    var children: List<CascadeSetDefaultChild>? = null
) : KPojo

@Table("cascade_set_default_child")
data class CascadeSetDefaultChild(
    @PrimaryKey
    var id: Int? = null,
    var parentId: Int? = null,
    @Cascade(["parentId"], ["id"], onDelete = CascadeDeleteAction.SET_DEFAULT, defaultValue = ["0"])
    var parent: CascadeSetDefaultParent? = null
) : KPojo

@Table("cascade_restrict_parent")
data class CascadeRestrictParent(
    @PrimaryKey
    var id: Int? = null,
    var children: List<CascadeRestrictChild>? = null
) : KPojo

@Table("cascade_restrict_child")
data class CascadeRestrictChild(
    @PrimaryKey
    var id: Int? = null,
    var parentId: Int? = null,
    @Cascade(["parentId"], ["id"], onDelete = CascadeDeleteAction.RESTRICT)
    var parent: CascadeRestrictParent? = null
) : KPojo

@Table("cascade_insert_parent")
data class CascadeInsertParent(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    @Cascade(["id"], ["parentId"])
    var children: List<CascadeInsertChild>? = null
) : KPojo

@Table("cascade_insert_child")
data class CascadeInsertChild(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var parentId: Int? = null,
    var name: String? = null
) : KPojo

@Table("cascade_insert_reverse_parent")
data class CascadeInsertReverseParent(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var children: List<CascadeInsertReverseChild>? = null
) : KPojo

@Table("cascade_insert_reverse_child")
data class CascadeInsertReverseChild(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var parentId: Int? = null,
    var name: String? = null,
    @Cascade(["parentId"], ["id"])
    var parent: CascadeInsertReverseParent? = null
) : KPojo

@Table("cascade_assigned_key_parent")
data class CascadeAssignedKeyParent(
    @PrimaryKey(custom = true)
    var id: String? = null,
    var name: String? = null,
    @Cascade(["id"], ["parentId"])
    var children: List<CascadeAssignedKeyChild>? = null
) : KPojo

@Table("cascade_assigned_key_child")
data class CascadeAssignedKeyChild(
    @PrimaryKey(custom = true)
    var id: String? = null,
    var parentId: String? = null,
    var name: String? = null
) : KPojo

@Table("cascade_uuid_key_parent")
data class CascadeUuidKeyParent(
    @PrimaryKey(uuid = true)
    var id: String? = null,
    var name: String? = null,
    @Cascade(["id"], ["parentId"])
    var children: List<CascadeUuidKeyChild>? = null
) : KPojo

@Table("cascade_uuid_key_child")
data class CascadeUuidKeyChild(
    @PrimaryKey(uuid = true)
    var id: String? = null,
    var parentId: String? = null,
    var name: String? = null
) : KPojo

@Table("cascade_snowflake_key_parent")
data class CascadeSnowflakeKeyParent(
    @PrimaryKey(snowflake = true)
    var id: Long? = null,
    var name: String? = null,
    @Cascade(["id"], ["parentId"])
    var children: List<CascadeSnowflakeKeyChild>? = null
) : KPojo

@Table("cascade_snowflake_key_child")
data class CascadeSnowflakeKeyChild(
    @PrimaryKey(snowflake = true)
    var id: Long? = null,
    var parentId: Long? = null,
    var name: String? = null
) : KPojo

@Table("cascade_custom_key_parent")
data class CascadeCustomKeyParent(
    @PrimaryKey(custom = true)
    var id: String? = null,
    var name: String? = null,
    @Cascade(["id"], ["parentId"])
    var children: List<CascadeCustomKeyChild>? = null
) : KPojo

@Table("cascade_custom_key_child")
data class CascadeCustomKeyChild(
    @PrimaryKey(custom = true)
    var id: String? = null,
    var parentId: String? = null,
    var name: String? = null
) : KPojo

class CascadeClauseBehaviorTest : MysqlTestBase() {

    @Test
    fun `cascade insert requests generated keys and patches child foreign keys`() {
        val child = CascadeInsertChild(name = "child")
        val parent = CascadeInsertParent(name = "root", children = listOf(child))
        val wrapper = CascadeRecordingWrapper(generatedKeys = mutableListOf(101L, 201L))

        val result = parent.insert().execute(wrapper)

        assertEquals(101L, result.lastInsertId)
        assertEquals(101, parent.id)
        assertEquals(201, child.id)
        assertEquals(101, child.parentId)
        assertEquals(
            listOf(
                "INSERT INTO `cascade_insert_parent` (`name`) VALUES (:name)",
                "INSERT INTO `cascade_insert_child` (`parent_id`, `name`) VALUES (:parentId, :name)"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf(
                mapOf<String, Any?>("name" to "root"),
                mapOf<String, Any?>("parentId" to 101, "name" to "child")
            ),
            wrapper.actionParams
        )
        assertEquals(
            listOf<GeneratedKeyRequest?>(
                GeneratedKeyRequest("cascade_insert_parent", "id"),
                GeneratedKeyRequest("cascade_insert_child", "id")
            ),
            wrapper.actionGeneratedKeyRequests
        )
    }

    @Test
    fun `cascade insert discovers child-owned relation mapping from allowed parent field`() {
        val child = CascadeInsertReverseChild(name = "child")
        val parent = CascadeInsertReverseParent(name = "root", children = listOf(child))
        val wrapper = CascadeRecordingWrapper(generatedKeys = mutableListOf(101L, 201L))

        val result = parent.insert()
            .cascade { [CascadeInsertReverseParent::children] }
            .execute(wrapper)

        assertEquals(101L, result.lastInsertId)
        assertEquals(101, parent.id)
        assertEquals(201, child.id)
        assertEquals(101, child.parentId)
        assertEquals(
            listOf(
                "INSERT INTO `cascade_insert_reverse_parent` (`name`) VALUES (:name)",
                "INSERT INTO `cascade_insert_reverse_child` (`parent_id`, `name`) VALUES (:parentId, :name)"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf(
                mapOf<String, Any?>("name" to "root"),
                mapOf<String, Any?>("parentId" to 101, "name" to "child")
            ),
            wrapper.actionParams
        )
        assertEquals(
            listOf<GeneratedKeyRequest?>(
                GeneratedKeyRequest("cascade_insert_reverse_parent", "id"),
                GeneratedKeyRequest("cascade_insert_reverse_child", "id")
            ),
            wrapper.actionGeneratedKeyRequests
        )
    }

    @Test
    fun `cascade insert executes children with assigned custom primary keys`() {
        val child = CascadeAssignedKeyChild(id = "child-1", name = "child")
        val parent = CascadeAssignedKeyParent(id = "parent-1", name = "root", children = listOf(child))
        val wrapper = CascadeRecordingWrapper()

        parent.insert().execute(wrapper)

        assertEquals("parent-1", child.parentId)
        assertEquals(
            listOf(
                "INSERT INTO `cascade_assigned_key_parent` (`id`, `name`) VALUES (:id, :name)",
                "INSERT INTO `cascade_assigned_key_child` (`id`, `parent_id`, `name`) VALUES (:id, :parentId, :name)"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf(
                mapOf<String, Any?>("id" to "parent-1", "name" to "root"),
                mapOf<String, Any?>("id" to "child-1", "parentId" to "parent-1", "name" to "child")
            ),
            wrapper.actionParams
        )
        assertEquals(
            listOf<GeneratedKeyRequest?>(null, null),
            wrapper.actionGeneratedKeyRequests
        )
    }

    @Test
    fun `cascade insert executes children with assigned UUID primary keys`() {
        val child = CascadeUuidKeyChild(
            id = "22222222-2222-2222-2222-222222222222",
            name = "child"
        )
        val parent = CascadeUuidKeyParent(
            id = "11111111-1111-1111-1111-111111111111",
            name = "root",
            children = listOf(child)
        )
        val wrapper = CascadeRecordingWrapper()

        parent.insert().execute(wrapper)

        assertEquals("11111111-1111-1111-1111-111111111111", child.parentId)
        assertEquals(
            listOf(
                "INSERT INTO `cascade_uuid_key_parent` (`id`, `name`) VALUES (:id, :name)",
                "INSERT INTO `cascade_uuid_key_child` (`id`, `parent_id`, `name`) VALUES (:id, :parentId, :name)"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf(
                mapOf<String, Any?>(
                    "id" to "11111111-1111-1111-1111-111111111111",
                    "name" to "root"
                ),
                mapOf<String, Any?>(
                    "id" to "22222222-2222-2222-2222-222222222222",
                    "parentId" to "11111111-1111-1111-1111-111111111111",
                    "name" to "child"
                )
            ),
            wrapper.actionParams
        )
        assertEquals(
            listOf<GeneratedKeyRequest?>(null, null),
            wrapper.actionGeneratedKeyRequests
        )
    }

    @Test
    fun `cascade insert generates UUID keys and propagates the parent key`() {
        val child = CascadeUuidKeyChild(name = "child")
        val parent = CascadeUuidKeyParent(name = "root", children = listOf(child))
        val wrapper = CascadeRecordingWrapper()

        parent.insert().execute(wrapper)

        val parentId = assertNotNull(parent.id)
        val childId = assertNotNull(child.id)
        assertEquals(parentId, UUID.fromString(parentId).toString())
        assertEquals(childId, UUID.fromString(childId).toString())
        assertEquals(parentId, child.parentId)
        assertEquals(
            listOf(
                "INSERT INTO `cascade_uuid_key_parent` (`id`, `name`) VALUES (:id, :name)",
                "INSERT INTO `cascade_uuid_key_child` (`id`, `parent_id`, `name`) VALUES (:id, :parentId, :name)"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf(
                mapOf<String, Any?>("id" to parentId, "name" to "root"),
                mapOf<String, Any?>("id" to childId, "parentId" to parentId, "name" to "child")
            ),
            wrapper.actionParams
        )
        assertEquals(
            listOf<GeneratedKeyRequest?>(null, null),
            wrapper.actionGeneratedKeyRequests
        )
    }

    @Test
    fun `cascade insert generates snowflake keys and propagates the parent key`() {
        val child = CascadeSnowflakeKeyChild(name = "child")
        val parent = CascadeSnowflakeKeyParent(name = "root", children = listOf(child))
        val wrapper = CascadeRecordingWrapper()

        parent.insert().execute(wrapper)

        val parentId = assertNotNull(parent.id)
        val childId = assertNotNull(child.id)
        assertTrue(parentId > 0L)
        assertTrue(childId > 0L)
        assertEquals(parentId, child.parentId)
        assertEquals(
            listOf(
                "INSERT INTO `cascade_snowflake_key_parent` (`id`, `name`) VALUES (:id, :name)",
                "INSERT INTO `cascade_snowflake_key_child` (`id`, `parent_id`, `name`) VALUES (:id, :parentId, :name)"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf(
                mapOf<String, Any?>("id" to parentId, "name" to "root"),
                mapOf<String, Any?>("id" to childId, "parentId" to parentId, "name" to "child")
            ),
            wrapper.actionParams
        )
        assertEquals(
            listOf<GeneratedKeyRequest?>(null, null),
            wrapper.actionGeneratedKeyRequests
        )
    }

    @Test
    fun `cascade insert generates custom keys and propagates the parent key`() {
        val previousGenerator = customIdGenerator
        val generatedIds = mutableListOf("custom-parent", "custom-child")
        customIdGenerator = object : KIdGenerator<String> {
            override fun nextId(): String = generatedIds.removeFirst()
        }

        try {
            val child = CascadeCustomKeyChild(name = "child")
            val parent = CascadeCustomKeyParent(name = "root", children = listOf(child))
            val wrapper = CascadeRecordingWrapper()

            parent.insert().execute(wrapper)

            assertEquals("custom-parent", parent.id)
            assertEquals("custom-child", child.id)
            assertEquals("custom-parent", child.parentId)
            assertEquals(emptyList(), generatedIds)
            assertEquals(
                listOf(
                    "INSERT INTO `cascade_custom_key_parent` (`id`, `name`) VALUES (:id, :name)",
                    "INSERT INTO `cascade_custom_key_child` (`id`, `parent_id`, `name`) VALUES (:id, :parentId, :name)"
                ),
                wrapper.actionSql
            )
            assertEquals(
                listOf(
                    mapOf<String, Any?>("id" to "custom-parent", "name" to "root"),
                    mapOf<String, Any?>(
                        "id" to "custom-child",
                        "parentId" to "custom-parent",
                        "name" to "child"
                    )
                ),
                wrapper.actionParams
            )
            assertEquals(
                listOf<GeneratedKeyRequest?>(null, null),
                wrapper.actionGeneratedKeyRequests
            )
        } finally {
            customIdGenerator = previousGenerator
        }
    }

    @Test
    fun `select cascade scans explicitly allowed relation fields outside sql projection`() {
        val parent = CascadeSelectBehaviorParent(id = 1)
        val children = listOf(
            CascadeSelectBehaviorChild(id = 11, parentId = 1, name = "alpha"),
            CascadeSelectBehaviorChild(id = 12, parentId = 1, name = "beta")
        )
        val wrapper = CascadeRecordingWrapper(queryResults = mutableListOf(listOf(parent), children))

        val rows = CascadeSelectBehaviorParent()
            .select()
            .cascade { [CascadeSelectBehaviorParent::children] }
            .toList<CascadeSelectBehaviorParent>(wrapper)

        assertEquals(
            listOf(ParentChildrenShape(1, listOf(11, 12))),
            rows.map { ParentChildrenShape(it.id, it.children!!.map { child -> child.id }) }
        )
        assertEquals(
            listOf(
                "SELECT `id`, `child_id` AS `childId` FROM `cascade_select_parent`",
                "SELECT `id`, `parent_id` AS `parentId`, `name` FROM `cascade_select_child` WHERE `parent_id` = :parentId"
            ),
            wrapper.querySql
        )
        assertEquals(
            listOf(
                emptyMap(),
                mapOf<String, Any?>("parentId" to 1)
            ),
            wrapper.queryParams
        )
    }

    @Test
    fun `setValues assigns collection children with a single batch query`() {
        val first = CascadeSelectBehaviorParent(id = 1)
        val second = CascadeSelectBehaviorParent(id = 2)
        val children = listOf(
            CascadeSelectBehaviorChild(id = 11, parentId = 1, name = "alpha"),
            CascadeSelectBehaviorChild(id = 12, parentId = 1, name = "beta"),
            CascadeSelectBehaviorChild(id = 21, parentId = 2, name = "gamma")
        )
        val wrapper = CascadeRecordingWrapper(queryResults = mutableListOf(children))
        val field = CascadeSelectBehaviorParent().__columns.single { it.name == "children" }
        val validRef = ValidCascade(field, field.cascade!!, CascadeSelectBehaviorChild(), CascadeSelectBehaviorParent().__tableName)

        CascadeSelectClause.setValues(
            parentRows = listOf(first, second),
            prop = "children",
            validRef = validRef,
            cascadeAllowed = null,
            cascadeSelectedProps = emptySet(),
            operationType = KOperationType.SELECT,
            wrapper = wrapper
        )

        assertEquals(
            listOf(
                ParentChildrenShape(1, listOf(11, 12)),
                ParentChildrenShape(2, listOf(21))
            ),
            listOf(first, second).map { ParentChildrenShape(it.id, it.children!!.map { child -> child.id }) }
        )
        assertEquals(
            listOf(
                "SELECT `id`, `parent_id` AS `parentId`, `name` FROM `cascade_select_child` WHERE `parent_id` IN (:parentId, :parentId@1)"
            ),
            wrapper.querySql
        )
        assertEquals(
            listOf(mapOf<String, Any?>("parentId" to 1, "parentId@1" to 2)),
            wrapper.queryParams
        )
    }

    @Test
    fun `setValues assigns composite key children with one or-of-and query`() {
        val first = CascadeCompositeParent(id1 = 1, id2 = 10)
        val second = CascadeCompositeParent(id1 = 2, id2 = 20)
        val children = listOf(
            CascadeCompositeChild(id = 11, parentId1 = 1, parentId2 = 10, name = "alpha"),
            CascadeCompositeChild(id = 21, parentId1 = 2, parentId2 = 20, name = "beta")
        )
        val wrapper = CascadeRecordingWrapper(queryResults = mutableListOf(children))
        val field = CascadeCompositeParent().__columns.single { it.name == "children" }
        val validRef = ValidCascade(field, field.cascade!!, CascadeCompositeChild(), CascadeCompositeParent().__tableName)

        CascadeSelectClause.setValues(
            parentRows = listOf(first, second),
            prop = "children",
            validRef = validRef,
            cascadeAllowed = null,
            cascadeSelectedProps = emptySet(),
            operationType = KOperationType.SELECT,
            wrapper = wrapper
        )

        assertEquals(
            listOf(
                ParentChildrenShape(1, listOf(11)),
                ParentChildrenShape(2, listOf(21))
            ),
            listOf(first, second).map { ParentChildrenShape(it.id1, it.children!!.map { child -> child.id }) }
        )
        assertEquals(
            listOf(
                "SELECT `id`, `parent_id1` AS `parentId1`, `parent_id2` AS `parentId2`, `name` FROM `cascade_composite_child` " +
                    "WHERE (`parent_id1` = :parentId1 AND `parent_id2` = :parentId2) OR (`parent_id1` = :parentId1@1 AND `parent_id2` = :parentId2@1)"
            ),
            wrapper.querySql
        )
        assertEquals(
            listOf(
                mapOf<String, Any?>(
                    "parentId1" to 1,
                    "parentId2" to 10,
                    "parentId1@1" to 2,
                    "parentId2@1" to 20
                )
            ),
            wrapper.queryParams
        )
    }

    @Test
    fun `setValues chunks large single key parent batches`() {
        val parents = (1..901).map { CascadeSelectBehaviorParent(id = it) }
        val wrapper = CascadeRecordingWrapper(
            queryResults = mutableListOf(emptyList(), emptyList())
        )
        val field = CascadeSelectBehaviorParent().__columns.single { it.name == "children" }
        val validRef = ValidCascade(field, field.cascade!!, CascadeSelectBehaviorChild(), CascadeSelectBehaviorParent().__tableName)

        CascadeSelectClause.setValues(
            parentRows = parents,
            prop = "children",
            validRef = validRef,
            cascadeAllowed = null,
            cascadeSelectedProps = emptySet(),
            operationType = KOperationType.SELECT,
            wrapper = wrapper
        )

        assertEquals(
            listOf(singleFkBatchSql(900), singleFkBatchSql(1)),
            wrapper.querySql
        )
        assertEquals(
            listOf(singleFkParams(1..900), singleFkParams(901..901)),
            wrapper.queryParams
        )
        assertEquals(
            (1..901).map { ParentChildrenShape(it, emptyList()) },
            parents.map { ParentChildrenShape(it.id, it.children!!.map { child -> child.id }) }
        )
    }

    @Test
    fun `setValues assigns single child with row scoped query`() {
        val parent = CascadeSelectBehaviorParent(id = 1, childId = 11)
        val child = CascadeSelectBehaviorChild(id = 11, parentId = 1, name = "alpha")
        val wrapper = CascadeRecordingWrapper(objectResults = mutableListOf(child))
        val field = CascadeSelectBehaviorParent().__columns.single { it.name == "child" }
        val validRef = ValidCascade(field, field.cascade!!, CascadeSelectBehaviorChild(), CascadeSelectBehaviorParent().__tableName)

        CascadeSelectClause.setValues(
            parentRows = listOf(parent),
            prop = "child",
            validRef = validRef,
            cascadeAllowed = null,
            cascadeSelectedProps = emptySet(),
            operationType = KOperationType.SELECT,
            wrapper = wrapper
        )

        assertEquals(
            ChildShape(id = 11, parentId = 1, name = "alpha"),
            parent.child!!.toChildShape()
        )
        assertEquals(
            listOf("SELECT `id`, `parent_id` AS `parentId`, `name` FROM `cascade_select_child` WHERE `id` = :id LIMIT 1"),
            wrapper.querySql
        )
    }

    @Test
    fun `node tree patches child foreign keys from parent values`() {
        val child = CascadeDeleteBehaviorChild(id = 10)
        val parent = CascadeDeleteBehaviorParent(id = 7, children = listOf(child))

        val node = parent.toTreeNode(
            data = NodeInfo(updateReferenceValue = true),
            cascadeAllowed = null,
            operationType = KOperationType.UPDATE,
            updateParams = mutableMapOf("id" to "idNew")
        )

        assertEquals(
            NodeShape(
                table = "cascade_delete_parent",
                values = mapOf("id" to 7, "children" to listOf(child)),
                updateParams = mapOf("id" to "idNew"),
                children = listOf(
                    NodeShape(
                        table = "cascade_delete_child",
                        values = mapOf("id" to 10, "parentId" to 7, "parent" to null),
                        updateParams = mapOf("parentId" to "idNew"),
                        children = emptyList()
                    )
                )
            ),
            node.toNodeShape()
        )
        assertEquals(7, child.parentId)
    }

    @Test
    fun `cascade delete keeps root task when selected records are empty`() {
        val root = KronosAtomicActionTask(
            sql = "DELETE FROM `cascade_delete_parent` WHERE `id` = :id",
            paramMap = mapOf("id" to 7),
            operationType = KOperationType.DELETE
        )
        val wrapper = CascadeRecordingWrapper(queryResults = mutableListOf(emptyList<KPojo>()))

        @Suppress("UNCHECKED_CAST")
        val task = CascadeDeleteClause.build(
            cascade = true,
            cascadeAllowed = null,
            targetType = typeOf<CascadeDeleteBehaviorParent>(),
            kClass = CascadeDeleteBehaviorParent::class as KClass<KPojo>,
            pojo = CascadeDeleteBehaviorParent(id = 7),
            where = null,
            paramMap = emptyMap(),
            logic = false,
            rootTask = root
        )
        task.execute(wrapper)

        assertEquals(
            listOf("SELECT `id` FROM `cascade_delete_parent`"),
            wrapper.querySql
        )
        assertEquals(
            listOf("DELETE FROM `cascade_delete_parent` WHERE `id` = :id"),
            wrapper.actionSql
        )
    }

    @Test
    fun `cascade delete prequery keeps inherited logical delete where as structured expression`() {
        val inheritedWhere = and(
            eq("id", "id"),
            eqNumber("deleted", "0")
        )
        val root = KronosAtomicActionTask(
            sql = "UPDATE `cascade_logic_delete_parent` SET `deleted` = :deletedNew WHERE `id` = :id AND `deleted` = 0",
            paramMap = mapOf("id" to 7, "deletedNew" to 1),
            operationType = KOperationType.DELETE
        )
        val wrapper = CascadeRecordingWrapper(queryResults = mutableListOf(emptyList<KPojo>()))

        @Suppress("UNCHECKED_CAST")
        val task = CascadeDeleteClause.build(
            cascade = true,
            cascadeAllowed = null,
            targetType = typeOf<CascadeLogicDeleteParent>(),
            kClass = CascadeLogicDeleteParent::class as KClass<KPojo>,
            pojo = CascadeLogicDeleteParent(id = 7),
            where = inheritedWhere,
            paramMap = mapOf("id" to 7, "deletedNew" to 1),
            logic = true,
            rootTask = root
        )
        task.execute(wrapper)

        assertEquals(
            listOf("SELECT `id`, `deleted` FROM `cascade_logic_delete_parent` WHERE `id` = :id AND `deleted` = 0"),
            wrapper.querySql
        )
        assertEquals(
            listOf(mapOf<String, Any?>("id" to 7)),
            wrapper.queryParams
        )
        assertEquals(
            listOf("UPDATE `cascade_logic_delete_parent` SET `deleted` = :deletedNew WHERE `id` = :id AND `deleted` = 0"),
            wrapper.actionSql
        )
    }

    @Test
    fun `cascade update keeps root task when selected records are empty`() {
        val root = KronosAtomicActionTask(
            sql = "UPDATE `cascade_delete_parent` SET `id` = :idNew WHERE `id` = :id",
            paramMap = mapOf("id" to 7, "idNew" to 8),
            operationType = KOperationType.UPDATE
        )
        val wrapper = CascadeRecordingWrapper(queryResults = mutableListOf(emptyList<KPojo>()))
        val idField = CascadeDeleteBehaviorParent().__columns.single { it.name == "id" }

        @Suppress("UNCHECKED_CAST")
        val task = CascadeUpdateClause.build(
            cascade = true,
            cascadeAllowed = null,
            pojo = CascadeDeleteBehaviorParent(id = 7),
            targetType = typeOf<CascadeDeleteBehaviorParent>(),
            kClass = CascadeDeleteBehaviorParent::class as KClass<KPojo>,
            paramMap = mapOf("id" to 7, "idNew" to 8),
            toUpdateFields = LinkedHashSet<Field>().also { it.add(idField) },
            where = null,
            rootTask = root
        )
        task.execute(wrapper)

        assertEquals(
            listOf("SELECT `id` FROM `cascade_delete_parent`"),
            wrapper.querySql
        )
        assertEquals(
            listOf("UPDATE `cascade_delete_parent` SET `id` = :idNew WHERE `id` = :id"),
            wrapper.actionSql
        )
    }

    @Test
    fun `cascade update prequery keeps inherited where parameters with suffixes`() {
        val inheritedWhere = and(
            eq("id", "id"),
            notEq("id", "id@1")
        )
        val root = KronosAtomicActionTask(
            sql = "UPDATE `cascade_action_parent` SET `id` = :idNew WHERE `id` = :id AND `id` <> :id@1",
            paramMap = mapOf("id" to 7, "id@1" to 8, "idNew" to 9),
            operationType = KOperationType.UPDATE
        )
        val wrapper = CascadeRecordingWrapper(queryResults = mutableListOf(emptyList<KPojo>()))
        val idField = CascadeActionParent().__columns.single { it.name == "id" }

        @Suppress("UNCHECKED_CAST")
        val task = CascadeUpdateClause.build(
            cascade = true,
            cascadeAllowed = null,
            pojo = CascadeActionParent(id = 7),
            targetType = typeOf<CascadeActionParent>(),
            kClass = CascadeActionParent::class as KClass<KPojo>,
            paramMap = mapOf("id" to 7, "id@1" to 8, "idNew" to 9),
            toUpdateFields = LinkedHashSet<Field>().also { it.add(idField) },
            where = inheritedWhere,
            rootTask = root
        )
        task.execute(wrapper)

        assertEquals(
            listOf("SELECT `id` FROM `cascade_action_parent` WHERE `id` = :id AND `id` <> :id@1"),
            wrapper.querySql
        )
        assertEquals(
            listOf(mapOf<String, Any?>("id" to 7, "id@1" to 8)),
            wrapper.queryParams
        )
        assertEquals(
            listOf("UPDATE `cascade_action_parent` SET `id` = :idNew WHERE `id` = :id AND `id` <> :id@1"),
            wrapper.actionSql
        )
    }

    @Test
    fun `cascade update replaces root task with child then parent updates`() {
        val root = KronosAtomicActionTask(
            sql = "UPDATE `cascade_action_parent` SET `id` = :idNew WHERE `id` = :id",
            paramMap = mapOf("id" to 7, "idNew" to 8),
            operationType = KOperationType.UPDATE
        )
        val parent = CascadeActionParent(id = 7)
        val child = CascadeActionChild(id = 30, parentId = 7)
        val wrapper = CascadeRecordingWrapper(
            queryResults = mutableListOf(listOf(parent), listOf(child))
        )
        val idField = CascadeActionParent().__columns.single { it.name == "id" }

        @Suppress("UNCHECKED_CAST")
        val task = CascadeUpdateClause.build(
            cascade = true,
            cascadeAllowed = null,
            pojo = CascadeActionParent(id = 7),
            targetType = typeOf<CascadeActionParent>(),
            kClass = CascadeActionParent::class as KClass<KPojo>,
            paramMap = mapOf("id" to 7, "idNew" to 8),
            toUpdateFields = LinkedHashSet<Field>().also { it.add(idField) },
            where = null,
            rootTask = root
        )
        task.execute(wrapper)

        assertEquals(
            listOf(
                "SELECT `id` FROM `cascade_action_parent`",
                "SELECT `id`, `parent_id` AS `parentId` FROM `cascade_action_child` WHERE `parent_id` = :parentId"
            ),
            wrapper.querySql
        )
        assertEquals(
            listOf(
                "UPDATE `cascade_action_child` SET `parent_id` = :parentIdNew WHERE `id` = :id AND `parent_id` = :parentId",
                "UPDATE `cascade_action_parent` SET `id` = :idNew WHERE `id` = :id"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf<Map<String, Any?>>(mapOf("parentIdNew" to 8, "id" to 30, "parentId" to 7), mapOf("id" to 7, "idNew" to 8)),
            wrapper.actionParams
        )
    }

    @Test
    fun `cascade delete replaces root task with child then parent deletes`() {
        val root = KronosAtomicActionTask(
            sql = "DELETE FROM `cascade_action_parent` WHERE `id` = :id",
            paramMap = mapOf("id" to 7),
            operationType = KOperationType.DELETE
        )
        val parent = CascadeActionParent(id = 7)
        val child = CascadeActionChild(id = 30, parentId = 7)
        val wrapper = CascadeRecordingWrapper(
            queryResults = mutableListOf(listOf(parent), listOf(child))
        )

        @Suppress("UNCHECKED_CAST")
        val task = CascadeDeleteClause.build(
            cascade = true,
            cascadeAllowed = null,
            targetType = typeOf<CascadeActionParent>(),
            kClass = CascadeActionParent::class as KClass<KPojo>,
            pojo = CascadeActionParent(id = 7),
            where = null,
            paramMap = emptyMap(),
            logic = false,
            rootTask = root
        )
        task.execute(wrapper)

        assertEquals(
            listOf(
                "SELECT `id` FROM `cascade_action_parent`",
                "SELECT `id`, `parent_id` AS `parentId` FROM `cascade_action_child` WHERE `parent_id` = :parentId"
            ),
            wrapper.querySql
        )
        assertEquals(
            listOf(
                "DELETE FROM `cascade_action_child` WHERE `id` = :id AND `parent_id` = :parentId",
                "DELETE FROM `cascade_action_parent` WHERE `id` = :id"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf<Map<String, Any?>>(mapOf("id" to 30, "parentId" to 7), mapOf("id" to 7)),
            wrapper.actionParams
        )
    }

    @Test
    fun `delete clause physical cascade keeps logic delete child as physical delete`() {
        val parent = CascadePhysicalDeleteParent(id = 7)
        val child = CascadePhysicalDeleteChild(id = 30, parentId = 7, deleted = false)
        val wrapper = CascadeRecordingWrapper(
            queryResults = mutableListOf(listOf(parent), listOf(child))
        )

        val task = CascadePhysicalDeleteParent(id = 7)
            .delete()
            .logic(false)
            .cascade(true)
            .by { it.id }
            .build(wrapper)

        task.execute(wrapper)

        assertEquals(
            listOf(
                "SELECT `id` FROM `cascade_physical_delete_parent` WHERE `id` = :id",
                "SELECT `id`, `parent_id` AS `parentId`, `deleted` FROM `cascade_physical_delete_child` WHERE `parent_id` = :parentId AND `deleted` = 0"
            ),
            wrapper.querySql
        )
        assertEquals(
            listOf<Map<String, Any?>>(
                mapOf("id" to 7),
                mapOf("parentId" to 7)
            ),
            wrapper.queryParams
        )
        assertEquals(
            listOf(
                "DELETE FROM `cascade_physical_delete_child` WHERE `id` = :id AND `parent_id` = :parentId AND `deleted` = :deleted",
                "DELETE FROM `cascade_physical_delete_parent` WHERE `id` = :id"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf<Map<String, Any?>>(
                mapOf("id" to 30, "parentId" to 7, "deleted" to 0),
                mapOf("id" to 7)
            ),
            wrapper.actionParams
        )
    }

    @Test
    fun `cascade delete one-to-one set null updates child reference before parent delete`() {
        val root = KronosAtomicActionTask(
            sql = "DELETE FROM `cascade_single_set_null_parent` WHERE `id` = :id",
            paramMap = mapOf("id" to 7),
            operationType = KOperationType.DELETE
        )
        val parent = CascadeSingleSetNullParent(id = 7)
        val child = CascadeSingleSetNullChild(id = 41, parentId = 7)
        val wrapper = CascadeRecordingWrapper(
            queryResults = mutableListOf(listOf(parent)),
            objectResults = mutableListOf(child)
        )

        @Suppress("UNCHECKED_CAST")
        val task = CascadeDeleteClause.build(
            cascade = true,
            cascadeAllowed = null,
            targetType = typeOf<CascadeSingleSetNullParent>(),
            kClass = CascadeSingleSetNullParent::class as KClass<KPojo>,
            pojo = CascadeSingleSetNullParent(id = 7),
            where = null,
            paramMap = emptyMap(),
            logic = false,
            rootTask = root
        )
        task.execute(wrapper)

        assertEquals(
            listOf(
                "SELECT `id` FROM `cascade_single_set_null_parent`",
                "SELECT `id`, `parent_id` AS `parentId` FROM `cascade_single_set_null_child` WHERE `parent_id` = :parentId LIMIT 1"
            ),
            wrapper.querySql
        )
        assertEquals(
            listOf<Map<String, Any?>>(
                emptyMap(),
                mapOf("parentId" to 7)
            ),
            wrapper.queryParams
        )
        assertEquals(
            listOf(
                "UPDATE `cascade_single_set_null_child` SET `parent_id` = :parentIdNew WHERE `id` = :id AND `parent_id` = :parentId",
                "DELETE FROM `cascade_single_set_null_parent` WHERE `id` = :id"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf<Map<String, Any?>>(
                mapOf("parentIdNew" to null, "id" to 41, "parentId" to 7),
                mapOf("id" to 7)
            ),
            wrapper.actionParams
        )
    }

    @Test
    fun `cascade delete set null updates child reference before parent delete`() {
        val root = KronosAtomicActionTask(
            sql = "DELETE FROM `cascade_set_null_parent` WHERE `id` = :id",
            paramMap = mapOf("id" to 7),
            operationType = KOperationType.DELETE
        )
        val parent = CascadeSetNullParent(id = 7)
        val child = CascadeSetNullChild(id = 31, parentId = 7)
        val wrapper = CascadeRecordingWrapper(
            queryResults = mutableListOf(listOf(parent), listOf(child))
        )

        @Suppress("UNCHECKED_CAST")
        val task = CascadeDeleteClause.build(
            cascade = true,
            cascadeAllowed = null,
            targetType = typeOf<CascadeSetNullParent>(),
            kClass = CascadeSetNullParent::class as KClass<KPojo>,
            pojo = CascadeSetNullParent(id = 7),
            where = null,
            paramMap = emptyMap(),
            logic = false,
            rootTask = root
        )
        task.execute(wrapper)

        assertEquals(
            listOf(
                "SELECT `id` FROM `cascade_set_null_parent`",
                "SELECT `id`, `parent_id` AS `parentId` FROM `cascade_set_null_child` WHERE `parent_id` = :parentId"
            ),
            wrapper.querySql
        )
        assertEquals(
            listOf(
                "UPDATE `cascade_set_null_child` SET `parent_id` = :parentIdNew WHERE `id` = :id AND `parent_id` = :parentId",
                "DELETE FROM `cascade_set_null_parent` WHERE `id` = :id"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf<Map<String, Any?>>(mapOf("parentIdNew" to null, "id" to 31, "parentId" to 7), mapOf("id" to 7)),
            wrapper.actionParams
        )
    }

    @Test
    fun `cascade delete set default updates child reference before parent delete`() {
        val root = KronosAtomicActionTask(
            sql = "DELETE FROM `cascade_set_default_parent` WHERE `id` = :id",
            paramMap = mapOf("id" to 7),
            operationType = KOperationType.DELETE
        )
        val parent = CascadeSetDefaultParent(id = 7)
        val child = CascadeSetDefaultChild(id = 32, parentId = 7)
        val wrapper = CascadeRecordingWrapper(
            queryResults = mutableListOf(listOf(parent), listOf(child))
        )

        @Suppress("UNCHECKED_CAST")
        val task = CascadeDeleteClause.build(
            cascade = true,
            cascadeAllowed = null,
            targetType = typeOf<CascadeSetDefaultParent>(),
            kClass = CascadeSetDefaultParent::class as KClass<KPojo>,
            pojo = CascadeSetDefaultParent(id = 7),
            where = null,
            paramMap = emptyMap(),
            logic = false,
            rootTask = root
        )
        task.execute(wrapper)

        assertEquals(
            listOf(
                "SELECT `id` FROM `cascade_set_default_parent`",
                "SELECT `id`, `parent_id` AS `parentId` FROM `cascade_set_default_child` WHERE `parent_id` = :parentId"
            ),
            wrapper.querySql
        )
        assertEquals(
            listOf(
                "UPDATE `cascade_set_default_child` SET `parent_id` = :parentIdNew WHERE `id` = :id AND `parent_id` = :parentId",
                "DELETE FROM `cascade_set_default_parent` WHERE `id` = :id"
            ),
            wrapper.actionSql
        )
        assertEquals(
            listOf<Map<String, Any?>>(mapOf("parentIdNew" to 0, "id" to 32, "parentId" to 7), mapOf("id" to 7)),
            wrapper.actionParams
        )
    }

    @Test
    fun `cascade delete restrict rejects non empty child reference`() {
        val root = KronosAtomicActionTask(
            sql = "DELETE FROM `cascade_restrict_parent` WHERE `id` = :id",
            paramMap = mapOf("id" to 7),
            operationType = KOperationType.DELETE
        )
        val parent = CascadeRestrictParent(id = 7)
        val child = CascadeRestrictChild(id = 33, parentId = 7)
        val wrapper = CascadeRecordingWrapper(
            queryResults = mutableListOf(listOf(parent), listOf(child))
        )

        @Suppress("UNCHECKED_CAST")
        val task = CascadeDeleteClause.build(
            cascade = true,
            cascadeAllowed = null,
            targetType = typeOf<CascadeRestrictParent>(),
            kClass = CascadeRestrictParent::class as KClass<KPojo>,
            pojo = CascadeRestrictParent(id = 7),
            where = null,
            paramMap = emptyMap(),
            logic = false,
            rootTask = root
        )
        assertFailsWith<UnsupportedOperationException> {
            task.execute(wrapper)
        }
        assertEquals(
            listOf(
                "SELECT `id` FROM `cascade_restrict_parent`",
                "SELECT `id`, `parent_id` AS `parentId` FROM `cascade_restrict_child` WHERE `parent_id` = :parentId"
            ),
            wrapper.querySql
        )
        assertEquals(emptyList(), wrapper.actionSql)
    }

    private data class ParentChildrenShape(val parentId: Int?, val childIds: List<Int?>)
    private data class ChildShape(val id: Int?, val parentId: Int?, val name: String?)

    private fun CascadeSelectBehaviorChild.toChildShape() = ChildShape(id, parentId, name)

    private data class NodeShape(
        val table: String,
        val values: Map<String, Any?>,
        val updateParams: Map<String, String>,
        val children: List<NodeShape>
    )

    private fun NodeOfKPojo.toNodeShape(): NodeShape = NodeShape(
        table = tableName,
        values = dataMap,
        updateParams = updateParams,
        children = children.map { it.toNodeShape() }
    )

    private fun singleFkBatchSql(count: Int): String {
        val parameters = (0 until count).joinToString(", ") { index -> ":${suffixedName("parentId", index)}" }
        return "SELECT `id`, `parent_id` AS `parentId`, `name` FROM `cascade_select_child` WHERE `parent_id` IN ($parameters)"
    }

    private fun singleFkParams(values: IntRange): Map<String, Any?> =
        values.withIndex().associateTo(linkedMapOf()) { (index, value) ->
            suffixedName("parentId", index) to value
        }

    private fun suffixedName(baseName: String, index: Int): String =
        if (index == 0) baseName else "$baseName@$index"

    private fun eq(columnName: String, parameterName: String): SqlExpr =
        SqlExpr.Binary(
            SqlExpr.Column(columnName = columnName),
            SqlBinaryOperator.Equal,
            SqlExpr.Parameter(SqlParameter.Named(parameterName))
        )

    private fun notEq(columnName: String, parameterName: String): SqlExpr =
        SqlExpr.Binary(
            SqlExpr.Column(columnName = columnName),
            SqlBinaryOperator.NotEqual,
            SqlExpr.Parameter(SqlParameter.Named(parameterName))
        )

    private fun eqNumber(columnName: String, value: String): SqlExpr =
        SqlExpr.Binary(
            SqlExpr.Column(columnName = columnName),
            SqlBinaryOperator.Equal,
            SqlExpr.NumberLiteral(value)
        )

    private fun and(left: SqlExpr, right: SqlExpr): SqlExpr =
        SqlExpr.Binary(left, SqlBinaryOperator.And, right)

    private class CascadeRecordingWrapper(
        private val queryResults: MutableList<List<KPojo>> = mutableListOf(),
        private val objectResults: MutableList<KPojo?> = mutableListOf(),
        private val generatedKeys: MutableList<Long> = mutableListOf()
    ) : KronosDataSourceWrapper {
        val querySql = mutableListOf<String>()
        val queryParams = mutableListOf<Map<String, Any?>>()
        val actionSql = mutableListOf<String>()
        val actionParams = mutableListOf<Map<String, Any?>>()
        val actionGeneratedKeyRequests = mutableListOf<GeneratedKeyRequest?>()
        override val url: String = "jdbc:mysql://localhost:3306/kronos"
        override val userName: String = "kronos"
        override val dbType: DBType = DBType.Mysql

        override fun toList(task: KAtomicQueryTask): List<Any?> {
            querySql += task.sql
            queryParams += task.paramMap
            return queryResults.removeFirstOrNull().orEmpty()
        }

        override fun first(task: KAtomicQueryTask): Any? {
            querySql += task.sql
            queryParams += task.paramMap
            return objectResults.removeFirstOrNull()
        }

        override fun update(task: KAtomicActionTask): Int {
            actionSql += task.sql
            actionParams += task.paramMap
            actionGeneratedKeyRequests += task.generatedKeyRequest
            generatedKeys.removeFirstOrNull()?.let { generatedKey ->
                task.generatedKeys += generatedKey
                task.lastInsertId = generatedKey
            }
            return 1
        }

        override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
            actionSql += task.sql
            actionParams += task.paramMapArr?.firstOrNull().orEmpty()
            return IntArray(task.paramMapArr?.size ?: 0) { 1 }
        }

        override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? =
            TransactionScope().block()
    }
}
