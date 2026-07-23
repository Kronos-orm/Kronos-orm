package com.kotlinorm.beans.dsl

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.NoValueStrategyType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator
import com.kotlinorm.syntax.expr.SqlSubqueryQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.codec.PreparedValue
import com.kotlinorm.utils.codec.PreparedValueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.reflect.typeOf

class KTableForConditionBehaviorTest {
    private val id = Field("id", "id", tableName = "tb_user")
    private val username = Field("user_name", "username", tableName = "u")

    @Test
    fun `comparison expressions bind unique named parameters`() {
        val table = KTableForCondition<KPojo>()

        assertEquals(
            SqlExpr.Binary(
                SqlExpr.Column(tableName = "tb_user", columnName = "id"),
                SqlBinaryOperator.Equal,
                SqlExpr.Parameter(SqlParameter.Named("id"))
            ),
            table.equalConditionExpr(id, null, not = false, value = 7)
        )
        assertEquals(
            SqlExpr.Binary(
                SqlExpr.Column(tableName = "tb_user", columnName = "id"),
                SqlBinaryOperator.NotEqual,
                SqlExpr.Parameter(SqlParameter.Named("id@1"))
            ),
            table.equalConditionExpr(id, null, not = true, value = 8)
        )
        assertEquals(
            SqlExpr.Binary(
                SqlExpr.Column(tableName = "u", columnName = "user_name"),
                SqlBinaryOperator.GreaterThan,
                SqlExpr.Parameter(SqlParameter.Named("usernameMin"))
            ),
            table.greaterThanConditionExpr(username, null, not = false, value = "m")
        )
        assertEquals(
            linkedMapOf<String, Any?>("id" to 7, "id@1" to 8, "usernameMin" to "m"),
            table.parameterValues
        )
    }

    @Test
    fun `condition expression helpers keep exact syntax shapes`() {
        val table = KTableForCondition<KPojo>()
        val idColumn = SqlExpr.Column(tableName = "tb_user", columnName = "id")

        assertEquals(idColumn, table.column(id))
        assertEquals(
            SqlExpr.Binary(idColumn, SqlBinaryOperator.Is(withNot = false), SqlExpr.NullLiteral),
            table.isNullConditionExpr(id, null, not = false, value = null)
        )
        assertEquals(
            SqlExpr.Like(
                SqlExpr.Column(tableName = "u", columnName = "user_name"),
                SqlExpr.Parameter(SqlParameter.Named("username")),
                withNot = false
            ),
            table.likeConditionExpr(username, null, not = false, value = "ann")
        )
        assertEquals(
            linkedMapOf<String, Any?>("username" to readyDatabaseValue("ann")),
            table.parameterValues
        )
        val wildcardLikeTable = KTableForCondition<KPojo>()
        assertEquals(
            SqlExpr.Like(
                SqlExpr.Column(tableName = "u", columnName = "user_name"),
                SqlExpr.Parameter(SqlParameter.Named("username")),
                withNot = true
            ),
            wildcardLikeTable.likeConditionExpr(username, null, not = true, value = "%_\\")
        )
        assertEquals(
            linkedMapOf<String, Any?>("username" to readyDatabaseValue("%_\\")),
            wildcardLikeTable.parameterValues
        )
        assertEquals(
            SqlExpr.Binary(idColumn, SqlBinaryOperator.Regexp, SqlExpr.Parameter(SqlParameter.Named("idPattern"))),
            table.regexpConditionExpr(id, null, not = false, value = 123)
        )
        assertEquals(
            SqlExpr.In(
                idColumn,
                SqlInRightOperand.Values(listOf(SqlExpr.Parameter(SqlParameter.Named("idList"), expandAsList = true)))
            ),
            table.inConditionExpr(id, null, not = false, value = listOf(1, 2, 3))
        )
        assertEquals(
            SqlExpr.Between(idColumn, SqlExpr.NumberLiteral("1"), SqlExpr.NumberLiteral("5")),
            table.betweenConditionExpr(id, null, not = false, value = 1..5)
        )
    }

