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

/**
 * Parameter contract for executing one SQL shape with multiple value maps.
 *
 * [paramMap] exists for compatibility with atomic task APIs and is not the batch payload.
 * Implementations expose ordered batch entries through [paramMapArr]; `null` means no entries.
 */
interface KBatchTask {
    /**
     * Compatibility view for single-task APIs.
     * Batch implementations may expose an empty map because [paramMapArr] is authoritative.
     */
    val paramMap: Map<String, Any?>

    /**
     * Parameter maps in batch execution order, or `null` for an empty batch.
     * Every entry is materialized independently against the batch's shared SQL.
     */
    val paramMapArr: Array<Map<String, Any?>>?
}
