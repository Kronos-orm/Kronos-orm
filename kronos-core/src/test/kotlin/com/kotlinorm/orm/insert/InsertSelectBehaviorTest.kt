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

package com.kotlinorm.orm.insert

import com.kotlinorm.beans.subquery.SubqueryOrder
import com.kotlinorm.beans.subquery.SubqueryOrderArchive
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InsertSelectBehaviorTest : MysqlTestBase() {

    @Test
    fun `default source column count mismatch is rejected`() {
        val error = assertFailsWith<IllegalArgumentException> {
            SubqueryOrder()
                .select { it.id }
                .insert<SubqueryOrderArchive>()
                .build()
        }

        assertEquals(
            "Insert-select source column count (1) must match target insertable field count (3).",
            error.message
        )
    }

    @Test
    fun `union source branch column count mismatch is rejected`() {
        val error = assertFailsWith<IllegalArgumentException> {
            union(
                SubqueryOrder()
                    .select { [it.id, it.userId, it.status] },
                SubqueryOrder()
                    .select { [it.id, it.userId] }
            )
                .insert<SubqueryOrderArchive>()
                .build()
        }

        assertEquals(
            "Insert-select source column count (2) must match target insertable field count (3).",
            error.message
        )
    }

    @Test
    fun `default source column count mismatch after explicit select is rejected`() {
        val error = assertFailsWith<IllegalArgumentException> {
            SubqueryOrder()
                .select {
                    [
                        it.id,
                        it.userId
                    ]
                }
                .insert<SubqueryOrderArchive>()
                .build()
        }

        assertEquals(
            "Insert-select source column count (2) must match target insertable field count (3).",
            error.message
        )
    }
}
