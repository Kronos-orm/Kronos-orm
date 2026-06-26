package com.kotlinorm.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LimitClauseTest {

    @Test
    fun testLimitClauseWithoutOffset() {
        val limit = LimitClause(10, null)
        
        assertEquals(10, limit.limit)
        assertNull(limit.offset)
    }

    @Test
    fun testLimitClauseWithOffset() {
        val limit = LimitClause(20, 5)
        
        assertEquals(20, limit.limit)
        assertEquals(5, limit.offset)
    }

    @Test
    fun testLimitClauseEquality() {
        val limit1 = LimitClause(15, 10)
        val limit2 = LimitClause(15, 10)
        
        assertEquals(limit1, limit2)
    }

    @Test
    fun testLimitClauseWithZeroOffset() {
        val limit = LimitClause(100, 0)
        
        assertEquals(100, limit.limit)
        assertEquals(0, limit.offset)
    }

    @Test
    fun testLimitClauseForPagination() {
        val pageSize = 25
        val pageNumber = 3
        val offset = (pageNumber - 1) * pageSize
        
        val limit = LimitClause(pageSize, offset)
        
        assertEquals(25, limit.limit)
        assertEquals(50, limit.offset)
    }
}
