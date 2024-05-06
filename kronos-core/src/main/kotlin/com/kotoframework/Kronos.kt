package com.kotoframework

import com.kotoframework.beans.dsw.NoneDataSourceWrapper
import com.kotoframework.beans.logging.BundledSimpleLoggerAdapter
import com.kotoframework.beans.logging.KLogMessage.Companion.logMessageOf
import com.kotoframework.beans.namingStrategy.NoneNamingStrategy
import com.kotoframework.beans.serializeResolver.NoneSerializeResolver
import com.kotoframework.enums.ColorPrintCode
import com.kotoframework.enums.KLoggerType
import com.kotoframework.enums.NoValueStrategy
import com.kotoframework.interfaces.KLogger
import com.kotoframework.interfaces.KronosDataSourceWrapper
import com.kotoframework.interfaces.KronosNamingStrategy
import com.kotoframework.interfaces.KronosSerializeResolver
import com.kotoframework.utils.DataSourceUtil.javaName
import kotlin.reflect.full.declaredFunctions

object Kronos {
    var defaultDataSource: () -> KronosDataSourceWrapper = { NoneDataSourceWrapper() }
    var fieldNamingStrategy: KronosNamingStrategy = NoneNamingStrategy()
    var tableNamingStrategy: KronosNamingStrategy = NoneNamingStrategy()
    var defaultLoggerType: KLoggerType = KLoggerType.DEFAULT_LOGGER
    var defaultLogger: (Any) -> KLogger =
        { BundledSimpleLoggerAdapter(it.javaName) }
    var defaultSerializeResolver: KronosSerializeResolver = NoneSerializeResolver()
    internal var defaultNoValueStrategy = NoValueStrategy.Ignore

    /**
     * detect logger implementation if kronos-logging is used
     */
    private fun detectLoggerImplementation() {
        try {
            val kronosClass = Class.forName("com.kotoframework.KotoLoggerApp").kotlin
            val kotoAppInstance = kronosClass.objectInstance
            kronosClass.declaredFunctions.first { it.name == "detectLoggerImplementation" }.call(kotoAppInstance)
        } catch (e: Exception) {
            if (e is ClassNotFoundException) {
                defaultLogger(this).info(
                    logMessageOf("Kronos-logging is not used.", ColorPrintCode.YELLOW.toArray()).endl().toArray()
                )
            } else {
                throw e
            }
        }
    }

    init {
        defaultLogger(this).info(
            logMessageOf("KotoFramework started.", ColorPrintCode.GREEN.toArray()).endl().toArray()
        )
        detectLoggerImplementation()
    }
}