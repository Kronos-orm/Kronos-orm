package com.kotoframework

import com.kotoframework.beans.dsw.NoneDataSourceWrapper
import com.kotoframework.beans.logging.BundledSimpleLoggerAdapter
import com.kotoframework.beans.logging.KLogMessage.Companion.logMessageOf
import com.kotoframework.beans.namingStrategy.NoneNamingStrategy
import com.kotoframework.beans.serializeResolver.NoneSerializeResolver
import com.kotoframework.enums.ColorPrintCode
import com.kotoframework.enums.KLoggerType
import com.kotoframework.interfaces.KLogger
import com.kotoframework.interfaces.KotoDataSourceWrapper
import com.kotoframework.interfaces.KotoNamingStrategy
import com.kotoframework.interfaces.KotoSerializeResolver
import com.kotoframework.utils.DataSourceUtil.javaName

object KotoApp {
    var defaultDataSource: () -> KotoDataSourceWrapper = { NoneDataSourceWrapper() }
    var fieldNamingStrategy: KotoNamingStrategy = NoneNamingStrategy()
    var tableNamingStrategy: KotoNamingStrategy = NoneNamingStrategy()
    var defaultLoggerType: KLoggerType = KLoggerType.DEFAULT_LOGGER
    var defaultLogger: (Any) -> KLogger =
        { BundledSimpleLoggerAdapter(it.javaName) }
    var defaultSerializeResolver: KotoSerializeResolver = NoneSerializeResolver()

    /**
     * detect logger implementation if Koto-logging is used
     */
    private fun detectLoggerImplementation() {
        try {
            Class.forName("com.kotoframework.KotoLoggerApp").apply {
                getMethod("detectLoggerImplementation", Any::class.java).invoke(this)
            }
        } catch (e: ClassNotFoundException) {
            defaultLogger(this).info(
                logMessageOf("Koto-logging is not used.", ColorPrintCode.YELLOW.toArray()).endl().toArray()
            )
        }
    }

    init {
        defaultLogger(this).info(
            logMessageOf("KotoFramework started.", ColorPrintCode.GREEN.toArray()).endl().toArray()
        )
        detectLoggerImplementation()
    }
}