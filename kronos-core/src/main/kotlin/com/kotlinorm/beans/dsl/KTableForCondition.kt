/**
 * Copyright 2022-2025 kronos-orm
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
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "UNUSED", "UnusedReceiverParameter")

package com.kotlinorm.beans.dsl

import com.kotlinorm.annotations.UnsafeCondition
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.NoValueStrategyType
import com.kotlinorm.functions.FunctionHandler
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.sql.materializeSqlQuery
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator
import com.kotlinorm.syntax.expr.SqlSubqueryQuantifier
import com.kotlinorm.utils.codec.PreparedValue
import com.kotlinorm.utils.codec.PreparedValueKind
import com.kotlinorm.utils.DEFAULT_LIKE_ESCAPE
import com.kotlinorm.utils.escapeLikeLiteral
import kotlin.jvm.JvmName
import kotlin.reflect.typeOf

/**
 * kTableForCondition
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `where` code.
 *
 * @param T the type of the table
 */
open class KTableForCondition<T : KPojo>(
    val sourceBinding: SourceBinding? = null
) {
    var sourceValues: MutableMap<String, Any?> = mutableMapOf()
    var operationType: KOperationType = KOperationType.SELECT
    var sqlExpr: SqlExpr? = null
    val parameterValues: MutableMap<String, Any?> = mutableMapOf()
    val parameterFields: MutableMap<String, Field> = mutableMapOf()
    private val parameterNameCounter: MutableMap<String, Int> = mutableMapOf()
    val f: FunctionHandler = FunctionHandler

    fun addCondition(expr: SqlExpr?) {
        if (expr == null) return
        sqlExpr = sqlExpr?.let { SqlExpr.Binary(it, SqlBinaryOperator.And, expr) } ?: expr
    }

    fun column(field: Field, tableName: String? = field.tableName): SqlExpr =
        field.toSourceColumn(sourceBinding, tableName)

    fun bindParameter(
        field: Field,
        value: Any?,
        baseName: String = field.parameterBaseName(),
        expandAsList: Boolean = false
    ): SqlExpr {
        val name = allocateParameterName(baseName)
        parameterValues[name] = value
        parameterFields[name] = field
        return SqlExpr.Parameter(SqlParameter.Named(name), expandAsList = expandAsList)
    }

    fun andExpr(children: List<SqlExpr?>): SqlExpr? =
        logicalExpr(SqlBinaryOperator.And, children)

    fun orExpr(children: List<SqlExpr?>): SqlExpr? =
        logicalExpr(SqlBinaryOperator.Or, children)

    @PublishedApi
    internal fun <Element> iterableAnyConditionExpr(
        values: Iterable<Element>,
        predicate: (Element) -> SqlExpr?,
        negated: Boolean
    ): SqlExpr =
        iterableConditionExpr(values, predicate, if (negated) SqlBinaryOperator.And else SqlBinaryOperator.Or)

    @PublishedApi
    internal fun <Element> iterableAllConditionExpr(
        values: Iterable<Element>,
        predicate: (Element) -> SqlExpr?,
        negated: Boolean
    ): SqlExpr =
        iterableConditionExpr(values, predicate, if (negated) SqlBinaryOperator.Or else SqlBinaryOperator.And)

    @PublishedApi
    internal fun <Element> iterableNoneConditionExpr(
        values: Iterable<Element>,
        predicate: (Element) -> SqlExpr?,
        negated: Boolean
    ): SqlExpr =
        iterableConditionExpr(values, predicate, if (negated) SqlBinaryOperator.Or else SqlBinaryOperator.And)

    private fun <Element> iterableConditionExpr(
        values: Iterable<Element>,
        predicate: (Element) -> SqlExpr?,
        operator: SqlBinaryOperator
    ): SqlExpr {
        val children = values.map(predicate).filterNotNull()
        if (children.isEmpty()) return SqlExpr.BooleanLiteral(false)
        return children.drop(1).fold(children.first()) { left, right -> SqlExpr.Binary(left, operator, right) }
    }

    private fun logicalExpr(operator: SqlBinaryOperator, children: List<SqlExpr?>): SqlExpr? {
        val items = children.filterNotNull()
        if (items.isEmpty()) return null
        return items.drop(1).fold(items.first()) { left, right -> SqlExpr.Binary(left, operator, right) }
    }

    fun rawConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? {
        return when (value) {
            null -> null
            is SqlExpr -> value
            is Boolean -> SqlExpr.BooleanLiteral(if (not) !value else value)
            is String -> SqlExpr.UnsafeRaw(value)
            else -> SqlExpr.StringLiteral(value.toString())
        }
    }

    fun isNullConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? {
        val expr = left ?: field?.let { column(it, tableName) } ?: return null
        return SqlExpr.Binary(expr, SqlBinaryOperator.Is(withNot = not), SqlExpr.NullLiteral)
    }

    fun equalConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? =
        comparisonConditionExpr(
            field,
            left,
            not,
            value,
            tableName,
            noValueStrategyType,
            positiveOperator = SqlBinaryOperator.Equal,
            negativeOperator = SqlBinaryOperator.NotEqual,
            quantifiedOperator = SqlQuantifiedComparisonOperator.Equal
        ) { NoValueStrategyType.JudgeNull }

    fun greaterThanConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? =
        comparisonConditionExpr(
            field,
            left,
            not,
            value,
            tableName,
            noValueStrategyType,
            positiveOperator = SqlBinaryOperator.GreaterThan,
            negativeOperator = SqlBinaryOperator.LessThanEqual,
            quantifiedOperator = SqlQuantifiedComparisonOperator.GreaterThan
        ) { NoValueStrategyType.False }

    fun greaterThanOrEqualConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? =
        comparisonConditionExpr(
            field,
            left,
            not,
            value,
            tableName,
            noValueStrategyType,
            positiveOperator = SqlBinaryOperator.GreaterThanEqual,
            negativeOperator = SqlBinaryOperator.LessThan,
            quantifiedOperator = SqlQuantifiedComparisonOperator.GreaterThanEqual
        ) { NoValueStrategyType.False }

    fun lessThanConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? =
        comparisonConditionExpr(
            field,
            left,
            not,
            value,
            tableName,
            noValueStrategyType,
            positiveOperator = SqlBinaryOperator.LessThan,
            negativeOperator = SqlBinaryOperator.GreaterThanEqual,
            quantifiedOperator = SqlQuantifiedComparisonOperator.LessThan
        ) { NoValueStrategyType.False }

    fun lessThanOrEqualConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? =
        comparisonConditionExpr(
            field,
            left,
            not,
            value,
            tableName,
            noValueStrategyType,
            positiveOperator = SqlBinaryOperator.LessThanEqual,
            negativeOperator = SqlBinaryOperator.GreaterThan,
            quantifiedOperator = SqlQuantifiedComparisonOperator.LessThanEqual
        ) { NoValueStrategyType.False }

    fun likeConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? {
        val leftExpr = left ?: field?.let { column(it, tableName) } ?: return null
        val noValueStrategy = noValueStrategy(
            value,
            emptyCollectionIsNoValue = false,
            explicit = noValueStrategyType,
            updateDeleteStrategy = { trueWhenNegatedFalseWhenPositive(not) }
        )
        if (noValueStrategy != null) {
            return when (noValueStrategy) {
                NoValueStrategyType.Ignore -> null
                NoValueStrategyType.False -> SqlExpr.BooleanLiteral(false)
                NoValueStrategyType.True -> SqlExpr.BooleanLiteral(true)
                NoValueStrategyType.JudgeNull -> SqlExpr.Binary(leftExpr, SqlBinaryOperator.Is(withNot = not), SqlExpr.NullLiteral)
                NoValueStrategyType.Auto -> null
            }
        }
        val baseField = field ?: Field("value", "value")
        val rightExpr = value.toLikeConditionValueExpr(baseField)
        return SqlExpr.Like(leftExpr, rightExpr, withNot = not)
    }

    fun startsWithConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? =
        literalLikeConditionExpr(field, left, not, value, tableName, noValueStrategyType, prefixWildcard = false, suffixWildcard = true)

    fun endsWithConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? =
        literalLikeConditionExpr(field, left, not, value, tableName, noValueStrategyType, prefixWildcard = true, suffixWildcard = false)

    fun containsConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? =
        literalLikeConditionExpr(field, left, not, value, tableName, noValueStrategyType, prefixWildcard = true, suffixWildcard = true)

    private fun literalLikeConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String?,
        noValueStrategyType: NoValueStrategyType?,
        prefixWildcard: Boolean,
        suffixWildcard: Boolean
    ): SqlExpr? {
        val leftExpr = left ?: field?.let { column(it, tableName) } ?: return null
        val noValueStrategy = noValueStrategy(
            value,
            emptyCollectionIsNoValue = false,
            explicit = noValueStrategyType,
            updateDeleteStrategy = { trueWhenNegatedFalseWhenPositive(not) }
        )
        if (noValueStrategy != null) {
            return when (noValueStrategy) {
                NoValueStrategyType.Ignore -> null
                NoValueStrategyType.False -> SqlExpr.BooleanLiteral(false)
                NoValueStrategyType.True -> SqlExpr.BooleanLiteral(true)
                NoValueStrategyType.JudgeNull -> SqlExpr.Binary(leftExpr, SqlBinaryOperator.Is(withNot = not), SqlExpr.NullLiteral)
                NoValueStrategyType.Auto -> null
            }
        }
        val baseField = field ?: Field("value", "value")
        val rightExpr = value.toLiteralLikeConditionValueExpr(baseField, prefixWildcard, suffixWildcard)
        return SqlExpr.Like(
            leftExpr,
            rightExpr,
            escape = SqlExpr.StringLiteral(DEFAULT_LIKE_ESCAPE.toString()),
            withNot = not
        )
    }

    fun regexpConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? {
        val leftExpr = left ?: field?.let { column(it, tableName) } ?: return null
        val noValueStrategy = noValueStrategy(
            value,
            emptyCollectionIsNoValue = false,
            explicit = noValueStrategyType,
            updateDeleteStrategy = { trueWhenNegatedFalseWhenPositive(not) }
        )
        if (noValueStrategy != null) return noValueSqlExpr(leftExpr, noValueStrategy, not)
        val baseField = field ?: Field("value", "value")
        val rightExpr = value.toConditionValueExpr(baseField, baseField.parameterBaseName("Pattern"))
        return SqlExpr.Binary(leftExpr, if (not) SqlBinaryOperator.NotRegexp else SqlBinaryOperator.Regexp, rightExpr)
    }

    fun inConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? {
        val leftExpr = left ?: field?.let { column(it, tableName) } ?: return null
        val noValueStrategy = noValueStrategy(
            value,
            emptyCollectionIsNoValue = true,
            explicit = noValueStrategyType,
            updateDeleteStrategy = { trueWhenNegatedFalseWhenPositive(not) },
            queryStrategy = {
                if (value.isEmptyArrayOrCollection()) trueWhenNegatedFalseWhenPositive(not) else NoValueStrategyType.Ignore
            }
        )
        if (noValueStrategy != null) return noValueSqlExpr(leftExpr, noValueStrategy, not)
        val baseField = field ?: Field("value", "value")
        return SqlExpr.In(leftExpr, value.toInRightOperand(baseField), withNot = not)
    }

    fun betweenConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String? = field?.tableName,
        noValueStrategyType: NoValueStrategyType? = null
    ): SqlExpr? {
        val leftExpr = left ?: field?.let { column(it, tableName) } ?: return null
        val noValueStrategy = noValueStrategy(
            value,
            emptyCollectionIsNoValue = false,
            explicit = noValueStrategyType,
            updateDeleteStrategy = { trueWhenNegatedFalseWhenPositive(not) }
        )
        if (noValueStrategy != null) return noValueSqlExpr(leftExpr, noValueStrategy, not)
        return value.toBetweenExpr(leftExpr, not)
    }

    private fun comparisonConditionExpr(
        field: Field?,
        left: SqlExpr?,
        not: Boolean,
        value: Any?,
        tableName: String?,
        noValueStrategyType: NoValueStrategyType?,
        positiveOperator: SqlBinaryOperator,
        negativeOperator: SqlBinaryOperator,
        quantifiedOperator: SqlQuantifiedComparisonOperator,
        updateDeleteNoValueStrategy: () -> NoValueStrategyType
    ): SqlExpr? {
        val leftExpr = left ?: field?.let { column(it, tableName) } ?: return null
        val noValueStrategy = noValueStrategy(
            value,
            emptyCollectionIsNoValue = false,
            explicit = noValueStrategyType,
            updateDeleteStrategy = updateDeleteNoValueStrategy
        )
        if (noValueStrategy != null) return noValueSqlExpr(leftExpr, noValueStrategy, not)
        if (value is QuantifiedSubqueryValue) {
            return quantifiedComparisonExpr(leftExpr, quantifiedOperator, value)
        }
        val baseField = field ?: Field("value", "value")
        val rightExpr = value.toConditionValueExpr(baseField, baseField.comparisonParameterBaseName(positiveOperator))
        return SqlExpr.Binary(leftExpr, if (not) negativeOperator else positiveOperator, rightExpr)
    }

    private fun noValueStrategy(
        value: Any?,
        emptyCollectionIsNoValue: Boolean,
        explicit: NoValueStrategyType?,
        updateDeleteStrategy: () -> NoValueStrategyType,
        queryStrategy: () -> NoValueStrategyType = { NoValueStrategyType.Ignore }
    ): NoValueStrategyType? {
        val noValue = value == null || (emptyCollectionIsNoValue && value.isEmptyArrayOrCollection())
        if (!noValue) return null
        if (explicit != null && explicit != NoValueStrategyType.Auto) return explicit
        return when (operationType) {
            KOperationType.UPDATE, KOperationType.DELETE -> updateDeleteStrategy()
            else -> queryStrategy()
        }
    }

    private fun trueWhenNegatedFalseWhenPositive(not: Boolean): NoValueStrategyType =
        if (not) NoValueStrategyType.True else NoValueStrategyType.False

    private fun noValueSqlExpr(leftExpr: SqlExpr, strategy: NoValueStrategyType, not: Boolean): SqlExpr? =
        when (strategy) {
            NoValueStrategyType.Ignore -> null
            NoValueStrategyType.False -> SqlExpr.BooleanLiteral(false)
            NoValueStrategyType.True -> SqlExpr.BooleanLiteral(true)
            NoValueStrategyType.JudgeNull -> SqlExpr.Binary(leftExpr, SqlBinaryOperator.Is(withNot = not), SqlExpr.NullLiteral)
            NoValueStrategyType.Auto -> null
        }

    private fun Any?.isEmptyArrayOrCollection(): Boolean =
        when (this) {
            is Collection<*> -> isEmpty()
            is Array<*> -> isEmpty()
            is BooleanArray -> isEmpty()
            is ByteArray -> isEmpty()
            is ShortArray -> isEmpty()
            is IntArray -> isEmpty()
            is LongArray -> isEmpty()
            is FloatArray -> isEmpty()
            is DoubleArray -> isEmpty()
            is CharArray -> isEmpty()
            else -> false
        }

    private fun Field.parameterBaseName(suffix: String = ""): String =
        name.ifBlank { columnName } + suffix

    private fun Field.comparisonParameterBaseName(operator: SqlBinaryOperator): String =
        when (operator) {
            SqlBinaryOperator.GreaterThan,
            SqlBinaryOperator.GreaterThanEqual -> parameterBaseName("Min")
            SqlBinaryOperator.LessThan,
            SqlBinaryOperator.LessThanEqual -> parameterBaseName("Max")
            else -> parameterBaseName()
        }

    private fun Any?.toConditionValueExpr(field: Field, parameterBaseName: String = field.parameterBaseName()): SqlExpr =
        when (this) {
            is SqlExpr -> this
            is KronosFunctionExpr -> expr
            is Field -> column(this)
            is KSelectable<*> -> SqlExpr.Subquery(
                materializeSqlQuery(parameterValues, parameterFields = parameterFields)
            )
            is QuantifiedSubqueryValue -> SqlExpr.Subquery(
                query.materializeSqlQuery(parameterValues, parameterFields = parameterFields)
            )
            null -> SqlExpr.NullLiteral
            else -> bindParameter(field, this, parameterBaseName)
        }

    private fun Any?.toLikeConditionValueExpr(field: Field): SqlExpr =
        when (this) {
            is SqlExpr -> this
            is KronosFunctionExpr -> expr
            is Field -> column(this)
            is KSelectable<*> -> SqlExpr.Subquery(
                materializeSqlQuery(parameterValues, parameterFields = parameterFields)
            )
            is QuantifiedSubqueryValue -> SqlExpr.Subquery(
                query.materializeSqlQuery(parameterValues, parameterFields = parameterFields)
            )
            null -> SqlExpr.NullLiteral
            else -> bindParameter(
                field,
                PreparedValue(
                    value = toString(),
                    sourceType = typeOf<String>(),
                    kind = PreparedValueKind.READY_DATABASE_VALUE
                ),
                field.parameterBaseName()
            )
        }

    private fun Any?.toLiteralLikeConditionValueExpr(
        field: Field,
        prefixWildcard: Boolean,
        suffixWildcard: Boolean
    ): SqlExpr =
        when (this) {
            is SqlExpr -> this
            is KronosFunctionExpr -> expr
            is Field -> column(this)
            is KSelectable<*> -> SqlExpr.Subquery(
                materializeSqlQuery(parameterValues, parameterFields = parameterFields)
            )
            is QuantifiedSubqueryValue -> SqlExpr.Subquery(
                query.materializeSqlQuery(parameterValues, parameterFields = parameterFields)
            )
            null -> SqlExpr.NullLiteral
            else -> bindParameter(
                field,
                PreparedValue(
                    value = toLiteralLikePattern(prefixWildcard, suffixWildcard),
                    sourceType = typeOf<String>(),
                    kind = PreparedValueKind.READY_DATABASE_VALUE
                ),
                field.parameterBaseName()
            )
        }

    private fun Any.toLiteralLikePattern(prefixWildcard: Boolean, suffixWildcard: Boolean): String =
        buildString {
            if (prefixWildcard) append('%')
            append(escapeLikeLiteral(this@toLiteralLikePattern.toString()))
            if (suffixWildcard) append('%')
        }

    private fun Any?.toInRightOperand(field: Field): SqlInRightOperand =
        when (this) {
            is KSelectable<*> -> SqlInRightOperand.Subquery(
                materializeSqlQuery(parameterValues, parameterFields = parameterFields)
            )
            is Iterable<*> -> SqlInRightOperand.Values(listOf(bindListParameter(field, this)))
            is Array<*> -> SqlInRightOperand.Values(listOf(bindListParameter(field, this)))
            is BooleanArray -> SqlInRightOperand.Values(listOf(bindListParameter(field, this)))
            is ByteArray -> SqlInRightOperand.Values(listOf(bindListParameter(field, this)))
            is CharArray -> SqlInRightOperand.Values(listOf(bindListParameter(field, this)))
            is DoubleArray -> SqlInRightOperand.Values(listOf(bindListParameter(field, this)))
            is FloatArray -> SqlInRightOperand.Values(listOf(bindListParameter(field, this)))
            is IntArray -> SqlInRightOperand.Values(listOf(bindListParameter(field, this)))
            is LongArray -> SqlInRightOperand.Values(listOf(bindListParameter(field, this)))
            is ShortArray -> SqlInRightOperand.Values(listOf(bindListParameter(field, this)))
            else -> SqlInRightOperand.Values(listOf(toConditionValueExpr(field)))
        }

    private fun bindListParameter(field: Field, values: Any?): SqlExpr =
        bindParameter(field, values, field.parameterBaseName("List"), expandAsList = true)

    private fun Any?.toBetweenExpr(leftExpr: SqlExpr, not: Boolean): SqlExpr? {
        val range = this as? ClosedRange<*> ?: return null
        return SqlExpr.Between(leftExpr, range.start.toLiteralExpr(), range.endInclusive.toLiteralExpr(), withNot = not)
    }

    private fun Any?.toLiteralExpr(): SqlExpr =
        when (this) {
            is SqlExpr -> this
            is KronosFunctionExpr -> expr
            is Field -> column(this)
            null -> SqlExpr.NullLiteral
            is String -> SqlExpr.StringLiteral(this)
            is Boolean -> SqlExpr.BooleanLiteral(this)
            is Number -> SqlExpr.NumberLiteral(toString())
            is Char -> SqlExpr.StringLiteral(toString())
            else -> SqlExpr.StringLiteral(toString())
        }

    fun quantifiedComparisonExpr(
        left: SqlExpr,
        operator: SqlQuantifiedComparisonOperator,
        value: QuantifiedSubqueryValue
    ): SqlExpr =
        SqlExpr.QuantifiedComparisonPredicate(
            expr = left,
            operator = operator,
            quantifier = value.quantifier,
            query = value.query.materializeSqlQuery(parameterValues, parameterFields = parameterFields)
        )

    fun existsExpr(query: KSelectable<*>, not: Boolean): SqlExpr =
        SqlExpr.ExistsPredicate(
            query.materializeSqlQuery(parameterValues, parameterFields = parameterFields),
            withNot = not
        )

    fun tupleExpr(fields: List<Field>): SqlExpr =
        SqlExpr.Tuple(fields.map { column(it) })

    private fun allocateParameterName(baseName: String): String {
        if (!parameterValues.containsKey(baseName)) {
            return baseName
        }
        var count = parameterNameCounter.getOrDefault(baseName, 0)
        var candidate: String
        do {
            count++
            candidate = "$baseName@$count"
        } while (parameterValues.containsKey(candidate))
        parameterNameCounter[baseName] = count
        return candidate
    }

    operator fun Any?.plus(@Suppress("UNUSED_PARAMETER") other: Any?): Any? = null

    operator fun Any?.minus(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    operator fun Any?.times(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    operator fun Any?.div(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    operator fun Any?.rem(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    /**
     * Retrieves the source value based on the provided 'fieldName'.
     *
     * @param fieldName the name of the field to retrieve the value for
     * @return the value associated with the provided 'fieldName', or null if not found
     */
    fun sourceValueByFieldName(fieldName: String): Any? {
        return sourceValues[fieldName]
    }

    val <T : Any?> T?.value get() = this

    @get:JvmName("valueN")
    val <T: Any> T.value get() = this

    /**
     * Check if the iterable contains the element
     *
     * Only for compiler plugin condition rewriting
     *
     * This expression always return `true` whether the iterable contains the element or not
     *
     * @param other the element to check
     * @return `true`
     */

    operator fun <K> Iterable<K?>?.contains(other: @kotlin.internal.NoInfer K?) = true

    @JvmName("containsOutK")
    operator fun <K> Collection<K?>?.contains(other: @kotlin.internal.NoInfer K?) = true

    operator fun <K> Array<K?>?.contains(other: @kotlin.internal.NoInfer K?) = true

    @JvmName("containsOutK")
    operator fun <K> Array<out K?>?.contains(other: @kotlin.internal.NoInfer K?) = true

    operator fun IntArray?.contains(other: @kotlin.internal.NoInfer Number?) = true

    operator fun LongArray?.contains(other: @kotlin.internal.NoInfer Number?) = true

    operator fun FloatArray?.contains(other: @kotlin.internal.NoInfer Number?) = true

    operator fun DoubleArray?.contains(other: @kotlin.internal.NoInfer Number?) = true

    operator fun CharArray?.contains(other: @kotlin.internal.NoInfer Char?) = true

    operator fun BooleanArray?.contains(other: @kotlin.internal.NoInfer Boolean?) = true

    operator fun CharSequence?.contains(other: @kotlin.internal.NoInfer Char?) = true
    
    operator fun CharSequence?.contains(other: CharSequence): Boolean = true

    @JvmName("containsSelectable")
    operator fun KSelectable<*>?.contains(@Suppress("UNUSED_PARAMETER") other: Any?) = true

    fun exists(@Suppress("UNUSED_PARAMETER") query: KSelectable<*>): Boolean = true

    @Suppress("UNCHECKED_CAST")
    fun <T> any(query: KSelectable<*>): T? =
        QuantifiedSubqueryValue(query, SqlSubqueryQuantifier.Any) as T?

    @Suppress("UNCHECKED_CAST")
    fun <T> some(query: KSelectable<*>): T? =
        QuantifiedSubqueryValue(query, SqlSubqueryQuantifier.Some) as T?

    @Suppress("UNCHECKED_CAST")
    fun <T> all(query: KSelectable<*>): T? =
        QuantifiedSubqueryValue(query, SqlSubqueryQuantifier.All) as T?

    val CharSequence?.contains get() = true

    fun <T> T?.cast() = this as Any?

    operator fun KPojo.minus(field: Any?) = this

    /**
     * Check if the Comparable<*> is greater than the specified
     *
     * Only for compiler plugin condition rewriting
     *
     * Return 1 whether which one is greater
     *
     * @param other The Comparable<*> to compare with.
     * @param T The type of the Comparable<*> to compare with.
     * @return `1`
     */
    operator fun <T> Comparable<T>?.compareTo(other: Comparable<T>?) = 1

    /**
     * Check if the Comparable<*> is greater than the specified
     *
     * Only for compiler plugin condition rewriting
     *
     * Return 1 whether which one is greater
     *
     * @param other The Comparable<*> to compare with.
     * @param T The type of the Comparable<*> to compare with.
     * @param R The type of the Comparable<*> to compare with.
     * @return `1`
     */
    @JvmName("compareToDifferentType")
    @UnsafeCondition("It's not safe to compare different Type, use `.cast()` to declare that the expression is safe.")
    operator fun <T, R> Comparable<T>?.compareTo(other: Comparable<R>?) = 1

    /**
     * Check if the Comparable<*> is greater than the specified
     *
     * Only for compiler plugin condition rewriting
     *
     * Return 1 whether which one is greater
     *
     * @param other The Comparable<*> to compare with.
     * @return `1`
     */
    operator fun Any?.compareTo(other: Any?) = 1

    /**
     * Keeps this SQL predicate when [boolean] is true.
     *
     * This is a Kronos condition-DSL operator rather than Kotlin's standard [kotlin.takeIf]. The nullable
     * receiver represents the SQL predicate produced by the expression on the left. [boolean] is evaluated
     * as ordinary Kotlin code and does not become part of the generated SQL.
     *
     * Example: `where { (it.age >= minAge).takeIf(minAge != null) }`
     *
     * @param boolean whether this SQL predicate should be kept
     * @return a compiler-rewritten placeholder value
     */
    fun Boolean?.takeIf(boolean: Boolean) = true

    /**
     * Keeps this SQL predicate when [boolean] is false.
     *
     * This is a Kronos condition-DSL operator rather than Kotlin's standard [kotlin.takeUnless]. The nullable
     * receiver represents the SQL predicate produced by the expression on the left. [boolean] is evaluated
     * as ordinary Kotlin code and does not become part of the generated SQL.
     *
     * Example: `where { (it.status == 0).takeUnless(includeInactive) }`
     *
     * @param boolean whether this SQL predicate should be omitted
     * @return a compiler-rewritten placeholder value
     */
    fun Boolean?.takeUnless(boolean: Boolean) = true

    /**
     * Checks if the given value is like the specified string.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is like the string or not
     *
     * @param other The string to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.like(other: String?) = true

    /**
     * Checks if the given value is not like the specified string.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is not like the string or not
     *
     * @param other The string to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.notLike(other: String?) = true

    /**
     * Checks if the given value is between the specified range.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is between the range or not
     *
     * @param other The range to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.between(other: ClosedRange<*>?) = true

    /**
     * Checks if the given value is not between the specified range.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is not between the range or not
     *
     * @param other The range to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.notBetween(other: ClosedRange<*>?) = true

    /**
     * Checks if the given value matches the specified string.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value matches the string or not
     *
     * @param other The string to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.startsWith(other: String?) = true

    /**
     * Checks if the given value matches the specified string.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value matches the string or not
     *
     * @param other The string to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.endsWith(other: String?) = true

    infix fun Comparable<*>?.regexp(other: String?) = true

    infix fun Comparable<*>?.notRegexp(other: String?) = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    fun String?.asSql() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    fun Boolean?.asSql() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.eq get() = true

    val KPojo.eq get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.neq get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.like get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.notLike get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.startsWith get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.endsWith get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Any?.isNull get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Any?.notNull get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.lt get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.gt get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.le get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin condition rewriting
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.ge get() = true

    val Comparable<*>?.regexp get() = true

    val Comparable<*>?.notRegexp get() = true

    fun buildContainsStr(str: String?): String? {
        return if(str == null) null
        else "%$str%"
    }

    companion object {
        /**
         * Runs the given block on a new instance of [KTableForCondition] with the given [T] object as the data source.
         *
         * @param T The type of the KPojo object.
         * @param block The block of code to be applied to the [KTableForCondition] instance.
         * @return The resulting [KTableForCondition] instance after applying the block.
         */
        fun <T : KPojo> T.afterFilter(block: KTableForCondition<T>.(T) -> Unit) =
            KTableForCondition<T>().block(this)

        fun <T : KPojo> T.afterFilter(
            sourceBinding: SourceBinding?,
            block: KTableForCondition<T>.(T) -> Unit
        ) = KTableForCondition<T>(sourceBinding).block(this)
    }
}

data class QuantifiedSubqueryValue(
    val query: KSelectable<*>,
    val quantifier: SqlSubqueryQuantifier
)
