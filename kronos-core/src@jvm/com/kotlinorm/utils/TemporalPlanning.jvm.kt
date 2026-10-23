package com.kotlinorm.utils
import com.kotlinorm.Kronos
import com.kotlinorm.resolvedTimeZone
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.utils.codec.PreparedValue
import com.kotlinorm.utils.codec.PreparedValueKind
import kotlin.reflect.KType
import kotlin.reflect.typeOf
internal actual fun currentStrategyTime(): PreparedValue = PreparedValue(
    java.time.LocalDateTime.now(java.time.Clock.system(Kronos.resolvedTimeZone)),
    typeOf<java.time.LocalDateTime>(), kind = PreparedValueKind.STRATEGY_TEMPORAL)
internal actual fun platformTemporalTarget(columnType: KColumnType): KType = when(columnType) {
    KColumnType.DATE -> typeOf<java.sql.Date>()
    KColumnType.TIME -> typeOf<java.sql.Time>()
    KColumnType.TIMESTAMP -> typeOf<java.sql.Timestamp>()
    else -> typeOf<java.time.LocalDateTime>()
}
