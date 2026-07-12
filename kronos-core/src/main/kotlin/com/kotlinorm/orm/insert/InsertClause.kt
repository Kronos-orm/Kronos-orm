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

package com.kotlinorm.orm.insert

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForInsertSelect.Companion.afterInsertSelect
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KronosFunctionExpr
import com.kotlinorm.beans.generator.SnowflakeIdGenerator
import com.kotlinorm.beans.generator.UUIDGenerator
import com.kotlinorm.beans.generator.customIdGenerator
import com.kotlinorm.beans.task.GeneratedKeyRequest
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.database.SqlManager.renderStatement
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeInsertClause
import com.kotlinorm.orm.sql.materializeSqlQuery
import com.kotlinorm.orm.union.UnionClause
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.inspect.SqlNodeRewriter
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.types.ToReference
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.createInstance
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.allocateBindParameterName
import com.kotlinorm.utils.resolveRuntimeMetadata
import com.kotlinorm.utils.resolvePrimaryKey
import com.kotlinorm.utils.toDatabaseBooleanValue
import kotlin.reflect.KClass

class InsertClause<T : KPojo>(val pojo: T) {
    private val metadata = pojo.resolveRuntimeMetadata()
    private val paramMap = pojo.toDataMap()
    private val tableName = metadata.tableName
    private var kClass = metadata.kClass
    private var createTimeStrategy = metadata.createTimeStrategy
    private var updateTimeStrategy = metadata.updateTimeStrategy
    private var logicDeleteStrategy = metadata.logicDeleteStrategy
    private var optimisticStrategy = metadata.optimisticLockStrategy
    private var primaryKey = metadata.primaryKey ?: resolvePrimaryKey(metadata.kClass, metadata.allColumns)
    internal var allColumns = metadata.allColumns
    private var cascadeEnabled = true
    private var withGeneratedId = false
    private var identityGeneratedKeyRequest: GeneratedKeyRequest? = null
    private var sourceQuery: KSelectable<*>? = null
    private var sourceUnion: UnionClause<*>? = null
    private var sourceValueProvider: ((List<Field>) -> List<Any?>)? = null

    /**
     * cascadeAllowed
     *
     * Fields that are allowed to use cascade, if not set, all fields are allowed to use cascade
     *
     * 允许级联的字段，若为空则允许所有字段级联
     */
    internal var cascadeAllowed: Set<Field>? = null

    fun cascade(enabled: Boolean): InsertClause<T> {
        cascadeEnabled = enabled
        return this
    }

    fun cascade(someFields: ToReference<T, Any?>): InsertClause<T> {
        someFields ?: throw EmptyFieldsException()
        cascadeEnabled = true
        pojo.afterReference {
            someFields(it)
            if (fields.isEmpty()) throw EmptyFieldsException()
            cascadeAllowed = fields.toSet()
        }
        return this
    }

    fun withId(): InsertClause<T> {
        withGeneratedId = true
        return this
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        val dataSource = wrapper.orDefault()
        if (sourceQuery != null || sourceUnion != null) {
            return buildSourceInsert(wrapper)
        }
        val toInsertFields = prepareInsertFields(dataSource, includeUnsetDefaultValueFields = false)
        val finalStatement = toSqlInsertStatement(toInsertFields)
        val renderedSql = renderStatement(dataSource, finalStatement, paramMap, metadata.fieldMap)
        val sql = renderedSql.sql
        val paramMapNew = renderedSql.parameters

        return CascadeInsertClause.build(
            cascadeEnabled,
            cascadeAllowed,
            pojo,
            KronosAtomicActionTask(
                sql,
                paramMapNew,
                operationType = KOperationType.INSERT,
                statement = finalStatement,
                generatedKeyRequest = identityGeneratedKeyRequest.takeIf { withGeneratedId }
            )
        )

    }

    private fun buildSourceInsert(wrapper: KronosDataSourceWrapper?): KronosActionTask {
        val dataSource = wrapper.orDefault()
        val toInsertFields = prepareInsertFields(
            dataSource,
            includeUnsetDefaultValueFields = sourceValueProvider != null
        )
        val parameterValues = linkedMapOf<String, Any?>()
        val statement = buildSourceInsertStatement(dataSource, toInsertFields, parameterValues)
        val renderedSql = renderStatement(dataSource, statement, parameterValues, metadata.fieldMap)
        return CascadeInsertClause.build(
            cascadeEnabled,
            cascadeAllowed,
            pojo,
            KronosAtomicActionTask(
                renderedSql.sql,
                renderedSql.parameters,
                operationType = KOperationType.INSERT,
                statement = statement,
                generatedKeyRequest = identityGeneratedKeyRequest.takeIf { withGeneratedId }
            )
        )
    }

