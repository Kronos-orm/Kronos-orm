package com.kotlinorm.utils

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.isEmptyArrayOrCollection
import com.kotlinorm.utils.Extensions.mapperTo
import com.kotlinorm.utils.Extensions.patchTo
import com.kotlinorm.utils.Extensions.safeMapperTo
import kotlin.test.Test
import kotlin.test.assertEquals

data class ExtensionSourcePojo(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class ExtensionTargetPojo(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

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

    @Test
    fun `mapper helpers hydrate target pojos from maps and source pojos`() {
        val data = linkedMapOf<String, Any?>("id" to 7, "name" to "Ada")
        val source = ExtensionSourcePojo(id = 8, name = "Grace")

        assertEquals(ExtensionTargetPojo(7, "Ada"), data.mapperTo(ExtensionTargetPojo::class) as ExtensionTargetPojo)
        assertEquals(ExtensionTargetPojo(7, "Ada"), data.safeMapperTo(ExtensionTargetPojo::class) as ExtensionTargetPojo)
        assertEquals(ExtensionTargetPojo(7, "Ada"), data.mapperTo<ExtensionTargetPojo>())
        assertEquals(ExtensionTargetPojo(7, "Ada"), data.safeMapperTo<ExtensionTargetPojo>())
        assertEquals(ExtensionTargetPojo(8, "Grace"), source.mapperTo<ExtensionTargetPojo>())
        assertEquals(ExtensionTargetPojo(8, "Grace"), source.safeMapperTo<ExtensionTargetPojo>())
        assertEquals(
            ExtensionTargetPojo(8, "Patched"),
            source.patchTo(ExtensionTargetPojo::class, "name" to "Patched", "missing" to "ignored") as ExtensionTargetPojo
        )
    }
}
