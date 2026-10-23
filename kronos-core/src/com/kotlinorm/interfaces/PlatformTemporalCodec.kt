package com.kotlinorm.interfaces

/**
 * Platform bridge for optional kotlinx-datetime compatibility types.
 *
 * The common codec uses descriptor keys and never links against kotlinx-datetime
 * classes. JVM and Native source sets provide the actual implementation.
 */
internal expect fun encodePlatformTemporal(
    value: Any,
    context: StaticValueCodecContext
): KronosPhysicalValue?

internal expect fun decodePlatformTemporal(
    value: KronosPhysicalValue,
    context: StaticValueCodecContext
): Any?
