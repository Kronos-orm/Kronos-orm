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

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.enums.JoinType
import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.KClass

/**
 * KJoinable
 *
 * KJoinable are used to construct SQL where clause with join
 *
 * @property tableName The name of the table of the joint table
 * @property joinType The type of the table joint
 * @property condition Conditions for joining tables
 * @property kClass The class of the POJO
 * @property kPojo The POJO object
 */
class KJoinable (
    val tableName: String,
    val joinType: JoinType,
    val condition: Criteria?,
    val kClass: KClass<KPojo>,
    val kPojo: KPojo
)