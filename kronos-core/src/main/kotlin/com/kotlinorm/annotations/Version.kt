package com.kotlinorm.annotations

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Version(val enable: Boolean = true)
