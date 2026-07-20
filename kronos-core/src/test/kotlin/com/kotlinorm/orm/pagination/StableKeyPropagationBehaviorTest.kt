/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.pagination

import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.NonNull
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.selectGeneratedProjection
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Table("stable_column_user")
data class StableColumnUser(
    @PrimaryKey
    @Column("stable_user_id")
    var userId: Int? = null,
    @Column("display_name")
    var displayName: String? = null
) : KPojo

@Table("stable_composite_key_row")
@TableIndex("uq_stable_tenant_external", ["tenant_id", "external_code"], type = "UNIQUE")
@TableIndex("uq_stable_tenant_optional", ["tenant_id", "optional_code"], type = "UNIQUE")
data class StableCompositeKeyRow(
    @NonNull
    @Column("tenant_id")
    var tenantId: Int? = null,
    @NonNull
    @Column("external_code")
    var externalCode: String? = null,
    @Column("optional_code")
    var optionalCode: String? = null
) : KPojo

@Table("stable_key_level_one")
data class StableKeyLevelOne(var keyOne: Int? = null) : KPojo

@Table("stable_key_level_two")
data class StableKeyLevelTwo(var keyTwo: Int? = null) : KPojo

@Table("stable_key_level_three")
data class StableKeyLevelThree(var keyThree: Int? = null) : KPojo

@Table("stable_key_inspection_row")
data class StableKeyInspectionRow(var userId: Int? = null) : KPojo

class StableKeyPropagationBehaviorTest : MysqlTestBase() {

    @Test
    fun `select all propagates a physical primary key by its logical output label`() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val innerPage = StableColumnUser()
            .select()
            .orderBy { it.userId.asc() }
            .page(1, 2)
        val cursor = innerPage
            .select { it.userId }
            .orderBy { it.userId.asc() }
            .cursor(1)

