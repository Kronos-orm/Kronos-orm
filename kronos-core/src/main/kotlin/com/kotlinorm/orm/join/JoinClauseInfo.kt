package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KJoinable

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/7/19 14:26
 **/
data class JoinClauseInfo(
    val tableName: String,
    val selectFields: List<Pair<String , Field>>,
    val distinct: Boolean,
    val pagination: Boolean,
    val pageIndex: Int,
    val pageSize: Int,
    val limit: Int? = null,
    val whereClauseSql: String? = null,
    val groupByClauseSql: String? = null,
    val orderByClauseSql: String? = null,
    val havingClauseSql: String? = null,
    val joinSql: String = ""
)