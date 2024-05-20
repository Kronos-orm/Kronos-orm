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
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KBatchTask

/**
 * Kronos Atomic Batch Task
 *
 * Batch execution task for [com.kotlinorm.interfaces.KronosDataSourceWrapper.batchUpdate]
 * Created by ousc on 2024/4/18 23:10
 */
data class KronosAtomicBatchTask(
    override val sql: String,
    override val paramMapArr: Array<Map<String, Any?>>? = null,
    override val operationType: KOperationType
) : KAtomicActionTask, KBatchTask {

    @Deprecated("Please use 'paramMapArr' instead.")
    override val paramMap: Map<String, Any?> = mapOf()

    /**
     * Parses the SQL statement and returns a pair of JDBC SQL and a list of JDBC parameter lists.
     *
     * @return a pair of JDBC SQL and a list of JDBC parameter lists. If paramMapArr is null, an empty array is used.
     */
    fun parsed() = (paramMapArr ?: arrayOf()).map { parseSqlStatement(sql, it) }.let {
        Pair(it.firstOrNull()?.jdbcSql, it.map { parsedSql -> parsedSql.jdbcParamList })
    }

    /**
     * Checks if this object is equal to another object.
     *
     * @param other the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KronosAtomicBatchTask

        if (sql != other.sql) return false
        if (paramMapArr != null) {
            if (other.paramMapArr == null) return false
            if (!paramMapArr.contentEquals(other.paramMapArr)) return false
        } else if (other.paramMapArr != null) return false
        if (operationType != other.operationType) return false

        return true
    }

    /**
     * Calculates the hash code value for the object.
     *
     * @return the hash code value of the object.
     */
    override fun hashCode(): Int {
        var result = sql.hashCode()
        result = 31 * result + (paramMapArr?.contentHashCode() ?: 0)
        result = 31 * result + operationType.hashCode()
        return result
    }
}