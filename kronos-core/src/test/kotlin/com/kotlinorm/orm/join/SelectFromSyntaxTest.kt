/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.join

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SelectFromSyntaxTest : MysqlTestBase() {
    @Test
    fun `relation chain builds a left-associated join tree`() {
        val statement = TestUser().join(UserRelation(), JoinTreeC()) { user, relation, third ->
            leftJoin { user.id == relation.id2 }
                .rightJoin { relation.id == third.relationId }
                .select { [user.username, relation.gender, third.label] }
        }.toSqlQuery() as SqlQuery.Select

        val root = assertIs<SqlTable.Join>(statement.from.single())
        assertEquals(SqlJoinType.Right, root.joinType)
        assertIs<SqlTable.Join>(root.left)
        assertIs<SqlTable.Ident>(root.right)
    }

    @Test
    fun `right nested raw join keeps parentheses-worthy AST shape and leaf order`() {
        val relationAndThird = UserRelation().join(JoinTreeC()) { relation, third ->
            innerJoin { relation.id == third.relationId }
        }

        val statement = TestUser().join(relationAndThird) { user, relation, third ->
            leftJoin { user.id == relation.id2 }
                .select { [user.username, relation.gender, third.label] }
        }.toSqlQuery() as SqlQuery.Select

        val root = assertIs<SqlTable.Join>(statement.from.single())
        assertEquals(SqlJoinType.Left, root.joinType)
        assertIs<SqlTable.Ident>(root.left)
        val nested = assertIs<SqlTable.Join>(root.right)
        assertEquals(SqlJoinType.Inner, nested.joinType)
        assertEquals(
            listOf("tb_user", "user_relation", "join_tree_c"),
            listOf(root.left, nested.left, nested.right).map { (it as SqlTable.Ident).name }
        )
    }

    @Test
    fun `left nested raw join remains a distinct AST shape`() {
        val userAndRelation = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
        }

        val statement = userAndRelation.join(JoinTreeC()) { user, relation, third ->
            leftJoin { relation.id == third.relationId }
                .select { [user.username, relation.gender, third.label] }
        }.toSqlQuery() as SqlQuery.Select

        val root = assertIs<SqlTable.Join>(statement.from.single())
        assertEquals(SqlJoinType.Left, root.joinType)
        assertIs<SqlTable.Join>(root.left)
        assertIs<SqlTable.Ident>(root.right)
    }

    @Test
    fun `all explicit relation kinds map to their SQL join types`() {
        assertJoinType(SqlJoinType.Left) { user, relation -> leftJoin { user.id == relation.id2 } }
        assertJoinType(SqlJoinType.Right) { user, relation -> rightJoin { user.id == relation.id2 } }
        assertJoinType(SqlJoinType.Inner) { user, relation -> innerJoin { user.id == relation.id2 } }
        assertJoinType(SqlJoinType.Full) { user, relation -> fullJoin { user.id == relation.id2 } }

        val cross = TestUser().join(UserRelation()) { _, _ ->
            crossJoin().select { it.id }
        }.toSqlQuery() as SqlQuery.Select
        val crossTable = assertIs<SqlTable.Join>(cross.from.single())
        assertEquals(SqlJoinType.Cross, crossTable.joinType)
        assertEquals(null, crossTable.condition)
    }

    @Test
    fun `join source and selected query expose separate public stages`() {
        val raw = TestUser().join(UserRelation()) { user, relation ->
            leftJoin { user.id == relation.id2 }
        }
        val query = raw.select { it.id }

        val rawMethods = raw::class.members.map { it.name }.toSet()
        val queryMethods = query::class.members.map { it.name }.toSet()

        assertTrue("select" in rawMethods)
        assertFalse("build" in rawMethods)
        assertFalse("where" in rawMethods)
        assertTrue("build" in queryMethods)
        assertTrue("where" in queryMethods)
        assertFalse("leftJoin" in queryMethods)
    }

    @Test
    fun `where without lambda retains root query-by-example semantics`() {
        val task = TestUser(id = 7).join(UserRelation()) { user, relation ->
            leftJoin { user.id == relation.id2 }
                .select { user.id }
                .where()
        }.build().atomicTask

        assertEquals(mapOf("id" to 7), task.paramMap)
        assertTrue(task.sql.contains("WHERE `tb_user`.`id` = :id"))
    }

    @Test
    fun `by allocates distinct parameters for same-named self-join fields`() {
        val first = JoinIdentityRow(id = 10)
        val second = JoinIdentityRow(id = 20)
        val task = first.join(second) { left, right ->
            innerJoin { left.parentId == right.id }
                .select { left.id }
                .by { [left.id, right.id] }
        }.build().atomicTask

        assertEquals(mapOf("id" to 10, "id@1" to 20), task.paramMap)
        assertTrue(task.sql.contains(":id"))
        assertTrue(task.sql.contains(":id@1"))
    }

    private fun assertJoinType(
        expected: SqlJoinType,
        relation: JoinSource2<TestUser, UserRelation>.(TestUser, UserRelation) -> JoinSource2<TestUser, UserRelation>
    ) {
        val statement = TestUser().join(UserRelation()) { user, joined ->
            relation(user, joined).select { user.id }
        }.toSqlQuery() as SqlQuery.Select
        val join = assertIs<SqlTable.Join>(statement.from.single())
        assertEquals(expected, join.joinType)
        assertIs<SqlJoinCondition.On>(join.condition)
        assertEquals(SqlBinaryOperator.Equal, (join.condition as SqlJoinCondition.On).condition.let {
            (it as SqlExpr.Binary).operator
        })
    }
}

@Table("join_tree_c")
data class JoinTreeC(
    var id: Int? = null,
    var relationId: Int? = null,
    var label: String? = null
) : KPojo

@Table("join_identity_row")
data class JoinIdentityRow(
    @PrimaryKey
    var id: Int? = null,
    var parentId: Int? = null
) : KPojo
