package com.kotoframework.utils

import com.kotoframework.KotoApp
import com.kotoframework.interfaces.KotoDataSourceWrapper

object DataSourceUtil {
    fun KotoDataSourceWrapper?.orDefault(): KotoDataSourceWrapper {
        return this ?: KotoApp.defaultDataSource()
    }

    val Any.javaName: String
        get() = this::class.java.name
}