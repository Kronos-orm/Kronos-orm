package com.kotlinorm.interfaces

import com.kotlinorm.Kronos
import com.kotlinorm.beans.transformers.TransformerManager.registerValueTransformer
import com.kotlinorm.utils.Extensions.safeMapperTo
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class KPojoTest {
    enum class TestEnum {
        A, B, C
    }

    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
        }
    }

    data class TestPojo(
        val id: Int? = null,
        val name: String? = null,
        val age: Int? = null,
        val testEnum: TestEnum? = null
    ) : KPojo

    class TestEnumTransformer : ValueTransformer {
        override fun isMatch(
            targetKotlinType: String,
            superTypesOfValue: List<String>,
            kClassOfValue: KClass<*>
        ): Boolean {
            return targetKotlinType == TestEnum::class.qualifiedName && kClassOfValue == String::class
        }

        override fun transform(
            targetKotlinType: String,
            value: Any,
            superTypesOfValue: List<String>,
            dateTimeFormat: String?,
            kClassOfValue: KClass<*>
        ): Any {
            return TestEnum.valueOf(value as String)
        }
    }

    @Test
    fun testKPojo() {
        registerValueTransformer(TestEnumTransformer())
        val map = mapOf(
            "id" to 1,
            "name" to "test",
            "age" to 18,
            "testEnum" to "A"
        )

        // Notice:
        // `value transformer` only work for `safeMapperTo`, not for `mapperTo`
        val pojo = map.safeMapperTo<TestPojo>()

        assertEquals(
            TestPojo(
                id = 1,
                name = "test",
                age = 18,
                testEnum = TestEnum.A
            ), pojo
        )
    }
}