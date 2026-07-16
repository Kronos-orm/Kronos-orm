/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.statement

import com.kotlinorm.database.SqlManager.renderStatement
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.toDatabaseValue

internal object OrmDmlRenderer {
    fun <T : KPojo> render(
        context: OrmContext<T>,
        wrapper: KronosDataSourceWrapper?,
        sqlStatement: SqlStatement
    ): RenderedOrmDml {
        val dataSource = wrapper.orDefault()
        val renderedSql = renderStatement(
            dataSource = dataSource,
            statement = sqlStatement,
            parameterValues = context.parameterValues(),
            fieldsMap = context.fieldMap
        )
        val parameters = context.renderedDatabaseParameters(
            dataSource,
            renderedSql.sql,
            context.fieldMap,
            ::toDatabaseValue
        )
        return RenderedOrmDml(
            sql = renderedSql.sql,
            paramMap = parameters,
            jdbcTypeHints = context.jdbcNullParameterTypeHints(parameters.keys),
            listParameterOccurrences = renderedSql.listParameterOccurrences
        )
    }
}

internal data class RenderedOrmDml(
    val sql: String,
    val paramMap: Map<String, Any?>,
    val jdbcTypeHints: Map<String, Int>,
    val listParameterOccurrences: Set<Int> = emptySet()
)
