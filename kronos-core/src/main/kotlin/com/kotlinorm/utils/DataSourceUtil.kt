package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object DataSourceUtil {
    fun KronosDataSourceWrapper?.orDefault(): KronosDataSourceWrapper {
        return this ?: Kronos.defaultDataSource()
    }

    val Any.javaName: String
        get() = this::class.java.name
}