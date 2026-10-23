package com.kotlinorm.interfaces

/** Locale-dependent Java formatter directives have no portable locale implementation. */
internal actual fun formatPlatformSpecificTemporalPattern(value: Any, context: StaticValueCodecContext, pattern: String): String =
    throw context.temporalPatternFailure(pattern)

internal actual fun parsePlatformSpecificTemporalPattern(text: String, context: StaticValueCodecContext, pattern: String): Any =
    throw context.temporalPatternFailure(pattern)
