package com.kotlinorm.functions.bundled.exts
import com.kotlinorm.functions.FunctionHandler
expect object PolymerizationFunctions {
    /** AVG mapped as Double; floating-point precision applies. */
    fun FunctionHandler.avgDouble(x: Number?): Double?


    fun FunctionHandler.count(x: Any?): Long?
    fun FunctionHandler.sum(x: Byte?): Long?
    fun FunctionHandler.sum(x: Short?): Long?
    fun FunctionHandler.sum(x: Int?): Long?
    fun FunctionHandler.sum(x: Long?): Long?
    fun FunctionHandler.sum(x: Float?): Double?
    fun FunctionHandler.sum(x: Double?): Double?
    fun <T : Comparable<T>> FunctionHandler.max(x: T?): T?
    fun FunctionHandler.max(x: Any?): Any?
    fun <T : Comparable<T>> FunctionHandler.min(x: T?): T?
    fun FunctionHandler.min(x: Any?): Any?
    fun FunctionHandler.groupConcat(x: Any?): Any?
}
