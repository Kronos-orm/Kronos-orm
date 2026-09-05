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

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.syntax.statement.SqlStatement

/**
 * Atomic mutation contract with operation-local metadata and generated-key output.
 *
 * Generated keys are populated by the executing wrapper only when [generatedKeyField]
 * applies. They remain raw database values; action execution does not run them through the
 * logical result decoder.
 */
interface KAtomicActionTask : KAtomicTask {
    override val statement: SqlStatement?

    /**
     * Mutable operation-local metadata forwarded to parameter binding and execution hooks.
     * Wrappers preserve the map by reference for the lifetime of one execution.
     */
    val stash: MutableMap<String, Any?>

    /**
     * Identity field whose database-generated value should be captured, or `null` when key retrieval is disabled.
     * A wrapper may additionally restrict retrieval to insert operations.
     */
    var generatedKeyField: Field?

    /**
     * Raw generated values returned by the data-source wrapper in driver order.
     * Wrappers append or replace entries according to their generated-key execution contract.
     */
    val generatedKeys: MutableList<Any?>

    /**
     * Numeric representation of the first generated key when it can be derived.
     * It remains `null` for non-numeric key shapes or when retrieval is disabled.
     */
    var lastInsertId: Long?
}
