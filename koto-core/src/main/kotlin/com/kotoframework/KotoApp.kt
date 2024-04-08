package com.kotoframework

import com.kotoframework.enums.KLoggerType
import com.kotoframework.interfaces.KLogger
import com.kotoframework.interfaces.KotoDataSourceWrapper
import com.kotoframework.interfaces.KotoNamingStrategy
import com.kotoframework.utils.BundledSimpleLoggerAdapter
import com.kotoframework.utils.NoneDataSourceWrapper
import com.kotoframework.utils.NoneNamingStrategy
import kotlin.reflect.full.functions

object KotoApp {
    var defaultDataSource: KotoDataSourceWrapper = NoneDataSourceWrapper()
    var defaultNamingStrategy: KotoNamingStrategy = NoneNamingStrategy()
    var defaultLoggerType: KLoggerType = KLoggerType.NONE
    var defaultLogger: (KotoDataSourceWrapper?) -> KLogger =
        { BundledSimpleLoggerAdapter((it ?: defaultDataSource)::class.java.simpleName) }

    /**
     * detect logger implementation if Koto-logging is used
     */
    private fun detectLoggerImplementation() {
        Class.forName("com.kotoframework.KotoLoggerApp")
            .kotlin.apply {
                functions
                    .firstOrNull { it.name == "detectLoggerImplementation" }
                    ?.call(objectInstance)
            }
    }

    init {
        detectLoggerImplementation()
    }
}