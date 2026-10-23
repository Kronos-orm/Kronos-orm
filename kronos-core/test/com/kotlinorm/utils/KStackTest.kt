package com.kotlinorm.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class KStackTest {
    @Test
    fun collectionLiteralCreatesStackInOrder() {
        val stack: KStack<Int> = [1, 2]

        stack.push(3)

        assertEquals(3, stack.pop())
        assertEquals(2, stack.pop())
        assertEquals(1, stack.pop())
    }
}
