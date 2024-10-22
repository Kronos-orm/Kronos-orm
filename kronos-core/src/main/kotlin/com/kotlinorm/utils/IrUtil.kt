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

package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex


fun fieldK2db(str: String): String {
    return Kronos.fieldNamingStrategy.k2db(str)
}

fun fieldDb2k(str: String): String {
    return Kronos.fieldNamingStrategy.db2k(str)
}

fun tableK2db(str: String): String {
    return Kronos.fieldNamingStrategy.k2db(str)
}

fun tableDb2k(str: String): String {
    return Kronos.fieldNamingStrategy.db2k(str)
}

fun getCreateTimeStrategy(): KronosCommonStrategy {
    return Kronos.createTimeStrategy
}

fun getUpdateTimeStrategy(): KronosCommonStrategy {
    return Kronos.updateTimeStrategy
}

fun getLogicDeleteStrategy(): KronosCommonStrategy {
    return Kronos.logicDeleteStrategy
}

fun getOptimisticLockStrategy(): KronosCommonStrategy {
    return Kronos.optimisticLockStrategy
}