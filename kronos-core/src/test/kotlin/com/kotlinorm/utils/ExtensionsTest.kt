package com.kotlinorm.utils

import com.kotlinorm.utils.Extensions.isEmptyArrayOrCollection
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionsTest {

    @Test
    fun `isEmptyArrayOrCollection covers collections arrays primitive arrays and non collections`() {
        val actual = listOf(
            emptyList<String>().isEmptyArrayOrCollection(),
            listOf("value").isEmptyArrayOrCollection(),
            emptyArray<String>().isEmptyArrayOrCollection(),
            arrayOf("value").isEmptyArrayOrCollection(),
            intArrayOf().isEmptyArrayOrCollection(),
            intArrayOf(1).isEmptyArrayOrCollection(),
            longArrayOf().isEmptyArrayOrCollection(),
            longArrayOf(1L).isEmptyArrayOrCollection(),
            shortArrayOf().isEmptyArrayOrCollection(),
            shortArrayOf(1).isEmptyArrayOrCollection(),
            floatArrayOf().isEmptyArrayOrCollection(),
            floatArrayOf(1f).isEmptyArrayOrCollection(),
            doubleArrayOf().isEmptyArrayOrCollection(),
            doubleArrayOf(1.0).isEmptyArrayOrCollection(),
            booleanArrayOf().isEmptyArrayOrCollection(),
            booleanArrayOf(true).isEmptyArrayOrCollection(),
            byteArrayOf().isEmptyArrayOrCollection(),
            byteArrayOf(1).isEmptyArrayOrCollection(),
            "value".isEmptyArrayOrCollection(),
            null.isEmptyArrayOrCollection()
        )

        assertEquals(
            listOf(
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                false,
                false,
                false
            ),
            actual
        )
    }
}
