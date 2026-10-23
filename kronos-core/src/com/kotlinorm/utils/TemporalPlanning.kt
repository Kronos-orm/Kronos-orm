package com.kotlinorm.utils
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.utils.codec.PreparedValue
import kotlin.reflect.KType
internal expect fun currentStrategyTime(): PreparedValue
internal expect fun platformTemporalTarget(columnType: KColumnType): KType
