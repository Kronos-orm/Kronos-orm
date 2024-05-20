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

package com.kotlinorm.beans.task

import com.kotlinorm.beans.dsw.NamedParameterUtils.parseSqlStatement
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KAtomicQueryTask

/**
 * Kronos Atomic Query Task
 *
 * Atomic execution task for Select
 *
 * @author OUSC
 * @create 2024/4/18 23:05
 */
data class KronosAtomicQueryTask(
    override val sql: String,
    override val paramMap: Map<String, Any?> = mapOf(),
    override val operationType: KOperationType = KOperationType.SELECT
) : KAtomicQueryTask {

    /**
     * Parses the SQL statement and returns a pair of JDBC SQL and a list of JDBC parameter lists.
     *
     * @return a pair of JDBC SQL and a list of JDBC parameter lists. If paramMapArr is null, an empty array is used.
     */
    override fun parsed() = parseSqlStatement(sql, paramMap)
}