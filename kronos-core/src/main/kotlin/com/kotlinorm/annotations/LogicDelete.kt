package com.kotlinorm.annotations

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogicDelete(val enable: Boolean = true)