package com.kotoframework.utils

import com.kotoframework.KotoApp
import com.kotoframework.interfaces.KronosDataSourceWrapper

object DataSourceUtil {
    fun KronosDataSourceWrapper?.orDefault(): KronosDataSourceWrapper {
        return this ?: KotoApp.defaultDataSource()
    }

    val Any.javaName: String
        get() = this::class.java.name
}