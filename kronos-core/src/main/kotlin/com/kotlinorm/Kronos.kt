package com.kotlinorm

import com.kotlinorm.beans.dsw.NoneDataSourceWrapper
import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter
import com.kotlinorm.beans.logging.KLogMessage.Companion.logMessageOf
import com.kotlinorm.beans.namingStrategy.NoneNamingStrategy
import com.kotlinorm.beans.serializeResolver.NoneSerializeResolver
import com.kotlinorm.enums.ColorPrintCode
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.enums.NoValueStrategy
import com.kotlinorm.interfaces.KLogger
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.interfaces.KronosNamingStrategy
import com.kotlinorm.interfaces.KronosSerializeResolver
import com.kotlinorm.utils.DataSourceUtil.javaName
import kotlin.reflect.full.declaredFunctions

object Kronos {
    var defaultDataSource: () -> KronosDataSourceWrapper = { NoneDataSourceWrapper() }
    var fieldNamingStrategy: KronosNamingStrategy = NoneNamingStrategy()
    var tableNamingStrategy: KronosNamingStrategy = NoneNamingStrategy()
    var defaultLoggerType: KLoggerType = KLoggerType.DEFAULT_LOGGER
    var defaultLogger: (Any) -> KLogger =
        { BundledSimpleLoggerAdapter(it.javaName) }
    var defaultSerializeResolver: KronosSerializeResolver = NoneSerializeResolver()
    internal var defaultNoValueStrategy = com.kotlinorm.enums.NoValueStrategy.Ignore

    /**
     * detect logger implementation if kronos-logging is used
     */
    private fun detectLoggerImplementation() {
        try {
            val kronosClass = Class.forName("com.kotlinorm.KotoLoggerApp").kotlin
            val kotoAppInstance = kronosClass.objectInstance
            kronosClass.declaredFunctions.first { it.name == "detectLoggerImplementation" }.call(kotoAppInstance)
        } catch (e: ClassNotFoundException) {
            defaultLogger(this).info(
                logMessageOf("Kronos-logging is not used.", ColorPrintCode.YELLOW.toArray()).endl().toArray()
            )
        }
    }

    init {
        defaultLogger(this).info(
            logMessageOf("kotlinorm started.", ColorPrintCode.GREEN.toArray()).endl().toArray()
        )
        detectLoggerImplementation()
    }
}