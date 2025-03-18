package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.beans.transformers.TransformerManager
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.ValueTransformer
import com.kotlinorm.utils.Extensions.safeMapperTo
import kotlin.test.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals

enum class TestEnum {
    A, B, C
}

enum class TestEnum2 {
    A, B, C
}

class EnumValueTransformTest {

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
        val testEnum: TestEnum? = null,
        val testEnum2: TestEnum2? = null
    ) : KPojo

    class TestEnumTransformer : ValueTransformer {
        override fun isMatch(
            targetKotlinType: String,
            superTypesOfValue: List<String>,
            kClassOfValue: KClass<*>
        ): Boolean {
            return targetKotlinType in listOf(TestEnum::class.qualifiedName, TestEnum2::class.qualifiedName) &&
                    kClassOfValue == String::class
        }

        override fun transform(
            targetKotlinType: String,
            value: Any,
            superTypesOfValue: List<String>,
            dateTimeFormat: String?,
            kClassOfValue: KClass<*>
        ): Any {
            return when (targetKotlinType) {
                TestEnum::class.qualifiedName -> TestEnum.valueOf(value as String)
                TestEnum2::class.qualifiedName -> TestEnum2.valueOf(value as String)
                else -> throw IllegalArgumentException("Unsupported targetKotlinType: $targetKotlinType")
            }
        }
    }

    class GeneralJvmEnumTransformer : ValueTransformer {
        override fun isMatch(
            targetKotlinType: String,
            superTypesOfValue: List<String>,
            kClassOfValue: KClass<*>
        ): Boolean {
            return Class.forName(targetKotlinType).isEnum
        }

        override fun transform(
            targetKotlinType: String,
            value: Any,
            superTypesOfValue: List<String>,
            dateTimeFormat: String?,
            kClassOfValue: KClass<*>
        ): Any {
            return Class.forName(targetKotlinType).enumConstants.first { it.toString() == value }
        }
    }

    @Test
    fun testEnumTransformSpecial() {
        TransformerManager.registerValueTransformer(TestEnumTransformer())
        val map = mapOf(
            "id" to 1,
            "name" to "test",
            "age" to 18,
            "testEnum" to "A",
            "testEnum2" to "B"
        )

        // Notice:
        // `value transformer` only work for `safeMapperTo`, not for `mapperTo`
        val pojo = map.safeMapperTo<TestPojo>()

        assertEquals(
            TestPojo(
                id = 1,
                name = "test",
                age = 18,
                testEnum = TestEnum.A,
                testEnum2 = TestEnum2.B
            ), pojo
        )
    }

    @Test
    fun testEnumTransformGeneral() {
        TransformerManager.registerValueTransformer(GeneralJvmEnumTransformer())
        val map = mapOf(
            "id" to 1,
            "name" to "test",
            "age" to 18,
            "testEnum" to "A",
            "testEnum2" to "B"
        )

        // Notice:
        // `value transformer` only work for `safeMapperTo`, not for `mapperTo`
        val pojo = map.safeMapperTo<TestPojo>()

        assertEquals(
            TestPojo(
                id = 1,
                name = "test",
                age = 18,
                testEnum = TestEnum.A,
                testEnum2 = TestEnum2.B
            ), pojo
        )
    }
}