    @Test
    fun `in collection binding stores original rhs and marks occurrence expandable`() {
        val idColumn = SqlExpr.Column(tableName = "tb_user", columnName = "id")
        val list = listOf(1, 2, 3)
        val listTable = KTableForCondition<KPojo>()

        assertEquals(
            SqlExpr.In(
                idColumn,
                SqlInRightOperand.Values(listOf(SqlExpr.Parameter(SqlParameter.Named("idList"), expandAsList = true)))
            ),
            listTable.inConditionExpr(id, null, not = false, value = list)
        )
        assertEquals(list, listTable.parameterValues["idList"])

        val array = arrayOf(1, 2)
        val arrayTable = KTableForCondition<KPojo>()
        arrayTable.inConditionExpr(id, null, not = false, value = array)
        assertEquals(true, arrayTable.parameterValues["idList"] === array)

        val intArray = intArrayOf(1, 2)
        val intArrayTable = KTableForCondition<KPojo>()
        intArrayTable.inConditionExpr(id, null, not = false, value = intArray)
        assertEquals(true, intArrayTable.parameterValues["idList"] === intArray)
    }

    @Test
    fun `literal string helper expressions escape like wildcards`() {
        val usernameColumn = SqlExpr.Column(tableName = "u", columnName = "user_name")
        val escape = SqlExpr.StringLiteral("\\")

        val containsTable = KTableForCondition<KPojo>()
        assertEquals(
            SqlExpr.Like(
                usernameColumn,
                SqlExpr.Parameter(SqlParameter.Named("username")),
                escape = escape
            ),
            containsTable.containsConditionExpr(username, null, not = false, value = "%_\\")
        )
        assertEquals(
            mapOf<String, Any?>("username" to readyDatabaseValue("%\\%\\_\\\\%")),
            containsTable.parameterValues
        )

        val startsWithTable = KTableForCondition<KPojo>()
        assertEquals(
            SqlExpr.Like(
                usernameColumn,
                SqlExpr.Parameter(SqlParameter.Named("username")),
                escape = escape
            ),
            startsWithTable.startsWithConditionExpr(username, null, not = false, value = "literal%_\\")
        )
        assertEquals(
            mapOf<String, Any?>("username" to readyDatabaseValue("literal\\%\\_\\\\%")),
            startsWithTable.parameterValues
        )

        val endsWithTable = KTableForCondition<KPojo>()
        assertEquals(
            SqlExpr.Like(
                usernameColumn,
                SqlExpr.Parameter(SqlParameter.Named("username")),
                escape = escape,
                withNot = true
            ),
            endsWithTable.endsWithConditionExpr(username, null, not = true, value = "%_mid\\")
        )
        assertEquals(
            mapOf<String, Any?>("username" to readyDatabaseValue("%\\%\\_mid\\\\")),
            endsWithTable.parameterValues
        )
    }

    @Test
    fun `comparison helpers cover negated operators and value expression kinds`() {
        val idColumn = SqlExpr.Column(tableName = "tb_user", columnName = "id")
        val owner = Field("owner_id", "ownerId", tableName = "tb_order")
        val lowered = KronosFunctionExpr(
            SqlExpr.Function(SqlIdentifier.of("LOWER"), args = listOf(SqlExpr.Column("u", "user_name"))),
            "lower"
        )

        assertEquals(
            SqlExpr.Binary(idColumn, SqlBinaryOperator.GreaterThanEqual, SqlExpr.Parameter(SqlParameter.Named("idMin"))),
            KTableForCondition<KPojo>().greaterThanOrEqualConditionExpr(id, null, not = false, value = 9)
        )
        assertEquals(
            SqlExpr.Binary(idColumn, SqlBinaryOperator.LessThan, SqlExpr.Parameter(SqlParameter.Named("idMin"))),
            KTableForCondition<KPojo>().greaterThanOrEqualConditionExpr(id, null, not = true, value = 9)
        )
        assertEquals(
            SqlExpr.Binary(idColumn, SqlBinaryOperator.LessThan, SqlExpr.Parameter(SqlParameter.Named("idMax"))),
            KTableForCondition<KPojo>().lessThanConditionExpr(id, null, not = false, value = 9)
        )
        assertEquals(
            SqlExpr.Binary(idColumn, SqlBinaryOperator.GreaterThan, SqlExpr.Parameter(SqlParameter.Named("idMax"))),
            KTableForCondition<KPojo>().lessThanOrEqualConditionExpr(id, null, not = true, value = 9)
        )
        assertEquals(
            SqlExpr.Binary(
                SqlExpr.Column(tableName = "root", columnName = "score"),
                SqlBinaryOperator.Equal,
                SqlExpr.Column(tableName = "tb_order", columnName = "owner_id")
            ),
            KTableForCondition<KPojo>().equalConditionExpr(
                field = null,
                left = SqlExpr.Column(tableName = "root", columnName = "score"),
                not = false,
                value = owner
            )
        )
        assertEquals(
            SqlExpr.Like(
                SqlExpr.Column(tableName = "u", columnName = "user_name"),
                lowered.expr,
                withNot = true
            ),
            KTableForCondition<KPojo>().likeConditionExpr(username, null, not = true, value = lowered)
        )
        assertEquals(
            SqlExpr.Binary(
                SqlExpr.Column(tableName = "u", columnName = "user_name"),
                SqlBinaryOperator.NotRegexp,
                SqlExpr.StringLiteral("^a")
            ),
            KTableForCondition<KPojo>().regexpConditionExpr(username, null, not = true, value = SqlExpr.StringLiteral("^a"))
        )
    }

