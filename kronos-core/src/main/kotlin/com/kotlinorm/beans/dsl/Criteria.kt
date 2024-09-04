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

package com.kotlinorm.beans.dsl

import com.kotlinorm.Kronos
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.enums.ConditionType.Companion.And
import com.kotlinorm.enums.ConditionType.Companion.IsNull
import com.kotlinorm.enums.ConditionType.Companion.Or
import com.kotlinorm.enums.ConditionType.Companion.Root
import com.kotlinorm.enums.NoValueStrategy
import com.kotlinorm.enums.smart

/**
 * Criteria
 *
 * Criteria are used to construct SQL where clause
 *
 * @property field the field for the condition
 * @property type the type of the condition
 * @property not whether the condition is not
 * @property value the value
 * @property tableName the table name
 * @property noValueStrategy when the value is null, whether to generate sql
 * @property children the children criteria
 */
class Criteria(
    var field: Field = Field("", ""), // original parameter name
    var type: ConditionType, // condition type
    var not: Boolean = false, // whether the condition is not
    var value: Any? = null, // value
    val tableName: String? = "", // table name
    var noValueStrategy: NoValueStrategy = Kronos.noValueStrategy, // when the value is null, whether to generate sql,
    var children: MutableList<Criteria?> = mutableListOf()
) {

    init {
        if (type != ConditionType.EQUAL && noValueStrategy == NoValueStrategy.Ignore) {
            noValueStrategy = NoValueStrategy.Smart
        }
    }

    internal val valueAcceptable: Boolean
        get() = type != IsNull && type != And && type != Or && type != Root

    /**
     * Adds a child criteria to the list of children.
     *
     * @param criteria the criteria to be added as a child, or null to remove the last child
     */
    fun addChild(criteria: Criteria?) {
        children.add(criteria)
    }
}