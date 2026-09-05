/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.compiler.support

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper

/** Supplies a deterministic dialect to compiler box tests that only inspect statements. */
object CompilerTestDataSourceWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:kronos-compiler-test"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql

    override fun toList(task: KAtomicQueryTask): List<Any?> = unsupportedExecution()

    override fun first(task: KAtomicQueryTask): Any? = unsupportedExecution()

    override fun update(task: KAtomicActionTask): Int = unsupportedExecution()

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = unsupportedExecution()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? = unsupportedExecution()

    private fun <T> unsupportedExecution(): T =
        error("Compiler test data source only supports statement generation.")
}
