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

package com.kotlinorm.orm.join

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.pagination.PagedClause
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinGeneratedArityBehaviorTest : MysqlTestBase() {

    @Test
    fun `generated join overloads register sources and total clauses through arity sixteen`() {
        val s1 = JoinSource1(1)
        val s2 = JoinSource2(2)
        val s3 = JoinSource3(3)
        val s4 = JoinSource4(4)
        val s5 = JoinSource5(5)
        val s6 = JoinSource6(6)
        val s7 = JoinSource7(7)
        val s8 = JoinSource8(8)
        val s9 = JoinSource9(9)
        val s10 = JoinSource10(10)
        val s11 = JoinSource11(11)
        val s12 = JoinSource12(12)
        val s13 = JoinSource13(13)
        val s14 = JoinSource14(14)
        val s15 = JoinSource15(15)
        val s16 = JoinSource16(16)

        val join2 = s1.join(s2) { _, _ -> }
        assertJoin(join2, ["join_source_1", "join_source_2"])
        assertPaged(join2.withTotal())
        val join3 = s1.join(s2, s3) { _, _, _ -> }
        assertJoin(join3, ["join_source_1", "join_source_2", "join_source_3"])
        assertPaged(join3.withTotal())
        val join4 = s1.join(s2, s3, s4) { _, _, _, _ -> }
        assertJoin(join4, ["join_source_1", "join_source_2", "join_source_3", "join_source_4"])
        assertPaged(join4.withTotal())
        val join5 = s1.join(s2, s3, s4, s5) { _, _, _, _, _ -> }
        assertJoin(join5, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5"])
        assertPaged(join5.withTotal())
        val join6 = s1.join(s2, s3, s4, s5, s6) { _, _, _, _, _, _ -> }
        assertJoin(join6, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6"])
        assertPaged(join6.withTotal())
        val join7 = s1.join(s2, s3, s4, s5, s6, s7) { _, _, _, _, _, _, _ -> }
        assertJoin(join7, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6", "join_source_7"])
        assertPaged(join7.withTotal())
        val join8 = s1.join(s2, s3, s4, s5, s6, s7, s8) { _, _, _, _, _, _, _, _ -> }
        assertJoin(join8, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6", "join_source_7", "join_source_8"])
        assertPaged(join8.withTotal())
        val join9 = s1.join(s2, s3, s4, s5, s6, s7, s8, s9) { _, _, _, _, _, _, _, _, _ -> }
        assertJoin(join9, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6", "join_source_7", "join_source_8", "join_source_9"])
        assertPaged(join9.withTotal())
        val join10 = s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10) { _, _, _, _, _, _, _, _, _, _ -> }
        assertJoin(join10, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6", "join_source_7", "join_source_8", "join_source_9", "join_source_10"])
        assertPaged(join10.withTotal())
        val join11 = s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11) { _, _, _, _, _, _, _, _, _, _, _ -> }
        assertJoin(join11, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6", "join_source_7", "join_source_8", "join_source_9", "join_source_10", "join_source_11"])
        assertPaged(join11.withTotal())
        val join12 = s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12) { _, _, _, _, _, _, _, _, _, _, _, _ -> }
        assertJoin(join12, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6", "join_source_7", "join_source_8", "join_source_9", "join_source_10", "join_source_11", "join_source_12"])
        assertPaged(join12.withTotal())
        val join13 = s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13) { _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        assertJoin(join13, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6", "join_source_7", "join_source_8", "join_source_9", "join_source_10", "join_source_11", "join_source_12", "join_source_13"])
        assertPaged(join13.withTotal())
        val join14 = s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14) { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        assertJoin(join14, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6", "join_source_7", "join_source_8", "join_source_9", "join_source_10", "join_source_11", "join_source_12", "join_source_13", "join_source_14"])
        assertPaged(join14.withTotal())
        val join15 = s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15) { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        assertJoin(join15, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6", "join_source_7", "join_source_8", "join_source_9", "join_source_10", "join_source_11", "join_source_12", "join_source_13", "join_source_14", "join_source_15"])
        assertPaged(join15.withTotal())
        val join16 = s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16) { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        assertJoin(join16, ["join_source_1", "join_source_2", "join_source_3", "join_source_4", "join_source_5", "join_source_6", "join_source_7", "join_source_8", "join_source_9", "join_source_10", "join_source_11", "join_source_12", "join_source_13", "join_source_14", "join_source_15", "join_source_16"])
        assertPaged(join16.withTotal())
    }

    private fun assertJoin(query: SelectFrom<*, *, *>, expectedTables: List<String>) {
        assertEquals(expectedTables, query.context.listOfPojo.map { it.second.__tableName })
    }

    private fun assertPaged(value: Any) {
        assertEquals(PagedClause::class, value::class)
    }
}

@Table("join_source_1")
data class JoinSource1(val id: Int? = null) : KPojo
@Table("join_source_2")
data class JoinSource2(val id: Int? = null) : KPojo
@Table("join_source_3")
data class JoinSource3(val id: Int? = null) : KPojo
@Table("join_source_4")
data class JoinSource4(val id: Int? = null) : KPojo
@Table("join_source_5")
data class JoinSource5(val id: Int? = null) : KPojo
@Table("join_source_6")
data class JoinSource6(val id: Int? = null) : KPojo
@Table("join_source_7")
data class JoinSource7(val id: Int? = null) : KPojo
@Table("join_source_8")
data class JoinSource8(val id: Int? = null) : KPojo
@Table("join_source_9")
data class JoinSource9(val id: Int? = null) : KPojo
@Table("join_source_10")
data class JoinSource10(val id: Int? = null) : KPojo
@Table("join_source_11")
data class JoinSource11(val id: Int? = null) : KPojo
@Table("join_source_12")
data class JoinSource12(val id: Int? = null) : KPojo
@Table("join_source_13")
data class JoinSource13(val id: Int? = null) : KPojo
@Table("join_source_14")
data class JoinSource14(val id: Int? = null) : KPojo
@Table("join_source_15")
data class JoinSource15(val id: Int? = null) : KPojo
@Table("join_source_16")
data class JoinSource16(val id: Int? = null) : KPojo
