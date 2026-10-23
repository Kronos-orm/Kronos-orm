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

package com.kotlinorm.beans.dsl

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.table.SqlJoinType
import kotlin.reflect.KType

/**
 * KJoinable
 *
 * KJoinable are used to construct SQL where clause with join
 *
 * @property tableName The name of the table of the joint table
 * @property joinType The type of the table joint
 * @property condition Conditions for joining tables
 * @property kType The complete declared type of the POJO
 * @property kPojo The POJO object
 */
class KJoinable (
    val tableName: String,
    val joinType: SqlJoinType,
    val kType: KType,
    val kPojo: KPojo,
    val condition: SqlExpr? = null,
    val tableAliasOverrides: Map<String, String> = emptyMap()
)
