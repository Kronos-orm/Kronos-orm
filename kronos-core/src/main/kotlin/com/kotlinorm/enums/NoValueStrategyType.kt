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

enum class NoValueStrategyType(val value: String) {
    Ignore("ignore"),
    False("false"),
    True("true"),
    JudgeNull("judgeNull"),
    Auto("auto");

    companion object {
        fun fromValue(value: String): NoValueStrategyType {
            return when (value) {
                "ignore" -> Ignore
                "false" -> False
                "true" -> True
                "judgeNull" -> JudgeNull
                "auto" -> Auto
                else -> throw IllegalArgumentException("No such value for NoValueStrategyType: $value")
            }
        }
    }
}