    @Test
    fun `in between quantified and explicit no value branches keep exact syntax`() {
        val query = ConditionSelectable()
        val idColumn = SqlExpr.Column(tableName = "tb_user", columnName = "id")

        assertEquals(
            SqlExpr.In(
                idColumn,
                SqlInRightOperand.Values(listOf(SqlExpr.Parameter(SqlParameter.Named("idList"), expandAsList = true))),
                withNot = true
            ),
            KTableForCondition<KPojo>().inConditionExpr(id, null, not = true, value = arrayOf(1, 2))
        )
        assertEquals(
            SqlExpr.In(
                idColumn,
                SqlInRightOperand.Values(listOf(SqlExpr.Column(tableName = "tb_user", columnName = "id")))
            ),
            KTableForCondition<KPojo>().inConditionExpr(id, null, not = false, value = id)
        )
        assertEquals(
            SqlExpr.Between(idColumn, SqlExpr.StringLiteral("a"), SqlExpr.StringLiteral("z"), withNot = true),
            KTableForCondition<KPojo>().betweenConditionExpr(id, null, not = true, value = "a".."z")
        )
        assertNull(KTableForCondition<KPojo>().betweenConditionExpr(id, null, not = false, value = "not-range"))
        assertEquals(
            SqlExpr.QuantifiedComparisonPredicate(
                expr = idColumn,
                operator = SqlQuantifiedComparisonOperator.GreaterThan,
                quantifier = SqlSubqueryQuantifier.Any,
                query = query.query
            ),
            KTableForCondition<KPojo>().greaterThanConditionExpr(id, null, not = false, value = QuantifiedSubqueryValue(query, SqlSubqueryQuantifier.Any))
        )
        assertEquals(
            SqlExpr.ExistsPredicate(query.query, withNot = true),
            KTableForCondition<KPojo>().existsExpr(query, not = true)
        )
        assertEquals(
            listOf<SqlExpr?>(
                null,
                SqlExpr.BooleanLiteral(true),
                SqlExpr.Binary(idColumn, SqlBinaryOperator.Is(withNot = false), SqlExpr.NullLiteral),
                SqlExpr.BooleanLiteral(false)
            ),
            listOf(
                KTableForCondition<KPojo>().inConditionExpr(id, null, not = false, value = null, noValueStrategyType = NoValueStrategyType.Ignore),
                KTableForCondition<KPojo>().inConditionExpr(id, null, not = false, value = null, noValueStrategyType = NoValueStrategyType.True),
                KTableForCondition<KPojo>().inConditionExpr(id, null, not = false, value = null, noValueStrategyType = NoValueStrategyType.JudgeNull),
                KTableForCondition<KPojo>().apply { operationType = KOperationType.DELETE }
                    .inConditionExpr(id, null, not = false, value = null, noValueStrategyType = NoValueStrategyType.Auto)
            )
        )
    }

    @Test
    fun `no value strategies return explicit syntax outcomes`() {
        val table = KTableForCondition<KPojo>()
        val idColumn = SqlExpr.Column(tableName = "tb_user", columnName = "id")

        assertNull(table.equalConditionExpr(id, null, not = false, value = null))
        table.operationType = KOperationType.UPDATE
        assertEquals(
            SqlExpr.Binary(idColumn, SqlBinaryOperator.Is(withNot = false), SqlExpr.NullLiteral),
            table.equalConditionExpr(id, null, not = false, value = null)
        )
        assertEquals(
            SqlExpr.BooleanLiteral(false),
            table.greaterThanConditionExpr(id, null, not = false, value = null)
        )
        assertEquals(
            SqlExpr.BooleanLiteral(true),
            table.inConditionExpr(id, null, not = true, value = emptyList<Int>())
        )
        assertEquals(
            SqlExpr.Binary(idColumn, SqlBinaryOperator.Is(withNot = true), SqlExpr.NullLiteral),
            table.likeConditionExpr(
                id,
                null,
                not = true,
                value = null,
                noValueStrategyType = NoValueStrategyType.JudgeNull
            )
        )
    }

