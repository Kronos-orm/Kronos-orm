package com.kotlinorm.ast

import com.kotlinorm.enums.SortType
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderByItemTest {

    @Test
    fun testOrderByItemAscending() {
        val expr = ColumnReference(tableAlias = "users", columnName = "name")
        val item = OrderByItem(expr, SortType.ASC)
        
        assertEquals(expr, item.expression)
        assertEquals(SortType.ASC, item.direction)
    }

    @Test
    fun testOrderByItemDescending() {
        val expr = ColumnReference(tableAlias = "users", columnName = "created_at")
        val item = OrderByItem(expr, SortType.DESC)
        
        assertEquals(expr, item.expression)
        assertEquals(SortType.DESC, item.direction)
    }

    @Test
    fun testOrderByItemWithFunctionCall() {
        val expr = FunctionCall("LOWER", listOf(ColumnReference(tableAlias = "users", columnName = "email")))
        val item = OrderByItem(expr, SortType.ASC)
        
        assertEquals(expr, item.expression)
        assertEquals(SortType.ASC, item.direction)
    }

    @Test
    fun testOrderByItemEquality() {
        val expr = ColumnReference(tableAlias = "products", columnName = "price")
        val item1 = OrderByItem(expr, SortType.DESC)
        val item2 = OrderByItem(expr, SortType.DESC)
        
        assertEquals(item1, item2)
    }

    @Test
    fun testMultipleOrderByItems() {
        val items = listOf(
            OrderByItem(ColumnReference(tableAlias = "users", columnName = "last_name"), SortType.ASC),
            OrderByItem(ColumnReference(tableAlias = "users", columnName = "first_name"), SortType.ASC),
            OrderByItem(ColumnReference(tableAlias = "users", columnName = "id"), SortType.DESC)
        )
        
        assertEquals(3, items.size)
        assertEquals(SortType.ASC, items[0].direction)
        assertEquals(SortType.ASC, items[1].direction)
        assertEquals(SortType.DESC, items[2].direction)
    }
}
