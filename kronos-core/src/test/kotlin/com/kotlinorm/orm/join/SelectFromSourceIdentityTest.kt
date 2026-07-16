package com.kotlinorm.orm.join

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KJoinable
import com.kotlinorm.beans.dsl.SourceIdentityScope
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.initializeCoreSqlTestDefaults
import com.kotlinorm.testutils.normalizeSql
import com.kotlinorm.utils.resolveRuntimeMetadata
import com.kotlinorm.wrappers.SampleSqliteJdbcWrapper
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SelectFromSourceIdentityTest {

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
        assertEquals("source_identity_customer__k3", frame.aliasFor(customer, setOf(
            "source_identity_customer__k1",
            "source_identity_customer__k2"
        )))
        assertEquals("source_identity_customer__k3", frame.existingAliasFor(customer))
        assertEquals("source_identity_invoice__k2", frame.aliasForSource(firstInvoice))
        assertEquals("source_identity_invoice__k3", frame.aliasForSource(secondInvoice))
        assertEquals("source_identity_invoice__k4", frame.aliasFor(unknownInvoice))
        assertTrue(frame.hasPhysicalTable("source_identity_invoice"))
        assertTrue(frame.hasDuplicatePhysicalTable("source_identity_invoice"))
        assertFalse(frame.hasDuplicatePhysicalTable("source_identity_customer"))
        assertTrue(frame.hasAliasForPhysicalTable("source_identity_customer"))
        assertEquals(
            listOf(
                "source_identity_customer__k3",
                "source_identity_invoice__k2",
                "source_identity_invoice__k3",
                "source_identity_invoice__k4"
            ).toSet(),
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
    fun `same-table correlated subquery keeps inner and outer source aliases distinct`() {
        initializeCoreSqlTestDefaults()

        val root = UserRelation()
        val outer = UserRelation()
        val inner = UserRelation()
        val selectFrom = SelectFrom2(root, outer).apply {
            initializeProjection(typeOf<UserRelation>(), typeOf<UserRelation?>())
        }

        var rootSource = ""
        var outerSource = ""
        var innerSource = ""
        var correlatedOuterSource = ""
        selectFrom.context.withSourceScope {
            rootSource = SourceIdentityScope.resolveTableName(root, root.__tableName)
            outerSource = SourceIdentityScope.resolveTableName(outer, outer.__tableName)
            SourceIdentityScope.withFrame(SourceIdentityScope.frame(listOf(inner))) {
                innerSource = SourceIdentityScope.resolveTableName(inner, inner.__tableName)
                correlatedOuterSource = SourceIdentityScope.resolveTableName(outer, outer.__tableName)
            }
        }

        selectFrom.context.joinables += KJoinable(
            tableName = outer.__tableName,
            joinType = SqlJoinType.Inner,
            kClass = outer.resolveRuntimeMetadata().kClass,
            kPojo = outer,
            condition = eq(column(rootSource, "id"), column(outerSource, "id2"))
        )
        selectFrom.context.where = SqlExpr.ExistsPredicate(
            SqlQuery.Select(
                select = listOf(SqlSelectItem.Asterisk()),
                from = listOf(SqlTable.Ident(inner.__tableName)),
                where = and(
                    eq(column(innerSource, "username"), column(rootSource, "username")),
                    gt(column(innerSource, "gender"), column(correlatedOuterSource, "gender"))
                )
            )
        )

        val task = selectFrom.build(SampleSqliteJdbcWrapper).atomicTask
        val expected = """
            SELECT "user_relation__k1"."id", "user_relation__k1"."username",
                "user_relation__k1"."gender", "user_relation__k1"."id2"
            FROM "user_relation" AS "user_relation__k1"
            INNER JOIN "user_relation" AS "user_relation__k2"
                ON "user_relation__k1"."id" = "user_relation__k2"."id2"
            WHERE EXISTS (SELECT * FROM "user_relation"
                WHERE "user_relation"."username" = "user_relation__k1"."username"
                    AND "user_relation"."gender" > "user_relation__k2"."gender")
        """.trimIndent()

        assertEquals(expected.normalizeSql(), task.sql.normalizeSql())
        assertFalse(task.sql.normalizeSql().contains("\"user_relation\".\"gender\" > \"user_relation\".\"gender\""))
    }

    @Test
    fun `same-table correlated subquery through joined source aliases inner and outer occurrences`() {
        initializeCoreSqlTestDefaults()

        val root = SourceIdentityCustomer()
        val outer = SourceIdentityInvoice()
        val inner = SourceIdentityInvoice()
        val selectFrom = SelectFrom2(root, outer).apply {
            initializeProjection(typeOf<SourceIdentityCustomer>(), typeOf<SourceIdentityCustomer?>())
        }

        var rootSource = ""
        var outerJoinSource = ""
        var innerFieldSource = ""
        var innerConditionSource = ""
        var correlatedOuterSource = ""
        selectFrom.context.withSourceScope {
            rootSource = SourceIdentityScope.resolveTableName(root, root.__tableName)
            outerJoinSource = SourceIdentityScope.resolveTableName(outer, outer.__tableName)
            SourceIdentityScope.withFrame(SourceIdentityScope.frame(listOf(inner))) {
                innerFieldSource = SourceIdentityScope.resolveTableName(inner, inner.__tableName)
                correlatedOuterSource = SourceIdentityScope.resolveTableName(outer, outer.__tableName)
                innerConditionSource = SourceIdentityScope.resolveTableName(inner, inner.__tableName)
            }
        }

        val rootId = root.resolveRuntimeMetadata().allFields.first { it.name == "id" }
        selectFrom.context.registerSelectedFields(listOf(rootId))
        selectFrom.context.joinables += KJoinable(
            tableName = outer.__tableName,
            joinType = SqlJoinType.Inner,
            kClass = outer.resolveRuntimeMetadata().kClass,
            kPojo = outer,
            condition = eq(column(rootSource, "id"), column(outerJoinSource, "customer_id"))
        )
        selectFrom.context.where = SqlExpr.ExistsPredicate(
            SqlQuery.Select(
                select = listOf(SqlSelectItem.Asterisk()),
                from = listOf(table(inner.__tableName, innerConditionSource)),
                where = and(
                    eq(column(innerConditionSource, "customer_id"), column(rootSource, "id")),
                    gt(column(innerConditionSource, "amount"), column(correlatedOuterSource, "amount"))
                )
            )
        )

        val task = selectFrom.build(SampleSqliteJdbcWrapper).atomicTask
        val expected = """
            SELECT "source_identity_customer"."id" AS "id"
            FROM "source_identity_customer"
            INNER JOIN "source_identity_invoice" AS "source_identity_invoice__k2"
                ON "source_identity_customer"."id" = "source_identity_invoice__k2"."customer_id"
            WHERE EXISTS (SELECT * FROM "source_identity_invoice" AS "source_identity_invoice__k1"
                WHERE "source_identity_invoice__k1"."customer_id" = "source_identity_customer"."id"
                    AND "source_identity_invoice__k1"."amount" > "source_identity_invoice__k2"."amount")
        """.trimIndent()

        assertEquals("source_identity_invoice", innerFieldSource)
        assertEquals(expected.normalizeSql(), task.sql.normalizeSql())
        assertFalse(
            task.sql.normalizeSql()
                .contains("\"source_identity_invoice\".\"amount\" > \"source_identity_invoice\".\"amount\"")
        )
    }

    private fun column(tableName: String, columnName: String): SqlExpr.Column =
        SqlExpr.Column(tableName = tableName, columnName = columnName)

    private fun table(tableName: String, alias: String): SqlTable.Ident =
        SqlTable.Ident(
            name = tableName,
            alias = alias.takeUnless { it == tableName }?.let { SqlTableAlias(it) }
        )

    private fun eq(left: SqlExpr, right: SqlExpr): SqlExpr =
        SqlExpr.Binary(left, SqlBinaryOperator.Equal, right)

    private fun gt(left: SqlExpr, right: SqlExpr): SqlExpr =
        SqlExpr.Binary(left, SqlBinaryOperator.GreaterThan, right)

    private fun and(left: SqlExpr, right: SqlExpr): SqlExpr =
        SqlExpr.Binary(left, SqlBinaryOperator.And, right)
}

@Table(name = "source_identity_customer")
data class SourceIdentityCustomer(
    @PrimaryKey var id: Int? = null,
    var name: String? = null
) : KPojo

@Table(name = "source_identity_invoice")
data class SourceIdentityInvoice(
    @PrimaryKey var id: Int? = null,
    var customerId: Int? = null,
    var amount: Int? = null,
    var status: Int? = null
) : KPojo

private class MutableSourceIdentityPojo(
    override var __tableName: String
) : KPojo
