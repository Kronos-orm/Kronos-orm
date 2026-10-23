package com.kotlinorm.functions.bundled.exts
import com.kotlinorm.functions.FunctionHandler
actual object PolymerizationFunctions {
    /** AVG mapped as Double; floating-point precision applies. */
    actual fun FunctionHandler.avgDouble(x: Number?): Double? = null


    actual fun FunctionHandler.count(x: Any?): Long? = null
    actual fun FunctionHandler.sum(x: Byte?): Long? = null
    actual fun FunctionHandler.sum(x: Short?): Long? = null
    actual fun FunctionHandler.sum(x: Int?): Long? = null
    actual fun FunctionHandler.sum(x: Long?): Long? = null
    actual fun FunctionHandler.sum(x: Float?): Double? = null
    actual fun FunctionHandler.sum(x: Double?): Double? = null
    actual fun <T : Comparable<T>> FunctionHandler.max(x: T?): T? = null
    actual fun FunctionHandler.max(x: Any?): Any? = null
    actual fun <T : Comparable<T>> FunctionHandler.min(x: T?): T? = null
    actual fun FunctionHandler.min(x: Any?): Any? = null
    actual fun FunctionHandler.groupConcat(x: Any?): Any? = null
}
