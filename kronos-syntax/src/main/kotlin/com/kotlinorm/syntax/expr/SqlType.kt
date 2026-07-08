/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.expr

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.token.SqlUnsafeToken

/**
 * SQL type syntax modelled after sqala's SqlType tree.
 */
sealed interface SqlType : SqlNode {
    data class Varchar(val maxLength: kotlin.Int? = null) : SqlType

    object Int : SqlType

    object Long : SqlType

    object Float : SqlType

    object Double : SqlType

    data class Decimal(val precision: Pair<kotlin.Int, kotlin.Int>? = null) : SqlType

    object Date : SqlType

    data class Timestamp(val mode: SqlTimeZoneMode? = null) : SqlType

    data class Time(val mode: SqlTimeZoneMode? = null) : SqlType

    object Json : SqlType

    object Boolean : SqlType

    object Interval : SqlType

    object Geometry : SqlType

    object Point : SqlType

    object LineString : SqlType

    object Polygon : SqlType

    object MultiPoint : SqlType

    object MultiLineString : SqlType

    object MultiPolygon : SqlType

    object GeometryCollection : SqlType

    data class Array(val type: SqlType) : SqlType

    /**
     * Structured escape hatch for dialect type names not covered by standard SQL.
     */
    data class Named(
        val name: String,
        val arguments: List<kotlin.Int> = emptyList()
    ) : SqlType

    data class UnsafeCustom(val tokens: List<SqlUnsafeToken>) : SqlType
}
