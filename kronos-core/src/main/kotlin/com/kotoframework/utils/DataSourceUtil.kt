package com.kotoframework.utils

import com.kotoframework.Kronos
import com.kotoframework.interfaces.KronosDataSourceWrapper

object DataSourceUtil {
    fun KronosDataSourceWrapper?.orDefault(): KronosDataSourceWrapper {
        return this ?: Kronos.defaultDataSource()
    }

    val Any.javaName: String
        get() = this::class.java.name
}