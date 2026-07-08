/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax

/**
 * Structured SQL identifier used when a name needs database/schema/table qualification.
 *
 * Existing syntax nodes may expose simple string fields for ergonomic construction. New code should
 * prefer this type when a name may contain more than one identifier part.
 */
data class SqlIdentifier(
    val parts: List<String>
) : SqlNode {
    init {
        require(parts.isNotEmpty()) { "SQL identifier requires at least one part." }
        require(parts.all { it.isNotBlank() }) { "SQL identifier parts must not be blank." }
    }

    val last: String get() = parts.last()

    val canonical: String get() = parts.joinToString(".")

    companion object {
        fun of(vararg parts: String): SqlIdentifier = SqlIdentifier(parts.toList())

        fun of(parts: Iterable<String>): SqlIdentifier = SqlIdentifier(parts.toList())
    }
}
