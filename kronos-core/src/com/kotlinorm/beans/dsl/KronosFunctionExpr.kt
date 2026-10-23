/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.dsl

import com.kotlinorm.syntax.expr.SqlExpr

data class KronosFunctionExpr(
    val expr: SqlExpr,
    val functionName: String,
    val alias: String? = null
) {
    fun alias(alias: String): KronosFunctionExpr = copy(alias = alias)
}

