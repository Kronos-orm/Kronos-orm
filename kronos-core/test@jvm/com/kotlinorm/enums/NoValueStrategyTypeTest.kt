package com.kotlinorm.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NoValueStrategyTypeTest {
    @Test
    fun `from value maps known wire values and rejects unknown values`() {
        assertEquals(NoValueStrategyType.Ignore, NoValueStrategyType.fromValue("ignore"))
        assertEquals(NoValueStrategyType.False, NoValueStrategyType.fromValue("false"))
        assertEquals(NoValueStrategyType.True, NoValueStrategyType.fromValue("true"))
        assertEquals(NoValueStrategyType.JudgeNull, NoValueStrategyType.fromValue("judgeNull"))
        assertEquals(NoValueStrategyType.Auto, NoValueStrategyType.fromValue("auto"))
        assertEquals(
            "No such value for NoValueStrategyType: missing",
            assertFailsWith<IllegalArgumentException> {
                NoValueStrategyType.fromValue("missing")
            }.message
        )
    }
}
