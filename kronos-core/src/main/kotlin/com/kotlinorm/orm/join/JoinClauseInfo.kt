/**
* Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.Field

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
    val datebaseOfTable: Map<String, String> = mapOf(),
    val whereClauseSql: String? = null,
    val groupByClauseSql: String? = null,
    val orderByClauseSql: String? = null,
    val havingClauseSql: String? = null,
    val joinSql: String = ""
)