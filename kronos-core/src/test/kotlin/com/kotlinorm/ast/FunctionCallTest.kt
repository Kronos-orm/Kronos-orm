package com.kotlinorm.ast

import com.kotlinorm.enums.SortType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FunctionCallTest {

    @Test
    fun testSimpleFunctionCall() {
        val func = FunctionCall("COUNT", listOf(ColumnReference(tableAlias = "users", columnName = "id")))
        
        assertEquals("COUNT", func.functionName)
        assertEquals(1, func.arguments.size)
        assertFalse(func.distinct)
        assertNull(func.filter)
        assertNull(func.over)
    }

    @Test
    fun testFunctionCallWithNoArguments() {
        val func = FunctionCall("NOW")
        
        assertEquals("NOW", func.functionName)
        assertEquals(0, func.arguments.size)
    }

    @Test
    fun testFunctionCallWithDistinct() {
        val func = FunctionCall(
            "COUNT",
            listOf(ColumnReference(tableAlias = "users", columnName = "email")),
            distinct = true
        )
        
        assertTrue(func.distinct)
    }

    @Test
    fun testFunctionCallWithFilter() {
        val filter = BinaryExpression(
            ColumnReference(tableAlias = "users", columnName = "age"),
            SqlOperator.GREATER_THAN,
            Literal.NumberLiteral("18")
        )
        val func = FunctionCall(
            "COUNT",
            listOf(ColumnReference(tableAlias = "users", columnName = "id")),
            filter = filter
        )
        
        assertNotNull(func.filter)
        assertTrue(func.filter is BinaryExpression)
    }

    @Test
    fun testFunctionCallWithWindowClause() {
        val window = WindowClause(
            partitionBy = listOf(ColumnReference(tableAlias = "users", columnName = "department")),
            orderBy = listOf(OrderByItem(ColumnReference(tableAlias = "users", columnName = "salary"), SortType.DESC))
        )
        val func = FunctionCall(
            "ROW_NUMBER",
            emptyList(),
            over = window
        )
        
        assertNotNull(func.over)
        assertNotNull(func.over?.partitionBy)
        assertNotNull(func.over?.orderBy)
    }

    @Test
    fun testFunctionCallWithMultipleArguments() {
        val func = FunctionCall(
            "CONCAT",
            listOf(
                ColumnReference(tableAlias = "users", columnName = "first_name"),
                Literal.StringLiteral(" "),
                ColumnReference(tableAlias = "users", columnName = "last_name")
            )
        )
        
        assertEquals(3, func.arguments.size)
    }

    @Test
    fun testWindowClauseWithPartitionBy() {
        val window = WindowClause(
            partitionBy = listOf(
                ColumnReference(tableAlias = "sales", columnName = "region"),
                ColumnReference(tableAlias = "sales", columnName = "product")
            )
        )
        
        assertEquals(2, window.partitionBy?.size)
        assertNull(window.orderBy)
        assertNull(window.frame)
    }

    @Test
    fun testWindowClauseWithOrderBy() {
        val window = WindowClause(
            orderBy = listOf(
                OrderByItem(ColumnReference(tableAlias = "sales", columnName = "date"), SortType.ASC),
                OrderByItem(ColumnReference(tableAlias = "sales", columnName = "amount"), SortType.DESC)
            )
        )
        
        assertEquals(2, window.orderBy?.size)
        assertNull(window.partitionBy)
    }

    @Test
    fun testWindowFrameBetween() {
        val frame = WindowFrame.BetweenFrame(
            type = WindowFrame.FrameType.ROWS,
            start = WindowFrame.FrameBoundary.UnboundedPreceding,
            end = WindowFrame.FrameBoundary.CurrentRow
        )
        
        assertEquals(WindowFrame.FrameType.ROWS, frame.type)
        assertTrue(frame.start is WindowFrame.FrameBoundary.UnboundedPreceding)
        assertTrue(frame.end is WindowFrame.FrameBoundary.CurrentRow)
    }

    @Test
    fun testWindowFrameSingleBoundary() {
        val frame = WindowFrame.SingleBoundaryFrame(
            type = WindowFrame.FrameType.RANGE,
            boundary = WindowFrame.FrameBoundary.UnboundedPreceding
        )
        
        assertEquals(WindowFrame.FrameType.RANGE, frame.type)
        assertTrue(frame.boundary is WindowFrame.FrameBoundary.UnboundedPreceding)
    }

    @Test
    fun testWindowFramePreceding() {
        val boundary = WindowFrame.FrameBoundary.Preceding(Literal.NumberLiteral("5"))
        
        assertTrue(boundary is WindowFrame.FrameBoundary.Preceding)
        assertEquals(Literal.NumberLiteral("5"), boundary.value)
    }

    @Test
    fun testWindowFrameFollowing() {
        val boundary = WindowFrame.FrameBoundary.Following(Literal.NumberLiteral("3"))
        
        assertTrue(boundary is WindowFrame.FrameBoundary.Following)
        assertEquals(Literal.NumberLiteral("3"), boundary.value)
    }

    @Test
    fun testWindowFrameWithExclude() {
        val frame = WindowFrame.BetweenFrame(
            type = WindowFrame.FrameType.ROWS,
            start = WindowFrame.FrameBoundary.UnboundedPreceding,
            end = WindowFrame.FrameBoundary.UnboundedFollowing,
            exclude = WindowFrame.ExcludeType.CURRENT_ROW
        )
        
        assertEquals(WindowFrame.ExcludeType.CURRENT_ROW, frame.exclude)
    }

    @Test
    fun testFunctionCallEquality() {
        val func1 = FunctionCall("MAX", listOf(ColumnReference(tableAlias = "users", columnName = "age")))
        val func2 = FunctionCall("MAX", listOf(ColumnReference(tableAlias = "users", columnName = "age")))
        
        assertEquals(func1, func2)
    }
}
