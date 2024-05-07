package com.kotlinorm.annotations

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CreateTime(val format: String = "" , val enable: Boolean = true)