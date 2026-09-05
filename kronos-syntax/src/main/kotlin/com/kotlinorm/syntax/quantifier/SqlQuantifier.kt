/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.quantifier

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.token.SqlUnsafeToken

/**
 * SQL quantifier used by SELECT, GROUP BY, aggregate functions, and set operators.
 */
sealed interface SqlQuantifier : SqlNode {
    object All : SqlQuantifier

    object Distinct : SqlQuantifier

    data class UnsafeCustom(val tokens: List<SqlUnsafeToken>) : SqlQuantifier
}
