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

package com.kotlinorm.enums

/**
 * Created by OUSC on 2022/4/18 10:49
 */

val Root = ConditionType.ROOT
val Like = ConditionType.LIKE
val Equal = ConditionType.EQUAL
val In = ConditionType.IN
val ISNULL = ConditionType.ISNULL
val SQL = ConditionType.SQL
val GT = ConditionType.GT
val GE = ConditionType.GE
val LT = ConditionType.LT
val LE = ConditionType.LE
val BETWEEN = ConditionType.BETWEEN
val AND = ConditionType.AND
val OR = ConditionType.OR
val REGEXP = ConditionType.REGEXP

val ASC = SortType.ASC
val DESC = SortType.DESC

val ignore = NoValueStrategy.Ignore
val alwaysFalse = NoValueStrategy.False
val alwaysTrue = NoValueStrategy.True
val judgeNull = NoValueStrategy.JudgeNull
val smart = NoValueStrategy.Smart