    @Test
    fun `empty array and collection in values collapse to exact boolean outcomes`() {
        val table = KTableForCondition<KPojo>()

        assertEquals(
            listOf(
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(false)
            ),
            listOf(
                table.inConditionExpr(id, null, not = false, value = emptyList<Int>()),
                table.inConditionExpr(id, null, not = false, value = emptyArray<Int>()),
                table.inConditionExpr(id, null, not = false, value = booleanArrayOf()),
                table.inConditionExpr(id, null, not = false, value = byteArrayOf()),
                table.inConditionExpr(id, null, not = false, value = shortArrayOf()),
                table.inConditionExpr(id, null, not = false, value = intArrayOf()),
                table.inConditionExpr(id, null, not = false, value = longArrayOf()),
                table.inConditionExpr(id, null, not = false, value = floatArrayOf()),
                table.inConditionExpr(id, null, not = false, value = doubleArrayOf()),
                table.inConditionExpr(id, null, not = false, value = charArrayOf())
            )
        )
    }

    @Test
    fun `non-empty primitive arrays bind as expanded in parameters`() {
        val values = listOf<Any>(
            booleanArrayOf(true),
            byteArrayOf(1),
            shortArrayOf(2),
            intArrayOf(3),
            longArrayOf(4L),
            floatArrayOf(5f),
            doubleArrayOf(6.0),
            charArrayOf('7')
        )
        val expectedExpr = SqlExpr.In(
            SqlExpr.Column(tableName = "tb_user", columnName = "id"),
            SqlInRightOperand.Values(
                listOf(SqlExpr.Parameter(SqlParameter.Named("idList"), expandAsList = true))
            )
        )

        values.forEach { value ->
            val table = KTableForCondition<KPojo>()

            assertEquals(expectedExpr, table.inConditionExpr(id, null, not = false, value = value))
            assertEquals(mapOf<String, Any?>("idList" to value), table.parameterValues)
        }
    }

    @Test
    fun `literal like no value strategies return exact outcomes`() {
        val usernameColumn = SqlExpr.Column(tableName = "u", columnName = "user_name")

        assertEquals(
            listOf<SqlExpr?>(
                null,
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(true),
                SqlExpr.Binary(usernameColumn, SqlBinaryOperator.Is(withNot = true), SqlExpr.NullLiteral),
                null,
                SqlExpr.BooleanLiteral(false),
                SqlExpr.BooleanLiteral(true)
            ),
            listOf(
                KTableForCondition<KPojo>().startsWithConditionExpr(
                    username, null, not = false, value = null, noValueStrategyType = NoValueStrategyType.Ignore
                ),
                KTableForCondition<KPojo>().endsWithConditionExpr(
                    username, null, not = false, value = null, noValueStrategyType = NoValueStrategyType.False
                ),
                KTableForCondition<KPojo>().containsConditionExpr(
                    username, null, not = false, value = null, noValueStrategyType = NoValueStrategyType.True
                ),
                KTableForCondition<KPojo>().startsWithConditionExpr(
                    username, null, not = true, value = null, noValueStrategyType = NoValueStrategyType.JudgeNull
                ),
                KTableForCondition<KPojo>().containsConditionExpr(
                    username, null, not = false, value = null, noValueStrategyType = NoValueStrategyType.Auto
                ),
                KTableForCondition<KPojo>().apply { operationType = KOperationType.UPDATE }
                    .startsWithConditionExpr(username, null, not = false, value = null),
                KTableForCondition<KPojo>().apply { operationType = KOperationType.DELETE }
                    .endsWithConditionExpr(username, null, not = true, value = null)
            )
        )
    }

