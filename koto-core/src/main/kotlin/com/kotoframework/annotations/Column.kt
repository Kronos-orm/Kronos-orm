package com.kotoframework.annotations

/**
 * Table
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class Column(
    val name: String
)