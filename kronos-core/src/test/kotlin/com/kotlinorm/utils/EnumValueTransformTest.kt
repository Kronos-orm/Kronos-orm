package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.beans.transformers.TransformerManager
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.ValueTransformer
import com.kotlinorm.utils.Extensions.safeMapperTo
import kotlin.test.Test
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.test.assertEquals

enum class TestEnum {
    A, B, C
}

enum class TestEnum2 {
    A, B, C
}

class EnumValueTransformTest {

    init {
        with(Kronos) {
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
            targetKotlinType: KType,
            sourceValueClass: KClass<*>
        ): Boolean {
            return targetKotlinType.classifier in [TestEnum::class, TestEnum2::class] &&
                    sourceValueClass == String::class
        }

        override fun transform(
            targetKotlinType: KType,
            value: Any,
            dateTimeFormat: String?,
            sourceValueClass: KClass<*>
        ): Any {
            return when (targetKotlinType.classifier) {
                TestEnum::class -> TestEnum.valueOf(value as String)
                TestEnum2::class -> TestEnum2.valueOf(value as String)
                else -> throw IllegalArgumentException("Unsupported targetKotlinType: $targetKotlinType")
            }
        }
    }

    class GeneralJvmEnumTransformer : ValueTransformer {
        override fun isMatch(
            targetKotlinType: KType,
            sourceValueClass: KClass<*>
        ): Boolean {
            val targetClass = targetKotlinType.classifier as? KClass<*> ?: return false
            return targetClass.supertypes.any { it.classifier == Enum::class }
        }

        override fun transform(
            targetKotlinType: KType,
            value: Any,
            dateTimeFormat: String?,
            sourceValueClass: KClass<*>
        ): Any {
            val targetClass = targetKotlinType.classifier as KClass<*>
            return requireNotNull(
                Class.forName(targetClass.qualifiedName!!).enumConstants.firstOrNull { it.toString() == value }
            )
        }
    }

    @Test
    fun testEnumTransformSpecial() {
        TransformerManager.registerValueTransformer(TestEnumTransformer())
        Kronos.strictSetValue = false
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
        TransformerManager.unregisterValueTransformer(TestEnumTransformer())
        TransformerManager.registerValueTransformer(GeneralJvmEnumTransformer())
        Kronos.strictSetValue = false
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
