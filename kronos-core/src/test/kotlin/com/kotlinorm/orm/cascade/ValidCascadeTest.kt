package com.kotlinorm.orm.cascade

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.Table
import com.kotlinorm.cache.kPojoFieldMapCache
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidCascadeTest : MysqlTestBase() {

    data class CascadeSelectChild(
        val id: Int? = null,
        val parentId: Int? = null
    ) : KPojo

    data class CascadeSelectParent(
        val id: Int? = null,
        @Cascade(["id"], ["parentId"])
        @Ignore([IgnoreAction.CASCADE_SELECT])
        val cascadeSelectChild: CascadeSelectChild? = null
    ) : KPojo

    data class CascadeSelectParent2(
        val id: Int? = null,
    ) : KPojo

    @Table("cascade_select_child")
    data class CascadeSelectChild2(
        val id: Int? = null,
        val parentId: Int? = null,
        @Cascade(["parentId"], ["id"])
        val parent: CascadeSelectParent2? = null
    ) : KPojo

    data class CascadeDeleteParent(
        val id: Int? = null,
        val children: List<CascadeDeleteChild>? = null
    ) : KPojo

    data class CascadeDeleteChild(
        val id: Int? = null,
        val parentId: Int? = null,
        @Cascade(["parentId"], ["id"])
        val parent: CascadeDeleteParent? = null
    ) : KPojo

    @Table("cascade_delete_parent")
    data class CascadeDeleteParent2(
        val id: Int? = null,
        @Cascade(["id"], ["parentId"])
        val children: List<CascadeDeleteChild>? = null
    ) : KPojo

    data class CascadeDeleteChild2(
        val id: Int? = null,
        val parentId: Int? = null,
    ) : KPojo

    @Test
    fun `should ignore CASCADE_SELECT fields in SELECT operation`() {
        val ignoreField = kPojoFieldMapCache[CascadeSelectParent().__kType]!!["cascadeSelectChild"]!!

        val result = findValidRefs(
            sourceType = typeOf<CascadeSelectParent>(),
            columns = [ignoreField],
            operationType = KOperationType.SELECT,
            allowed = null,
            allowAll = true
        )

        assertTrue(result.isEmpty(), "应忽略带 CASCADE_SELECT 标记的字段")
    }

    @Test
    fun `should find direct cascades for SELECT operation`() {
        val field = kPojoFieldMapCache[CascadeSelectChild2().__kType]!!["parent"]!!
        val result = findValidRefs(
            sourceType = typeOf<CascadeSelectParent2>(),
            columns = [field],
            operationType = KOperationType.SELECT,
            allowed = null,
            allowAll = true
        )

        assertEquals(1, result.size, "应找到直接级联关系")
        with(result.first()) {
            assertEquals("parent", field.name, "字段名称匹配")
            assertTrue(refPojo is CascadeSelectParent2, "引用类型应为 CascadeSelectParent2")
            assertEquals("cascade_select_child", tableName, "表名应来自目标实体")
        }
    }

    @Test
    fun `should find reverse cascades for DELETE operation`() {
        val field = kPojoFieldMapCache[CascadeDeleteParent().__kType]!!["children"]!!
        val result = findValidRefs(
            sourceType = typeOf<CascadeDeleteParent>(),
            columns = [field],
            operationType = KOperationType.DELETE,
            allowed = null,
            allowAll = true
        )

        assertEquals(1, result.size, "应找到反向级联关系")
        with(result.first()) {
            assertEquals("children", field.name, "字段名称匹配")
            assertTrue(refPojo is CascadeDeleteChild, "引用类型应为 CascadeDeleteChild")
            assertEquals("cascade_delete_child", tableName, "表名应来自目标实体")
        }
    }

    @Test
    fun `should filter invalid cascades for DELETE operation`() {
        val field = kPojoFieldMapCache[CascadeDeleteParent2().__kType]!!["children"]!!

        val result = findValidRefs(
            sourceType = typeOf<CascadeDeleteChild2>(),
            columns = [field],
            operationType = KOperationType.DELETE,
            allowed = null,
            allowAll = true
        )

        assertTrue(result.isEmpty(), "DELETE 操作应过滤不支持的级联字段")
    }
}