    @Test
    fun `literal like accepts expression field function and subquery right sides`() {
        val usernameColumn = SqlExpr.Column(tableName = "u", columnName = "user_name")
        val escape = SqlExpr.StringLiteral("\\")
        val rawExpr = SqlExpr.StringLiteral("literal")
        val owner = Field("owner_name", "ownerName", tableName = "tb_order")
        val ownerColumn = SqlExpr.Column(tableName = "tb_order", columnName = "owner_name")
        val lowered = KronosFunctionExpr(
            SqlExpr.Function(SqlIdentifier.of("LOWER"), args = listOf(ownerColumn)),
            "lower"
        )
        val query = ConditionSelectable()

        assertEquals(
            listOf(
                SqlExpr.Like(usernameColumn, rawExpr, escape = escape),
                SqlExpr.Like(usernameColumn, lowered.expr, escape = escape),
                SqlExpr.Like(usernameColumn, ownerColumn, escape = escape),
                SqlExpr.Like(usernameColumn, SqlExpr.Subquery(query.query), escape = escape),
                SqlExpr.Like(usernameColumn, SqlExpr.Subquery(query.query), escape = escape)
            ),
            listOf(
                KTableForCondition<KPojo>().startsWithConditionExpr(username, null, not = false, value = rawExpr),
                KTableForCondition<KPojo>().endsWithConditionExpr(username, null, not = false, value = lowered),
                KTableForCondition<KPojo>().containsConditionExpr(username, null, not = false, value = owner),
                KTableForCondition<KPojo>().startsWithConditionExpr(username, null, not = false, value = query),
                KTableForCondition<KPojo>().endsWithConditionExpr(
                    username,
                    null,
                    not = false,
                    value = QuantifiedSubqueryValue(query, SqlSubqueryQuantifier.All)
                )
            )
        )
    }

    @Test
    fun `raw and logical helpers compose exact expressions`() {
        val table = KTableForCondition<KPojo>()
        val first = SqlExpr.BooleanLiteral(true)
        val second = SqlExpr.UnsafeRaw("score > 0")
        val third = SqlExpr.StringLiteral("fallback")

        assertEquals(second, table.rawConditionExpr(null, null, not = false, value = "score > 0"))
        assertEquals(SqlExpr.BooleanLiteral(false), table.rawConditionExpr(null, null, not = true, value = true))
        assertEquals(third, table.rawConditionExpr(null, null, not = false, value = "fallback".toStringBuilder()))
        assertEquals(
            SqlExpr.Binary(SqlExpr.Binary(first, SqlBinaryOperator.And, second), SqlBinaryOperator.And, third),
            table.andExpr(listOf(first, null, second, third))
        )
        assertEquals(
            SqlExpr.Binary(first, SqlBinaryOperator.Or, second),
            table.orExpr(listOf(first, second))
        )
        table.addCondition(first)
        table.addCondition(second)
        assertEquals(SqlExpr.Binary(first, SqlBinaryOperator.And, second), table.sqlExpr)
    }

    @Test
    fun `iterable predicate helpers combine generated expressions`() {
        val first = SqlExpr.BooleanLiteral(true)
        val second = SqlExpr.UnsafeRaw("score > 0")
        val values = listOf<SqlExpr?>(first, null, second)

        assertEquals(
            SqlExpr.Binary(first, SqlBinaryOperator.Or, second),
            KTableForCondition<KPojo>().iterableAnyConditionExpr(values, { it }, negated = false)
        )
        assertEquals(
            SqlExpr.Binary(first, SqlBinaryOperator.And, second),
            KTableForCondition<KPojo>().iterableAnyConditionExpr(values, { it }, negated = true)
        )
        assertEquals(
            SqlExpr.Binary(first, SqlBinaryOperator.And, second),
            KTableForCondition<KPojo>().iterableAllConditionExpr(values, { it }, negated = false)
        )
        assertEquals(
            SqlExpr.Binary(first, SqlBinaryOperator.Or, second),
            KTableForCondition<KPojo>().iterableAllConditionExpr(values, { it }, negated = true)
        )
        assertEquals(
            SqlExpr.Binary(first, SqlBinaryOperator.And, second),
            KTableForCondition<KPojo>().iterableNoneConditionExpr(values, { it }, negated = false)
        )
        assertEquals(
            SqlExpr.Binary(first, SqlBinaryOperator.Or, second),
            KTableForCondition<KPojo>().iterableNoneConditionExpr(values, { it }, negated = true)
        )
        assertEquals(
            SqlExpr.BooleanLiteral(false),
            KTableForCondition<KPojo>().iterableAnyConditionExpr(emptyList<SqlExpr?>(), { it }, negated = false)
        )
    }

