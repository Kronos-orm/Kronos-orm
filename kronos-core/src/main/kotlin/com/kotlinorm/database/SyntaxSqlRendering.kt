/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.parser.NamedParameterUtils
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.render.RenderedSql
import com.kotlinorm.syntax.render.SqlRenderContext
import com.kotlinorm.syntax.render.toRenderedSql
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.utils.toDatabaseParameterValue

internal fun SqlStatement.renderForCore(
    dataSource: KronosDataSourceWrapper,
    parameterValues: Map<String, Any?>,
    fieldsMap: Map<String, Field> = emptyMap()
): RenderedSql {
    val rendered = toRenderedSql(
        SqlRenderContext(
            dialect = dataSource.sqlDialect,
            parameterValues = parameterValues
        )
    )
    val parameterNames = NamedParameterUtils.parseSqlStatement(rendered.sql).parameterNames
    val usedParameterNames = parameterNames.toSet()
    val parameters = rendered.parameters
        .filterKeys { it in usedParameterNames }
        .mapValues { (key, value) ->
            toDatabaseParameterValue(dataSource, fieldsMap, key, value)
        }
    return rendered.copy(
        parameters = parameters,
        parameterNames = parameterNames.filter { it in parameters }
    )
}
