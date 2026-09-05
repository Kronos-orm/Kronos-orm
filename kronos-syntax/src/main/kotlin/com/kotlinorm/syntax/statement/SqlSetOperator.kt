/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.statement

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.quantifier.SqlQuantifier

sealed interface SqlSetOperator : SqlNode {
    val quantifier: SqlQuantifier?

    data class Union(override val quantifier: SqlQuantifier? = null) : SqlSetOperator

    data class Except(override val quantifier: SqlQuantifier? = null) : SqlSetOperator

    data class Intersect(override val quantifier: SqlQuantifier? = null) : SqlSetOperator
}

