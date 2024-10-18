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

package com.kotlinorm.beans.config

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.enums.ConditionType.Companion.Between
import com.kotlinorm.enums.ConditionType.Companion.Equal
import com.kotlinorm.enums.ConditionType.Companion.Gt
import com.kotlinorm.enums.ConditionType.Companion.Ge
import com.kotlinorm.enums.ConditionType.Companion.Lt
import com.kotlinorm.enums.ConditionType.Companion.Le
import com.kotlinorm.enums.ConditionType.Companion.In
import com.kotlinorm.enums.ConditionType.Companion.Like
import com.kotlinorm.enums.ConditionType.Companion.Regexp
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.KOperationType.UPDATE
import com.kotlinorm.enums.KOperationType.DELETE
import com.kotlinorm.enums.NoValueStrategyType
import com.kotlinorm.enums.NoValueStrategyType.JudgeNull
import com.kotlinorm.enums.NoValueStrategyType.Ignore
import com.kotlinorm.enums.NoValueStrategyType.False
import com.kotlinorm.interfaces.NoValueStrategy
import com.kotlinorm.utils.ConditionSqlBuilder.isEmptyArrayOrCollection

object DefaultNoValueStrategy : NoValueStrategy {
    override fun ifNoValue(kOperateType: KOperationType, criteria: Criteria): NoValueStrategyType {
        return when (kOperateType) {
            UPDATE, DELETE -> when (criteria.type) {
                Equal -> JudgeNull
                Like, In, Between, Regexp -> NoValueStrategyType.fromValue((criteria.not).toString())
                Gt, Ge, Lt, Le -> False
                else -> Ignore
            }

            else ->
                if (criteria.type == In && criteria.value.isEmptyArrayOrCollection())
                    NoValueStrategyType.fromValue((criteria.not).toString())
                else
                    Ignore
        }
    }
}