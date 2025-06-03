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

package com.kotlinorm.beans.config

import com.kotlinorm.beans.dsl.Field

/**
 * Kronos Common Strategy
 *
 * Strategy for common
 *
 * @property enabled whether the strategy is enabled
 * @property field the field for the strategy
 */
class KronosCommonStrategy(
    var enabled: Boolean = false,
    var field: Field,
){
    fun bind(tableName: String): KronosCommonStrategy {
        field.tableName = tableName
        return this
    }
}