        assertEquals(listOf(listOf("userId")), innerPage.outputStableKeyCandidates())
        assertEquals(
            "SELECT `q`.`userId` FROM " +
                "(SELECT `stable_user_id` AS `userId`, `display_name` AS `displayName` " +
                "FROM `stable_column_user` ORDER BY `userId` ASC LIMIT 2 OFFSET 0) AS `q` " +
                "ORDER BY `q`.`userId` ASC LIMIT 2",
            cursor.build(wrapper).atomicTask.sql
        )
        val cursorStatement = cursor.toSqlQuery(wrapper) as SqlQuery.Select
        assertEquals(
            innerPage.toSqlQuery(wrapper),
            (cursorStatement.from.single() as SqlTable.Subquery).query
        )
    }

    @Test
    fun `composite unique key propagates only when every non null member is projected`() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val completePage = StableCompositeKeyRow()
            .select { [it.tenantId, it.externalCode] }
            .orderBy { it.tenantId.asc() }
            .page(1, 3)
        val completeCursor = completePage
            .select { [it.tenantId, it.externalCode] }
            .orderBy { it.tenantId.asc() }
            .cursor(2)

        assertEquals(
            listOf(listOf("tenantId", "externalCode")),
            completePage.outputStableKeyCandidates()
        )
        assertEquals(
            "SELECT `q`.`tenantId`, `q`.`externalCode` FROM " +
                "(SELECT `tenant_id` AS `tenantId`, `external_code` AS `externalCode` " +
                "FROM `stable_composite_key_row` ORDER BY `tenantId` ASC LIMIT 3 OFFSET 0) AS `q` " +
                "ORDER BY `q`.`tenantId` ASC, `q`.`externalCode` ASC LIMIT 3",
            completeCursor.build(wrapper).atomicTask.sql
        )

        val incompletePage = StableCompositeKeyRow()
            .select { it.tenantId }
            .page(1, 3)
        val incompleteOuter = incompletePage
            .select { it.tenantId }
            .orderBy { it.tenantId.asc() }
        assertEquals(emptyList(), incompletePage.outputStableKeyCandidates())
        assertEquals(
            "Cursor pagination requires a primary key or unique key tie-breaker.",
            assertFailsWith<IllegalArgumentException> { incompleteOuter.cursor(2) }.message
        )

        val nullablePage = StableCompositeKeyRow()
            .select { [it.tenantId, it.optionalCode] }
            .page(1, 3)
        val nullableOuter = nullablePage
            .select { [it.tenantId, it.optionalCode] }
            .orderBy { it.tenantId.asc() }
        assertEquals(emptyList(), nullablePage.outputStableKeyCandidates())
        assertEquals(
            "Cursor pagination requires a primary key or unique key tie-breaker.",
            assertFailsWith<IllegalArgumentException> { nullableOuter.cursor(2) }.message
        )
    }

    @Test
    fun `stable key aliases propagate across multiple derived layers without changing the paged source`() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val innerPage = TestUser()
            .select(StableKeyLevelOne::class) { it.id.alias("keyOne") }
            .orderBy { it.id.asc() }
            .page(2, 2)
        val levelTwo = innerPage
            .selectGeneratedProjection<StableKeyLevelOne, StableKeyLevelTwo, StableKeyLevelOne>(
                StableKeyLevelTwo::class,
                StableKeyLevelOne::class
            ) { it.keyOne.alias("keyTwo") }
        val levelThree = levelTwo
            .selectGeneratedProjection<StableKeyLevelTwo, StableKeyLevelThree, StableKeyLevelTwo>(
                StableKeyLevelThree::class,
                StableKeyLevelTwo::class
            ) { it.keyTwo.alias("keyThree") }
        val cursor = levelThree
            .select { it.keyThree }
            .orderBy { it.keyThree.asc() }
            .cursor(2)

        assertEquals(listOf(listOf("keyOne")), innerPage.outputStableKeyCandidates())
        assertEquals(listOf(listOf("keyTwo")), levelTwo.outputStableKeyCandidates())
        assertEquals(listOf(listOf("keyThree")), levelThree.outputStableKeyCandidates())
        assertEquals(
            "SELECT `q`.`keyThree` FROM " +
                "(SELECT `q`.`keyTwo` AS `keyThree` FROM " +
                "(SELECT `q`.`keyOne` AS `keyTwo` FROM " +
                "(SELECT `id` AS `keyOne` FROM `tb_user` WHERE `deleted` = 0 " +
                "ORDER BY `id` ASC LIMIT 2 OFFSET 2) AS `q`) AS `q`) AS `q` " +
                "ORDER BY `q`.`keyThree` ASC LIMIT 3",
            cursor.build(wrapper).atomicTask.sql
        )

        val outerStatement = cursor.toSqlQuery(wrapper) as SqlQuery.Select
        val levelThreeStatement = (outerStatement.from.single() as SqlTable.Subquery).query as SqlQuery.Select
        val levelTwoStatement = (levelThreeStatement.from.single() as SqlTable.Subquery).query as SqlQuery.Select
        val innerStatement = (levelTwoStatement.from.single() as SqlTable.Subquery).query
        assertEquals(innerPage.toSqlQuery(wrapper), innerStatement)
    }

    @Test
    fun `join and union queries do not expose unproven root keys through offset wrappers`() {
        val joined = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { [user.id, relation.id.alias("relationId")] }
        }
        val joinedPage = joined.page(1, 2)
        val unioned = union(
            TestUser().select { it.id },
            TestUser().select { it.id }
        ).limit(2, 0)
        val unionPage = OffsetPageQuery(unioned, pageIndex = 1, pageSize = 2)

        assertEquals(emptyList(), joined.outputStableKeyCandidates())
        assertEquals(emptyList(), joinedPage.outputStableKeyCandidates())
        assertEquals(emptyList(), unioned.outputStableKeyCandidates())
        assertEquals(emptyList(), unionPage.outputStableKeyCandidates())
    }

    @Test
    fun `stable key inspection does not resolve a wrapper or build a SQL plan`() {
        val selectable = StableKeyRecordingSelectable()
        val page = OffsetPageQuery(selectable, pageIndex = 1, pageSize = 2)

        assertEquals(listOf(listOf("userId")), page.outputStableKeyCandidates())
        assertEquals(1, selectable.stableKeyCalls)
        assertEquals(0, selectable.buildCalls)
        assertEquals(0, selectable.planCalls)
    }
}

private class StableKeyRecordingSelectable : KSelectable<StableKeyInspectionRow>(StableKeyInspectionRow()) {
    override val selectedType = typeOf<StableKeyInspectionRow>()
    var stableKeyCalls = 0
    var buildCalls = 0
    var planCalls = 0

    override fun outputStableKeyCandidates(): List<List<String>> {
        stableKeyCalls++
        return listOf(listOf("userId"))
    }

    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        buildCalls++
        error("Stable-key inspection must not build the query.")
    }

    override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan {
        planCalls++
        error("Stable-key inspection must not plan SQL.")
    }
}
