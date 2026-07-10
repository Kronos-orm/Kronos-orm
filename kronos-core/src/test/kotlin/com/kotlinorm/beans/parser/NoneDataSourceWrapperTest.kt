package com.kotlinorm.beans.parser

import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.exceptions.NoDataSourceException
import com.kotlinorm.i18n.Noun.noDataSourceMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.reflect.typeOf

class NoneDataSourceWrapperTest {

    @Test
    fun `none data source wrapper throws exact no data source message for every entry point`() {
        val queryTask = KronosAtomicQueryTask("SELECT 1", targetType = typeOf<Int>())
        val actionTask = KronosAtomicActionTask("UPDATE t SET id = :id", mapOf("id" to 1))
        val batchTask = KronosAtomicBatchTask("UPDATE t SET id = :id", arrayOf(mapOf("id" to 1)))

        assertNoDataSource { NoneDataSourceWrapper.url }
        assertNoDataSource { NoneDataSourceWrapper.userName }
        assertNoDataSource { NoneDataSourceWrapper.dbType }
        assertNoDataSource { NoneDataSourceWrapper.toList(queryTask) }
        assertNoDataSource { NoneDataSourceWrapper.first(queryTask) }
        assertNoDataSource { NoneDataSourceWrapper.update(actionTask) }
        assertNoDataSource { NoneDataSourceWrapper.batchUpdate(batchTask) }
        assertNoDataSource { NoneDataSourceWrapper.transact(null, null) { TransactionScope() } }
    }

    private fun assertNoDataSource(block: () -> Any?) {
        assertEquals(
            noDataSourceMessage,
            assertFailsWith<NoDataSourceException> { block() }.message
        )
    }
}
