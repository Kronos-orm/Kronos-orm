package com.kotlinorm.utils
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.utils.codec.PreparedValue
import com.kotlinorm.utils.codec.PreparedValueKind
import kotlin.reflect.KType
import kotlin.reflect.typeOf
internal actual fun currentStrategyTime(): PreparedValue = PreparedValue(
    kotlin.time.Clock.System.now(), typeOf<kotlin.time.Instant>(), kind = PreparedValueKind.STRATEGY_TEMPORAL)
internal actual fun platformTemporalTarget(columnType: KColumnType): KType = typeOf<String>()
