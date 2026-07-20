/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.join

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinGeneratedArityBehaviorTest : MysqlTestBase() {
    @Test
    fun `generated join overloads preserve source order through arity sixteen`() {
        val s1 = JoinArityEntity1(1)
        val s2 = JoinArityEntity2(2)
        val s3 = JoinArityEntity3(3)
        val s4 = JoinArityEntity4(4)
        val s5 = JoinArityEntity5(5)
        val s6 = JoinArityEntity6(6)
        val s7 = JoinArityEntity7(7)
        val s8 = JoinArityEntity8(8)
        val s9 = JoinArityEntity9(9)
        val s10 = JoinArityEntity10(10)
        val s11 = JoinArityEntity11(11)
        val s12 = JoinArityEntity12(12)
        val s13 = JoinArityEntity13(13)
        val s14 = JoinArityEntity14(14)
        val s15 = JoinArityEntity15(15)
        val s16 = JoinArityEntity16(16)

        assertSources(s1.join(s2) { _, _ -> this }, listOf("join_arity_1", "join_arity_2"))

        assertSources(s1.join(s2, s3) { _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3"))

        assertSources(s1.join(s2, s3, s4) { _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4"))

        assertSources(s1.join(s2, s3, s4, s5) { _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5"))

        assertSources(s1.join(s2, s3, s4, s5, s6) { _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6"))

        assertSources(s1.join(s2, s3, s4, s5, s6, s7) { _, _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6", "join_arity_7"))

        assertSources(s1.join(s2, s3, s4, s5, s6, s7, s8) { _, _, _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6", "join_arity_7", "join_arity_8"))

        assertSources(s1.join(s2, s3, s4, s5, s6, s7, s8, s9) { _, _, _, _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6", "join_arity_7", "join_arity_8", "join_arity_9"))

        assertSources(s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10) { _, _, _, _, _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6", "join_arity_7", "join_arity_8", "join_arity_9", "join_arity_10"))

        assertSources(s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11) { _, _, _, _, _, _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6", "join_arity_7", "join_arity_8", "join_arity_9", "join_arity_10", "join_arity_11"))

        assertSources(s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12) { _, _, _, _, _, _, _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6", "join_arity_7", "join_arity_8", "join_arity_9", "join_arity_10", "join_arity_11", "join_arity_12"))

        assertSources(s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13) { _, _, _, _, _, _, _, _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6", "join_arity_7", "join_arity_8", "join_arity_9", "join_arity_10", "join_arity_11", "join_arity_12", "join_arity_13"))

        assertSources(s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14) { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6", "join_arity_7", "join_arity_8", "join_arity_9", "join_arity_10", "join_arity_11", "join_arity_12", "join_arity_13", "join_arity_14"))

        assertSources(s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15) { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6", "join_arity_7", "join_arity_8", "join_arity_9", "join_arity_10", "join_arity_11", "join_arity_12", "join_arity_13", "join_arity_14", "join_arity_15"))

        assertSources(s1.join(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16) { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> this }, listOf("join_arity_1", "join_arity_2", "join_arity_3", "join_arity_4", "join_arity_5", "join_arity_6", "join_arity_7", "join_arity_8", "join_arity_9", "join_arity_10", "join_arity_11", "join_arity_12", "join_arity_13", "join_arity_14", "join_arity_15", "join_arity_16"))

    }

    private fun assertSources(source: JoinSource<*, *>, expectedTables: List<String>) {
        assertEquals(expectedTables, source.joinState.sources.map { it.__tableName })
    }
}

@Table("join_arity_1")
data class JoinArityEntity1(val id: Int? = null) : KPojo

@Table("join_arity_2")
data class JoinArityEntity2(val id: Int? = null) : KPojo

@Table("join_arity_3")
data class JoinArityEntity3(val id: Int? = null) : KPojo

@Table("join_arity_4")
data class JoinArityEntity4(val id: Int? = null) : KPojo

@Table("join_arity_5")
data class JoinArityEntity5(val id: Int? = null) : KPojo

@Table("join_arity_6")
data class JoinArityEntity6(val id: Int? = null) : KPojo

@Table("join_arity_7")
data class JoinArityEntity7(val id: Int? = null) : KPojo

@Table("join_arity_8")
data class JoinArityEntity8(val id: Int? = null) : KPojo

@Table("join_arity_9")
data class JoinArityEntity9(val id: Int? = null) : KPojo

@Table("join_arity_10")
data class JoinArityEntity10(val id: Int? = null) : KPojo

@Table("join_arity_11")
data class JoinArityEntity11(val id: Int? = null) : KPojo

@Table("join_arity_12")
data class JoinArityEntity12(val id: Int? = null) : KPojo

@Table("join_arity_13")
data class JoinArityEntity13(val id: Int? = null) : KPojo

@Table("join_arity_14")
data class JoinArityEntity14(val id: Int? = null) : KPojo

@Table("join_arity_15")
data class JoinArityEntity15(val id: Int? = null) : KPojo

@Table("join_arity_16")
data class JoinArityEntity16(val id: Int? = null) : KPojo
