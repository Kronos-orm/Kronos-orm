/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.generator

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KIdGenerator
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PrimaryKeyValueGeneratorTest {

    @Test
    fun `existing primary key value is preserved for every strategy`() {
        val currentValue = Any()

        PrimaryKeyType.entries.forEach { strategy ->
            assertSame(currentValue, field(strategy).resolveGeneratedPrimaryKeyValue(currentValue), strategy.name)
        }
    }

    @Test
    fun `built-in strategies generate UUID and snowflake values`() {
        val uuid = field(PrimaryKeyType.UUID).resolveGeneratedPrimaryKeyValue(null)
        val snowflake = field(PrimaryKeyType.SNOWFLAKE).resolveGeneratedPrimaryKeyValue(null)

        assertTrue(uuid is String)
        assertEquals(uuid, UUID.fromString(uuid).toString())
        assertTrue(snowflake is Long)
        assertTrue(snowflake > 0L)
    }

    @Test
    fun `custom strategy delegates when configured and returns null otherwise`() {
        val previousGenerator = customIdGenerator
        try {
            customIdGenerator = object : KIdGenerator<String> {
                override fun nextId(): String = "custom-primary-key"
            }
            assertEquals(
                "custom-primary-key",
                field(PrimaryKeyType.CUSTOM).resolveGeneratedPrimaryKeyValue(null)
            )

            customIdGenerator = null
            assertNull(field(PrimaryKeyType.CUSTOM).resolveGeneratedPrimaryKeyValue(null))
        } finally {
            customIdGenerator = previousGenerator
        }
    }

    @Test
    fun `non-generating strategies leave a missing value null`() {
        assertEquals(
            listOf(null, null, null),
            listOf(
                field(PrimaryKeyType.NOT).resolveGeneratedPrimaryKeyValue(null),
                field(PrimaryKeyType.DEFAULT).resolveGeneratedPrimaryKeyValue(null),
                field(PrimaryKeyType.IDENTITY).resolveGeneratedPrimaryKeyValue(null)
            )
        )
    }

    private fun field(primaryKeyType: PrimaryKeyType): Field =
        Field(columnName = "id", name = "id", primaryKey = primaryKeyType)
}
