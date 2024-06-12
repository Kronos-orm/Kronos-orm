package com.kotlinorm.annotations

/**
 * Created by ousc on 2022/7/20 15:40
 */

/**
 * Unsafe Criteria
 *
 * @constructor Create empty Unsafe Criteria
 * @author ousc
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "This function is cast, please use it carefully.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class UnsafeCriteria(val message: String = "")