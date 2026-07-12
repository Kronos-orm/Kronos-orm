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
    ): Pair<String, Map<String, Any?>> {
        val dataSource = wrapper.orDefault()
        val renderedSqlText = renderStatement(
            dataSource = dataSource,
            statement = sqlStatement,
            parameterValues = context.parameterValues(),
            fieldsMap = context.fieldMap
        ).sql
        return renderedSqlText to context.renderedDatabaseParameters(
            dataSource,
            renderedSqlText,
            context.fieldMap,
            ::toDatabaseValue
        )
    }
}
