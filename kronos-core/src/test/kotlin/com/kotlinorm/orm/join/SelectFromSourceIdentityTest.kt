/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.join

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UnsafeProjectionOverride
import com.kotlinorm.beans.dsl.SourceIdentityScope
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.normalizeSql
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleSqliteJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SelectFromSourceIdentityTest : MysqlTestBase() {
    @Test
    fun `resolver falls back when receiver or active frames do not identify the source`() {
        val source = SourceIdentityCustomer()
        val sameValuesButDifferentIdentity = SourceIdentityCustomer()

        assertEquals("declared_table", SourceIdentityScope.resolveTableName(Any(), "declared_table"))
        assertEquals("declared_table", SourceIdentityScope.resolveTableName(source, "declared_table"))

        val frame = SourceIdentityScope.frame(listOf(source))
        SourceIdentityScope.withFrame(frame) {
            assertEquals(
                "declared_table",
                SourceIdentityScope.resolveTableName(sameValuesButDifferentIdentity, "declared_table")
            )
            assertTrue(frame.contains(source))
            assertFalse(frame.contains(sameValuesButDifferentIdentity))
            assertSame(source, frame.sourceOf(source)?.pojo)
            assertNull(frame.sourceOf(sameValuesButDifferentIdentity))
        }

        assertEquals("declared_table", SourceIdentityScope.resolveTableName(source, "declared_table"))
    }

    @Test
    fun `frame aliases preserve identity reserved names and unique table mappings`() {
        val customer = SourceIdentityCustomer()
        val firstInvoice = SourceIdentityInvoice()
        val secondInvoice = SourceIdentityInvoice()
        val unknownInvoice = SourceIdentityInvoice()
        val frame = SourceIdentityScope.frame(listOf(customer, firstInvoice, secondInvoice))

        assertNull(frame.aliasForSource(customer))
        assertNull(frame.aliasForSource(unknownInvoice))
        assertEquals(
            "source_identity_customer__k3",
            frame.aliasFor(customer, setOf("source_identity_customer__k1", "source_identity_customer__k2"))
        )
        assertEquals("source_identity_customer__k3", frame.existingAliasFor(customer))
        assertEquals("source_identity_invoice__k2", frame.aliasForSource(firstInvoice))
        assertEquals("source_identity_invoice__k3", frame.aliasForSource(secondInvoice))
        assertEquals("source_identity_invoice__k4", frame.aliasFor(unknownInvoice))
        assertTrue(frame.hasPhysicalTable("source_identity_invoice"))
        assertTrue(frame.hasDuplicatePhysicalTable("source_identity_invoice"))
        assertFalse(frame.hasDuplicatePhysicalTable("source_identity_customer"))
        assertTrue(frame.hasAliasForPhysicalTable("source_identity_customer"))
        assertEquals(
            setOf(
                "source_identity_customer__k3",
                "source_identity_invoice__k2",
                "source_identity_invoice__k3",
                "source_identity_invoice__k4"
            ),
            frame.allAliases().toSet()
        )
        assertEquals(
            mapOf("source_identity_customer" to "source_identity_customer__k3"),
            frame.aliasesByTableName()
        )
    }

    @Test
    fun `blank captured table name uses declared name and current runtime name for aliases`() {
        val source = MutableSourceIdentityPojo("")
        val frame = SourceIdentityScope.frame(listOf(source))

        SourceIdentityScope.withFrame(frame) {
            assertEquals("declared_table", SourceIdentityScope.resolveTableName(source, "declared_table"))
        }

        source.__tableName = "runtime_table"
        assertEquals("runtime_table__k1", frame.aliasFor(source))
        assertEquals("runtime_table__k1", frame.aliasForSource(source))
        assertEquals(mapOf("" to "runtime_table__k1"), frame.aliasesByTableName())
    }

    @Test
    fun `nested same-table frame reserves outer alias for the inner source`() {
        val outer = SourceIdentityInvoice()
        val inner = SourceIdentityInvoice()
        val outerFrame = SourceIdentityScope.frame(listOf(outer))
        val innerFrame = SourceIdentityScope.frame(listOf(inner))

        SourceIdentityScope.withFrame(outerFrame) {
            SourceIdentityScope.withFrame(innerFrame) {
                assertEquals(
                    "source_identity_invoice__k1",
                    SourceIdentityScope.resolveTableName(outer, outer.__tableName)
                )
                assertEquals(
                    "source_identity_invoice__k2",
                    SourceIdentityScope.resolveTableName(inner, inner.__tableName)
                )
            }
        }

        assertEquals("source_identity_invoice__k1", outerFrame.existingAliasFor(outer))
        assertEquals("source_identity_invoice__k2", innerFrame.existingAliasFor(inner))
    }

    @Test
    fun `equal self-join instances retain distinct identity aliases`() {
        val left = JoinIdentityRow(id = 1)
        val right = JoinIdentityRow(id = 1)
        val statement = left.join(right) { first, second ->
            innerJoin { first.id == second.parentId }
                .select { [first.id, second.parentId] }
        }.toSqlQuery() as SqlQuery.Select

        val join = statement.from.single() as SqlTable.Join
        val leftTable = join.left as SqlTable.Ident
        val rightTable = join.right as SqlTable.Ident

        assertNotEquals(leftTable.alias, rightTable.alias)
        assertEquals("join_identity_row__k1", leftTable.alias?.alias)
        assertEquals("join_identity_row__k2", rightTable.alias?.alias)
        assertSame(left, left)
        assertSame(right, right)
    }

    @OptIn(UnsafeProjectionOverride::class)
    @Test
    fun `nested source rebinding reserves aliases across the final leaf set`() {
        val nestedLeft = JoinIdentityRow(id = 2)
        val nestedRight = JoinIdentityRow(id = 3)
        val nested = nestedLeft.join(nestedRight) { first, second ->
            innerJoin { first.id == second.parentId }
        }
        val root = JoinIdentityRow(id = 1)
        val statement = root.join(nested) { first, second, third ->
            leftJoin { first.id == second.parentId }
                .select { [first.id, second.id, third.id] }
        }.toSqlQuery() as SqlQuery.Select

        val outer = statement.from.single() as SqlTable.Join
        val inner = outer.right as SqlTable.Join
        val aliases = listOf(outer.left, inner.left, inner.right)
            .map { (it as SqlTable.Ident).alias?.alias }

        assertEquals(
            listOf(
                "join_identity_row__k1",
                "join_identity_row__k2",
                "join_identity_row__k3"
            ),
            aliases
        )
    }

    @Test
    fun `source identity frame resolves by object identity rather than table name`() {
        val first = JoinIdentityRow()
        val second = JoinIdentityRow()
        val frame = SourceIdentityScope.frame(listOf(first, second))

        assertTrue(frame.contains(first))
        assertTrue(frame.contains(second))
        assertEquals("join_identity_row__k1", frame.aliasForSource(first))
        assertEquals("join_identity_row__k2", frame.aliasForSource(second))
    }

    @Test
    fun `same-table correlated subquery keeps inner and both outer aliases distinct`() {
        val root = UserRelation()
        val outer = UserRelation()
        val inner = UserRelation()

        val task = root.join(outer) { first, second ->
            innerJoin { first.id == second.id2 }
                .select()
                .where {
                    exists(
                        inner.select().where { nested ->
                            nested.username == first.username && nested.gender > second.gender
                        }
                    )
                }
        }.build(SampleSqliteJdbcWrapper).atomicTask

        val expected = """
            SELECT "user_relation__k1"."id", "user_relation__k1"."username",
                "user_relation__k1"."gender", "user_relation__k1"."id2"
            FROM "user_relation" AS "user_relation__k1"
            INNER JOIN "user_relation" AS "user_relation__k2"
                ON "user_relation__k1"."id" = "user_relation__k2"."id2"
            WHERE EXISTS (SELECT "id", "username", "gender", "id2" FROM "user_relation"
                WHERE "user_relation"."username" = "user_relation__k1"."username"
                    AND "user_relation"."gender" > "user_relation__k2"."gender")
        """.trimIndent()

        assertEquals(expected.normalizeSql(), task.sql.normalizeSql())
        assertFalse(task.sql.normalizeSql().contains("\"user_relation\".\"gender\" > \"user_relation\".\"gender\""))
    }

    @Test
    fun `correlated subquery through joined source reserves the outer joined alias`() {
        val customer = SourceIdentityCustomer()
        val outerInvoice = SourceIdentityInvoice()
        val innerInvoice = SourceIdentityInvoice()

        val task = customer.join(outerInvoice) { outerCustomer, outer ->
            innerJoin { outerCustomer.id == outer.customerId }
                .select { outerCustomer.id }
                .where {
                    exists(
                        innerInvoice.select().where { inner ->
                            inner.customerId == outerCustomer.id && inner.amount > outer.amount
                        }
                    )
                }
        }.build(SampleSqliteJdbcWrapper).atomicTask

        val expected = """
            SELECT "source_identity_customer"."id" AS "id"
            FROM "source_identity_customer"
            INNER JOIN "source_identity_invoice" AS "source_identity_invoice__k2"
                ON "source_identity_customer"."id" = "source_identity_invoice__k2"."customer_id"
            WHERE EXISTS (SELECT "source_identity_invoice__k1"."id",
                "source_identity_invoice__k1"."customer_id" AS "customerId",
                "source_identity_invoice__k1"."amount", "source_identity_invoice__k1"."status"
                FROM "source_identity_invoice" AS "source_identity_invoice__k1"
                WHERE "source_identity_invoice__k1"."customer_id" = "source_identity_customer"."id"
                    AND "source_identity_invoice__k1"."amount" > "source_identity_invoice__k2"."amount")
        """.trimIndent()

        assertEquals(expected.normalizeSql(), task.sql.normalizeSql())
        assertFalse(
            task.sql.normalizeSql()
                .contains("\"source_identity_invoice\".\"amount\" > \"source_identity_invoice\".\"amount\"")
        )
    }

    @Test
    fun `late correlated alias rewrites early fields through pagination snapshots`() {
        val customer = SourceIdentityCustomer()
        val outerInvoice = SourceIdentityInvoice()
        val innerInvoice = SourceIdentityInvoice()

        val query = customer.join(outerInvoice) { outerCustomer, outer ->
            innerJoin { outerCustomer.id == outer.customerId }
                .select { outer.amount }
                .where { outer.status == 1 }
                .orderBy { outer.id.desc() }
                .where {
                    exists(
                        innerInvoice.select().where { inner ->
                            inner.customerId == outerCustomer.id && inner.amount > outer.amount
                        }
                    )
                }
        }

        val baseTask = query.build(SampleSqliteJdbcWrapper).atomicTask
        val pageTask = query.page(1, 5).build(SampleSqliteJdbcWrapper).atomicTask
        val cursorTask = query.cursor(2).build(SampleSqliteJdbcWrapper).atomicTask
        val baseSql = """
            SELECT "source_identity_invoice__k2"."amount" AS "amount"
            FROM "source_identity_customer"
            INNER JOIN "source_identity_invoice" AS "source_identity_invoice__k2"
                ON "source_identity_customer"."id" = "source_identity_invoice__k2"."customer_id"
            WHERE "source_identity_invoice__k2"."status" = :status
                AND EXISTS (SELECT "source_identity_invoice__k1"."id",
                    "source_identity_invoice__k1"."customer_id" AS "customerId",
                    "source_identity_invoice__k1"."amount", "source_identity_invoice__k1"."status"
                    FROM "source_identity_invoice" AS "source_identity_invoice__k1"
                    WHERE "source_identity_invoice__k1"."customer_id" = "source_identity_customer"."id"
                        AND "source_identity_invoice__k1"."amount" > "source_identity_invoice__k2"."amount")
            ORDER BY "source_identity_invoice__k2"."id" DESC
        """.trimIndent().normalizeSql()
        val cursorSql = """
            SELECT "source_identity_invoice__k2"."amount" AS "amount",
                "source_identity_invoice__k2"."id" AS "__kronos_cursor_id",
                "source_identity_customer"."id" AS "__kronos_cursor_id_1"
            FROM "source_identity_customer"
            INNER JOIN "source_identity_invoice" AS "source_identity_invoice__k2"
                ON "source_identity_customer"."id" = "source_identity_invoice__k2"."customer_id"
            WHERE "source_identity_invoice__k2"."status" = :status
                AND EXISTS (SELECT "source_identity_invoice__k1"."id",
                    "source_identity_invoice__k1"."customer_id" AS "customerId",
                    "source_identity_invoice__k1"."amount", "source_identity_invoice__k1"."status"
                    FROM "source_identity_invoice" AS "source_identity_invoice__k1"
                    WHERE "source_identity_invoice__k1"."customer_id" = "source_identity_customer"."id"
                        AND "source_identity_invoice__k1"."amount" > "source_identity_invoice__k2"."amount")
            ORDER BY "source_identity_invoice__k2"."id" DESC,
                "source_identity_customer"."id" ASC LIMIT 3
        """.trimIndent().normalizeSql()

        assertEquals(baseSql, baseTask.sql.normalizeSql())
        assertEquals("$baseSql LIMIT 5 OFFSET 0", pageTask.sql.normalizeSql())
        assertEquals(cursorSql, cursorTask.sql.normalizeSql())
        listOf(baseTask, pageTask, cursorTask).forEach { task ->
            assertFalse(task.sql.contains("\"source_identity_invoice\".\""))
        }
    }
}

@Table("source_identity_customer")
data class SourceIdentityCustomer(
    @PrimaryKey var id: Int? = null,
    var name: String? = null
) : KPojo

@Table("source_identity_invoice")
data class SourceIdentityInvoice(
    @PrimaryKey var id: Int? = null,
    var customerId: Int? = null,
    var amount: Int? = null,
    var status: Int? = null
) : KPojo

private class MutableSourceIdentityPojo(
    override var __tableName: String
) : KPojo
