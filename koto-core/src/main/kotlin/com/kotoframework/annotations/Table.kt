package com.kotoframework.annotations

/**
 * Table
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class Table(val name: String)