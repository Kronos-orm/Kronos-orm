package com.kotlinorm
internal val Kronos.resolvedTimeZone: java.time.ZoneId
    get() = timeZone?.let(java.time.ZoneId::of) ?: java.time.ZoneId.systemDefault()
