package com.kotlinorm.orm.union

import com.kotlinorm.Kronos
import com.kotlinorm.beans.sample.Customer
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.orm.select.select
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

class MysqlUnionTest {
    object UnionWrapper : SampleMysqlJdbcWrapper() {
        var cursor = 0;
        override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
            return when (cursor++) {
                0 -> listOf(
                    MysqlUser(1, "user1", 1),
                    MysqlUser(2, "user2", 1)
                )

                1 -> listOf(
                    MysqlUser(3, "user3", 1),
                    MysqlUser(4, "user4", 1)
                )

                2 -> listOf(
                    Customer(1, "customer1", "1111111111", "aaaaa"),
                )

                else -> emptyList()
            }.map {
                @Suppress("UNCHECKED_CAST")
                it.toDataMap().filterValues { it != null } as Map<String, Any>
            }
        }
    }

    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            dataSource = { UnionWrapper }
        }
    }

    @Test
    fun testUnion() {
        val result = union(
            MysqlUser().select().where { it.id == 1 || it.id == 2 },
            MysqlUser().select().where { it.id == 3 || it.id == 4 },
            Customer().select().limit(1),
        ).query()

        assertEquals(
            listOf(
                mapOf(
                    "MysqlUserId" to 1,
                    "MysqlUserUsername" to "user1",
                    "MysqlUserScore" to 1,
                    "MysqlUser@1Id" to 3,
                    "MysqlUser@1Username" to "user3",
                    "MysqlUser@1Score" to 1,
                    "CustomerId" to 1,
                    "name" to "customer1",
                    "contactNumber" to "1111111111",
                    "email" to "aaaaa"
                ),
                mapOf(
                    "MysqlUserId" to 2,
                    "MysqlUserUsername" to "user2",
                    "MysqlUserScore" to 1,
                    "MysqlUser@1Id" to 4,
                    "MysqlUser@1Username" to "user4",
                    "MysqlUser@1Score" to 1,
                    "CustomerId" to null,
                    "name" to null,
                    "contactNumber" to null,
                    "email" to null
                )
            ),
            result
        )
    }

}