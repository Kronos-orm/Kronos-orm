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

import com.kotlinorm.utils.getCreateTimeStrategy
import com.kotlinorm.utils.getLogicDeleteStrategy
import com.kotlinorm.utils.getUpdateTimeStrategy

/**
 * Kronos Pojo Class
 *
 * Interface for data class to be used in kronos
 * after extending [KPojo], a data class can be recognized as a table class in Kronos-orm
 *
 * @author OUSC
 */
abstract class KPojo {

    /**
     * toDataMap
     *
     * Transform the KPojo into a mutable map where the keys are strings and the values are nullable Any types.
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return mutableMapOf<String, Any?>
     */
    open fun toDataMap() = mutableMapOf<String, Any?>()

    /**
     * fromMapData
     *
     * Transform the mutable map into a KPojo
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KPojo
     */
    @Suppress("UNCHECKED_CAST")
    open fun <T : KPojo> safeFromMapData(map: Map<String, Any?>) = this as T

    /**
     * fromMapData
     *
     * Transform the mutable map into a KPojo
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KPojo
     */
    @Suppress("UNCHECKED_CAST")
    open fun <T : KPojo> fromMapData(map: Map<String, Any?>) = this as T

    /**
     * kronos TableName
     *
     * The name of the table
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return String
     */
    open fun kronosTableName() = ""

    /**
     * kronos TableName
     *
     * The name of the table
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return String
     */
    open fun kronosTableIndex() = mutableListOf<KTableIndex>()

    /**
     * kronosColumns
     *
     * A list of fields to be recognized as columns in the table
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return MutableList<Field>
     */
    open fun kronosColumns() = mutableListOf<Field>()

    /**
     * kronosCreateTime
     *
     * Common strategy to be recognized as the create_time
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KronosCommonStrategy
     */
    open fun kronosCreateTime() = getCreateTimeStrategy()

    /**
     * kronosUpdateTime
     *
     * Common strategy to be recognized as the update_time
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KronosCommonStrategy
     */
    open fun kronosUpdateTime() = getUpdateTimeStrategy()

    /**
     * kronosLogicDelete
     *
     * Common strategy to be recognized as the logic_delete
     *
     * **the body of this function will be generated by the compiler plugin**
     * @return KronosCommonStrategy
     */
    open fun kronosLogicDelete() = getLogicDeleteStrategy()
}