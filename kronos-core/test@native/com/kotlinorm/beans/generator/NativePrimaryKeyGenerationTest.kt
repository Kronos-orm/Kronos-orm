@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.kotlinorm.beans.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class NativePrimaryKeyGenerationTest {
    @Test
    fun uuidValuesUseCanonicalPlatformIndependentFormat() {
        val values = List(UUID_COUNT) { UUIDGenerator.nextId() }
        assertEquals(UUID_COUNT, values.toSet().size)
        values.forEach { assertEquals(it, Uuid.parse(it).toString()) }
    }

    @Test
    fun snowflakeValuesAreUniqueAndOrdered() {
        val values = List(SNOWFLAKE_COUNT) { SnowflakeIdGenerator.nextId() }
        assertEquals(SNOWFLAKE_COUNT, values.toSet().size)
        assertTrue(values.zipWithNext().all { (previous, next) -> previous < next })
    }
}

private const val UUID_COUNT = 100
private const val SNOWFLAKE_COUNT = 1000
