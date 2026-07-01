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
 * Binary operators carry precedence so renderers can make parenthesizing decisions from the tree.
 * Higher values bind tighter.
 */
sealed class SqlBinaryOperator(val precedence: Int) : SqlNode {
    object Times : SqlBinaryOperator(60)
    object Div : SqlBinaryOperator(60)
    object Mod : SqlBinaryOperator(60)
    object Plus : SqlBinaryOperator(50)
    object Minus : SqlBinaryOperator(50)
    object Concat : SqlBinaryOperator(40)
    object Equal : SqlBinaryOperator(30)
    object NotEqual : SqlBinaryOperator(30)
    data class IsDistinctFrom(val withNot: Boolean = false) : SqlBinaryOperator(30)
    data class Is(val withNot: Boolean = false) : SqlBinaryOperator(30)
    object GreaterThan : SqlBinaryOperator(30)
    object GreaterThanEqual : SqlBinaryOperator(30)
    object LessThan : SqlBinaryOperator(30)
    object LessThanEqual : SqlBinaryOperator(30)
    object Overlaps : SqlBinaryOperator(30)
    object Regexp : SqlBinaryOperator(30)
    object NotRegexp : SqlBinaryOperator(30)
    object BitwiseAnd : SqlBinaryOperator(55)
    object BitwiseOr : SqlBinaryOperator(45)
    object BitwiseXor : SqlBinaryOperator(45)
    object BitwiseLeftShift : SqlBinaryOperator(55)
    object BitwiseRightShift : SqlBinaryOperator(55)
    object And : SqlBinaryOperator(20)
    object Or : SqlBinaryOperator(10)
    data class UnsafeCustom(val tokens: List<SqlUnsafeToken>) : SqlBinaryOperator(0)
}

sealed interface SqlUnaryOperator : SqlNode {
    object Positive : SqlUnaryOperator

    object Negative : SqlUnaryOperator

    object Not : SqlUnaryOperator

    object BitwiseNot : SqlUnaryOperator

    data class UnsafeCustom(val tokens: List<SqlUnsafeToken>) : SqlUnaryOperator
}

sealed interface SqlQuantifiedComparisonOperator : SqlNode {
    object Equal : SqlQuantifiedComparisonOperator

    object NotEqual : SqlQuantifiedComparisonOperator

    object GreaterThan : SqlQuantifiedComparisonOperator

    object GreaterThanEqual : SqlQuantifiedComparisonOperator

    object LessThan : SqlQuantifiedComparisonOperator

    object LessThanEqual : SqlQuantifiedComparisonOperator

    data class UnsafeCustom(val tokens: List<SqlUnsafeToken>) : SqlQuantifiedComparisonOperator
}