    fun toSqlStatement(
        parameterValues: MutableMap<String, Any?> = linkedMapOf(),
        wrapper: KronosDataSourceWrapper? = null
    ): SqlDmlStatement.Insert {
        val dataSource = wrapper.orDefault()
        val toInsertFields = prepareInsertFields(
            dataSource,
            includeUnsetDefaultValueFields = sourceValueProvider != null
        )
        if (sourceQuery != null || sourceUnion != null) {
            return buildSourceInsertStatement(dataSource, toInsertFields, parameterValues)
        }
        toInsertFields.forEach { field ->
            parameterValues[field.name] = paramMap[field.name]
        }
        return toSqlInsertStatement(toInsertFields)
    }

    private fun buildSourceInsertStatement(
        dataSource: KronosDataSourceWrapper,
        toInsertFields: List<Field>,
        parameterValues: MutableMap<String, Any?>
    ): SqlDmlStatement.Insert {
        val sourceSelectable = sourceQuery ?: sourceUnion ?: error("INSERT SELECT requires a source query.")
        val source = sourceSelectable.materializeSqlQuery(parameterValues, mutableMapOf(), dataSource)
        val sourceProjection = sourceValueProvider?.invoke(toInsertFields)
            ?.also { provided ->
                require(provided.size == toInsertFields.size) {
                    "Insert-select value count (${provided.size}) must match target insertable field count (${toInsertFields.size})."
                }
            }
            ?.mapIndexed { index, value ->
                value.toInsertSelectSqlExpr(toInsertFields[index], parameterValues, dataSource)
            }
        val finalSource = sourceProjection
            ?.let { source.rewriteProjection(it) }
            ?: source.also { it.validateInsertSelectArity(toInsertFields.size) }
        return SqlDmlStatement.Insert(
            table = SqlTable.Ident(tableName),
            columns = toInsertFields.map { SqlIdentifier.of(it.columnName) },
            mode = SqlInsertMode.Subquery(finalSource)
        )
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build(wrapper).execute(wrapper)
    }

    @PublishedApi
    internal fun <S : KPojo> fromSource(
        query: KSelectable<S>,
        values: ToInsertSelect<S, Any?> = null
    ): InsertClause<T> {
        if (query is UnionClause<*>) {
            sourceQuery = null
            sourceUnion = query
        } else {
            sourceQuery = query
            sourceUnion = null
        }
        sourceValueProvider = values?.let { insertValues ->
            {
                @Suppress("UNCHECKED_CAST")
                val selectedClass = query.selectedType.classifier as KClass<S>
                val source = selectedClass.createInstance()
                source.afterInsertSelect { insertValues(it) }
            }
        }
        cascadeEnabled = false
        return this
    }

    @PublishedApi
    internal fun fromSource(
        query: UnionClause<*>,
        values: ((List<Field>) -> List<Any?>)? = null
    ): InsertClause<T> {
        sourceQuery = null
        sourceUnion = query
        sourceValueProvider = values
        cascadeEnabled = false
        return this
    }

    private fun toSqlInsertStatement(toInsertFields: List<Field>): SqlDmlStatement.Insert =
        SqlDmlStatement.Insert(
            table = SqlTable.Ident(tableName),
            columns = toInsertFields.map { field -> SqlIdentifier.of(field.columnName) },
            mode = SqlInsertMode.Values(
                listOf(
                    toInsertFields.map { field ->
                        SqlExpr.Parameter(SqlParameter.Named(field.name))
                    }
                )
            )
        )