    @Test
    fun `runtime-only compiler stubs return stable sentinel values`() {
        val table = KTableForCondition<KPojo>()
        table.sourceValues["id"] = 42
        val nullable: Any? = 1
        val comparable: Comparable<Int>? = 1

        with(table) {
            assertNull(nullable + 2)
            assertNull(nullable - 2)
            assertNull(nullable * 2)
            assertNull(nullable / 2)
            assertNull(nullable % 2)
            assertEquals(42, sourceValueByFieldName("id"))
            assertEquals(5, 5.value)
            assertNull((null as String?).value)
            assertEquals(true, (listOf(1) as Iterable<Int?>?).contains(9))
            assertEquals(true, (arrayOf(1) as Array<Int?>?).contains(9))
            assertEquals(true, (intArrayOf(1) as IntArray?).contains(9))
            assertEquals(true, (longArrayOf(1) as LongArray?).contains(9))
            assertEquals(true, (floatArrayOf(1f) as FloatArray?).contains(9))
            assertEquals(true, (doubleArrayOf(1.0) as DoubleArray?).contains(9))
            assertEquals(true, (charArrayOf('a') as CharArray?).contains('z'))
            assertEquals(true, (booleanArrayOf(true) as BooleanArray?).contains(false))
            assertEquals(true, ("abc" as CharSequence?).contains('z'))
            assertEquals(true, ("abc" as CharSequence?).contains("z"))
            assertEquals(true, (null as CharSequence?).contains)
            assertEquals(1, comparable.compareTo(2))
            assertEquals(1, (1 as Any?).compareTo("x"))
            assertEquals(true, (true as Boolean?).takeIf(false))
            assertEquals(true, (true as Boolean?).takeUnless(true))
            assertEquals(true, (1 as Comparable<*>?) like "x")
            assertEquals(true, (1 as Comparable<*>?) notLike "x")
            assertEquals(true, (1 as Comparable<*>?) between 1..2)
            assertEquals(true, (1 as Comparable<*>?) notBetween 1..2)
            assertEquals(true, (1 as Comparable<*>?) startsWith "1")
            assertEquals(true, (1 as Comparable<*>?) endsWith "1")
            assertEquals(true, (1 as Comparable<*>?) regexp "1")
            assertEquals(true, (1 as Comparable<*>?) notRegexp "1")
            assertEquals(true, "raw".asSql())
            assertEquals(true, (true as Boolean?).asSql())
            assertEquals(true, 1.eq)
            assertEquals(true, RuntimeConditionPojo().eq)
            assertEquals(true, 1.neq)
            assertEquals(true, 1.like)
            assertEquals(true, 1.notLike)
            assertEquals(true, 1.startsWith)
            assertEquals(true, 1.endsWith)
            assertEquals(true, (null as Any?).isNull)
            assertEquals(true, (null as Any?).notNull)
            assertEquals(true, 1.lt)
            assertEquals(true, 1.gt)
            assertEquals(true, 1.le)
            assertEquals(true, 1.ge)
            assertEquals(true, 1.regexp)
            assertEquals(true, 1.notRegexp)
            assertEquals(RuntimeConditionPojo(), RuntimeConditionPojo() - "id")
            assertEquals(1, 1.cast())
            assertEquals(true, ConditionSelectable().contains(1))
            assertEquals(true, exists(ConditionSelectable()))
            assertEquals(SqlSubqueryQuantifier.Any, (any<QuantifiedSubqueryValue>(ConditionSelectable()))?.quantifier)
            assertEquals(SqlSubqueryQuantifier.Some, (some<QuantifiedSubqueryValue>(ConditionSelectable()))?.quantifier)
            assertEquals(SqlSubqueryQuantifier.All, (all<QuantifiedSubqueryValue>(ConditionSelectable()))?.quantifier)
            assertEquals("%abc%", buildContainsStr("abc"))
            assertNull(buildContainsStr(null))
        }
    }

    private fun readyDatabaseValue(value: String): PreparedValue = PreparedValue(
        value = value,
        sourceType = typeOf<String>(),
        kind = PreparedValueKind.READY_DATABASE_VALUE
    )
}

private fun String.toStringBuilder(): StringBuilder = StringBuilder(this)

@Table("runtime_condition_pojo")
data class RuntimeConditionPojo(val id: Int? = null) : KPojo

private class ConditionSelectable : KSelectable<RuntimeConditionPojo>(RuntimeConditionPojo()) {
    override val selectedType = typeOf<RuntimeConditionPojo>()

    val query = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.NumberLiteral("1"))),
        from = listOf(SqlTable.Ident("runtime_condition_pojo"))
    )

    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask =
        error("ConditionSelectable is only used for syntax materialization tests.")

    internal override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan =
        SqlQueryPlan(query, emptyMap())
}
