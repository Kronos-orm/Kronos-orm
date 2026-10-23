package com.kotlinorm.beans.generator

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IdGeneratorTest {
    @Test
    fun nextIdGeneratesUniqueIds() {
        val generator = SnowflakeIdGenerator
        val id1 = generator.nextId()
        val id2 = generator.nextId()
        assertNotEquals(id1, id2)
    }

    @Test
    fun nextIdThrowsExceptionWhenClockMovesBackwards() {
        val generator = SnowflakeIdGenerator
        val currentTimestamp = System.currentTimeMillis()
        generator::class.java.getDeclaredField("lastTimestamp").apply {
            isAccessible = true
            set(generator, currentTimestamp + 10000)
        }
        assertFailsWith<IllegalStateException> { generator.nextId() }
    }

    @Test
    fun datacenterIdSetterThrowsExceptionForInvalidValue() {
        assertFailsWith<IllegalArgumentException> { SnowflakeIdGenerator.datacenterId = -1 }
        assertFailsWith<IllegalArgumentException> { SnowflakeIdGenerator.datacenterId = 32 }
    }

    @Test
    fun workerIdSetterThrowsExceptionForInvalidValue() {
        assertFailsWith<IllegalArgumentException> { SnowflakeIdGenerator.workerId = -1 }
        assertFailsWith<IllegalArgumentException> { SnowflakeIdGenerator.workerId = 32 }
    }

    @Test
    fun nextIdHandlesSequenceOverflow() {
        val generator = SnowflakeIdGenerator
        generator::class.java.getDeclaredField("sequence").apply {
            isAccessible = true
            set(generator, (1 shl (
                generator::class.java.getDeclaredField("SEQUENCE_BITS").apply {
                    isAccessible = true
                }.getInt(generator)
            )) - 1L)
        }
        generator::class.java.getDeclaredField("lastTimestamp").apply {
            isAccessible = true
            set(generator, System.currentTimeMillis())
        }
        val id = generator.nextId()
        assertTrue(id > 0)
    }
}