package com.kotlinorm.orm.select

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.enums.PessimisticLock

data class SelectClauseInfo(
    val databaseName: String?,
    val tableName: String,
    val selectFields: List<Field>,
    val distinct: Boolean,
    val pagination: Boolean,
    val pageIndex: Int,
    val pageSize: Int,
    val limit: Int? = null,
    val lock: PessimisticLock? = null,
    val whereClauseSql: String? = null,
    val groupByClauseSql: String? = null,
    val orderByClauseSql: String? = null,
    val havingClauseSql: String? = null,
)