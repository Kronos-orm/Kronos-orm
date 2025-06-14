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

package com.kotlinorm.interfaces

import com.kotlinorm.Kronos.createTimeStrategy
import com.kotlinorm.Kronos.logicDeleteStrategy
import com.kotlinorm.Kronos.optimisticLockStrategy
import com.kotlinorm.Kronos.primaryKeyStrategy
import com.kotlinorm.Kronos.updateTimeStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex

/**
 * Kronos Pojo Class
 *
 * Interface for data class to be used in kronos
 * after extending [KPojo], a data class can be recognized as a table class in Kronos-orm
 *
 * @author OUSC
 */
interface KPojo {
    /**
     * kClass
     *
     * Get the KClass of the KPojo
     *
     * **the body of this function will be generated by the compiler plugin**
     */
    fun kClass() = KPojo::class
    /**
     * toDataMap
     *
     * Transform the KPojo into a mutable map where the keys are strings and the values are nullable Any types.
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return mutableMapOf<String, Any?>
     */
    fun toDataMap() = mutableMapOf<String, Any?>()

    /**
     * fromMapData
     *
     * Transform the mutable map into a KPojo
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KPojo
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : KPojo> safeFromMapData(map: Map<String, Any?>) = this as T

    /**
     * fromMapData
     *
     * Transform the mutable map into a KPojo
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KPojo
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : KPojo> fromMapData(map: Map<String, Any?>) = this as T

    /**
     * get
     *
     * Get the value of the field by name
     *
     * **the body of this function will be generated by the compiler plugin**
     * @param name the name of the field
     * @return Any?
     */
    operator fun get(name: String): Any? = null

    /**
     * set
     *
     * Set the value of the field by name
     *
     * **the body of this function will be generated by the compiler plugin**
     * @param name the name of the field
     * @param value the value of the field
     */
    operator fun set(name: String, value: Any?){}

    /**
     * kronos TableName
     *
     * The name of the table
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return String
     */
    fun kronosTableName() = ""

    /**
     * kronosTableComment
     *
     * The comment of the table
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return String
     */
    fun kronosTableComment() = ""

    /**
     * kronos TableName
     *
     * The name of the table
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return String
     */
    fun kronosTableIndex() = mutableListOf<KTableIndex>()

    /**
     * kronosColumns
     *
     * A list of fields to be recognized as columns in the table
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return MutableList<Field>
     */
    fun kronosColumns() = mutableListOf<Field>()

    /**
     * kronosPrimaryKey
     *
     * Primary key strategy to be recognized as the primary key
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return PrimaryKeyStrategy
     */
    fun kronosPrimaryKey() = primaryKeyStrategy

    /**
     * kronosCreateTime
     *
     * Common strategy to be recognized as the create_time
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KronosCommonStrategy
     */
    fun kronosCreateTime() = createTimeStrategy

    /**
     * kronosUpdateTime
     *
     * Common strategy to be recognized as the update_time
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KronosCommonStrategy
     */
    fun kronosUpdateTime() = updateTimeStrategy

    /**
     * kronosLogicDelete
     *
     * Common strategy to be recognized as the logic_delete
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KronosCommonStrategy
     */
    fun kronosLogicDelete() = logicDeleteStrategy

    /**
     * kronosOptimisticLock
     *
     * Common strategy to be recognized as the optimistic_lock
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KronosCommonStrategy
     */
    fun kronosOptimisticLock() = optimisticLockStrategy
}