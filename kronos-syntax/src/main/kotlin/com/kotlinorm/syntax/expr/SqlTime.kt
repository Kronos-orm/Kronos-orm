/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.expr

import com.kotlinorm.syntax.SqlNode

enum class SqlTimeUnit {
    Year,
    Month,
    Day,
    Hour,
    Minute,
    Second
}

sealed interface SqlIntervalField : SqlNode {
    data class Single(val unit: SqlTimeUnit) : SqlIntervalField

    data class To(val start: SqlTimeUnit, val end: SqlTimeUnit) : SqlIntervalField
}

sealed interface SqlTimeType : SqlNode {
    data class Timestamp(val mode: SqlTimeZoneMode? = null) : SqlTimeType

    object Date : SqlTimeType

    data class Time(val mode: SqlTimeZoneMode? = null) : SqlTimeType
}

enum class SqlTimeZoneMode {
    WithTimeZone,
    WithoutTimeZone
}