    private fun prepareInsertFields(
        dataSource: KronosDataSourceWrapper,
        includeUnsetDefaultValueFields: Boolean
    ): MutableList<Field> {
        var databaseGeneratesIdentity = false
        identityGeneratedKeyRequest = null
        val toInsertFields = mutableListOf<Field>()
        val primaryKeyField = primaryKey

        when (primaryKeyField.primaryKey) {
            PrimaryKeyType.UUID -> paramMap[primaryKeyField.name] = UUIDGenerator.nextId()
            PrimaryKeyType.SNOWFLAKE -> paramMap[primaryKeyField.name] = SnowflakeIdGenerator.nextId()
            PrimaryKeyType.CUSTOM -> paramMap[primaryKeyField.name] = customIdGenerator?.nextId()
            PrimaryKeyType.IDENTITY -> databaseGeneratesIdentity = true
            else -> {}
        }
        if (paramMap[primaryKeyField.name] != null || primaryKeyField.defaultValue != null) {
            databaseGeneratesIdentity = false
        }
        if (databaseGeneratesIdentity) {
            identityGeneratedKeyRequest = GeneratedKeyRequest(tableName, primaryKeyField.columnName)
        }

        arrayOf(
            createTimeStrategy to true,
            updateTimeStrategy to true,
            optimisticStrategy to false
        ).forEach {
            it.first?.execute(it.second) { field, value ->
                paramMap[field.name] = value
            }
        }
        logicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
            paramMap[field.name] = toDatabaseBooleanValue(dataSource, field, false)
        }

        allColumns.forEach {
            if (!it.isColumn) return@forEach
            if (it.primaryKey == PrimaryKeyType.IDENTITY && paramMap[it.name] == null) return@forEach
            if (!includeUnsetDefaultValueFields && it.defaultValue != null && paramMap[it.name] == null) return@forEach
            toInsertFields.add(it)
        }
        return toInsertFields
    }

    private fun SqlQuery.rewriteProjection(values: List<SqlExpr>): SqlQuery =
        when (this) {
            is SqlQuery.Select -> copy(select = values.map { SqlSelectItem.Expr(it.remapSelectOutputReferences(select)) })
            is SqlQuery.Set -> copy(left = left.rewriteProjection(values), right = right.rewriteProjection(values))
            is SqlQuery.With -> copy(query = query.rewriteProjection(values))
            is SqlQuery.Values -> error("INSERT SELECT source projection rewrite requires a SELECT query.")
        }

    private fun SqlQuery.validateInsertSelectArity(expected: Int) {
        when (this) {
            is SqlQuery.Select -> requireInsertSelectArity(select.size, expected)
            is SqlQuery.Set -> {
                left.validateInsertSelectArity(expected)
                right.validateInsertSelectArity(expected)
            }
            is SqlQuery.With -> query.validateInsertSelectArity(expected)
            is SqlQuery.Values -> values.forEach { row -> requireInsertSelectArity(row.size, expected) }
        }
    }

    private fun requireInsertSelectArity(actual: Int, expected: Int) {
        require(actual == expected) {
            "Insert-select source column count ($actual) must match target insertable field count ($expected)."
        }
    }

    private fun SqlExpr.remapSelectOutputReferences(selectItems: List<SqlSelectItem>): SqlExpr {
        val outputExprs = selectItems.mapNotNull { item ->
            val exprItem = item as? SqlSelectItem.Expr ?: return@mapNotNull null
            val metadata = exprItem.metadata ?: return@mapNotNull null
            val source = metadata.source
            metadata.outputName to when {
                metadata.scope == SqlSelectItemSourceScope.Source && source != null -> SqlExpr.Column(
                    tableName = source.tableName,
                    columnName = source.columnName,
                    qualifier = source.qualifier,
                    identifier = source.identifier
                )
                else -> metadata.expression
            }
        }.toMap()

        if (outputExprs.isEmpty()) return this

        return object : SqlNodeRewriter {
            override fun rewriteExpr(expr: SqlExpr): SqlExpr =
                when (expr) {
                    is SqlExpr.Column -> outputExprs[expr.columnName] ?: outputExprs[expr.identifier.last] ?: expr
                    else -> super.rewriteExpr(expr)
                }
        }.rewriteExpr(this)
    }

    private fun Any?.toInsertSelectSqlExpr(
        targetField: Field,
        parameterValues: MutableMap<String, Any?>,
        dataSource: KronosDataSourceWrapper
    ): SqlExpr {
        return when (this) {
            null -> SqlExpr.NullLiteral
            is SqlExpr -> this
            is KronosFunctionExpr -> expr
            is Field -> SqlExpr.Column(
                tableName = tableName?.takeIf { it.isNotBlank() },
                columnName = columnName
            )
            is KSelectable<*> -> SqlExpr.Subquery(materializeSqlQuery(parameterValues, mutableMapOf(), dataSource))
            else -> {
                val paramName = allocateBindParameterName(
                    targetField.name,
                    parameterValues,
                    mutableMapOf()
                )
                parameterValues[paramName] = this
                SqlExpr.Parameter(SqlParameter.Named(paramName))
            }
        }
    }

}
