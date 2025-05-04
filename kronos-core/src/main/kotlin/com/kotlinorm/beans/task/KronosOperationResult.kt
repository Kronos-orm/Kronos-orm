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

/**
 * Kronos Operation Result
 *
 * the result of operation
 *
 * @property affectedRows the number of affected rows
 */
data class KronosOperationResult(
    val affectedRows: Int = 0
) {
    val stash = mutableMapOf<String, Any?>() // 存储临时数据的map
}