package com.kotlinorm.interfaces

import com.kotlinorm.Kronos
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.utils.StaticTypeRef
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class StaticValueCodecPlatformJvmTest {
    @Test
    fun `jvm actual keeps kotlinx datetime compatibility and applies configured zone`() {
        val originalZone = Kronos.timeZone
        try {
            Kronos.timeZone = "Asia/Shanghai"
            val context = StaticValueCodecContext(
                direction = ValueCodecDirection.DECODE,
                origin = ValueCodecOrigin.DATABASE,
                targetType = StaticTypeRef("kotlinx.datetime.LocalDateTime", nullable = false),
                storage = ValueStorage.NONE
            )
            assertEquals(
                LocalDateTime.parse("2026-08-09T18:11:12"),
                KronosValueCodecs.decode(KronosPhysicalValue.Text("2026-08-09T10:11:12Z"), context)
            )

            val legacy = Instant.parse("2026-08-09T10:11:12Z")
            val encoded = KronosValueCodecs.encode(
                legacy,
                context.copy(
                    direction = ValueCodecDirection.ENCODE,
                    origin = ValueCodecOrigin.PARAMETER,
                    targetType = StaticTypeRef("kotlinx.datetime.Instant", nullable = false)
                )
            )
            assertEquals(KronosPhysicalValue.Text(legacy.toString()), encoded)

            val defaultInstant = Instant.parse("2026-08-09T10:11:12Z")
            assertEquals(
                defaultInstant,
                KronosValueCodecs.decode(
                    KronosPhysicalValue.Text(defaultInstant.toString()),
                    context.copy(targetType = StaticTypeRef("kotlin.time.Instant", nullable = false))
                )
            )
        } finally {
            Kronos.timeZone = originalZone
        }
    }
}
