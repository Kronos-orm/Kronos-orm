package com.kotlinorm

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsw.NoneDataSourceWrapper
import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter
import com.kotlinorm.beans.logging.KLogMessage.Companion.logMessageOf
import com.kotlinorm.beans.namingStrategy.NoneNamingStrategy
import com.kotlinorm.beans.serializeResolver.NoneSerializeResolver
import com.kotlinorm.enums.ColorPrintCode
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.enums.NoValueStrategy
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.interfaces.KronosNamingStrategy
import com.kotlinorm.interfaces.KronosSerializeResolver
import com.kotlinorm.types.KLoggerFactory
import kotlin.reflect.full.declaredFunctions

object Kronos {
    var defaultLogger: KLoggerFactory =
        { BundledSimpleLoggerAdapter(it::class.simpleName!!) }
    internal var noValueStrategy = NoValueStrategy.Ignore

    var dataSource: () -> KronosDataSourceWrapper = { NoneDataSourceWrapper() }
    var loggerType: KLoggerType = KLoggerType.DEFAULT_LOGGER
    var logPath = listOf("console")
    var serializeResolver: KronosSerializeResolver = NoneSerializeResolver()
    var fieldNamingStrategy: KronosNamingStrategy = NoneNamingStrategy()
    var tableNamingStrategy: KronosNamingStrategy = NoneNamingStrategy()
    var updateTimeStrategy: KronosCommonStrategy = KronosCommonStrategy(false, Field("update_time", "updateTime"))
    var createTimeStrategy: KronosCommonStrategy = KronosCommonStrategy(false, Field("create_time", "createTime"))
    var logicDeleteStrategy: KronosCommonStrategy = KronosCommonStrategy(false, Field("deleted"))

    /**
     * detect logger implementation if kronos-logging is used
     */
    private fun detectLoggerImplementation() {
        try {
            val kronosClass = Class.forName("com.kotlinorm.KronosLoggerApp").kotlin
            kronosClass.declaredFunctions.first { it.name == "detectLoggerImplementation" }
                .call(kronosClass.objectInstance)
        } catch (e: ClassNotFoundException) {
            defaultLogger(this).info(
                logMessageOf("Kronos-logging is not used.", ColorPrintCode.YELLOW.toArray()).endl().toArray()
            )
        }
    }

    fun KLoggerFactory.start() {
        detectLoggerImplementation()
    }

    init {
        defaultLogger(this).info(
            logMessageOf("Kronos ORM Framework started.", ColorPrintCode.GREEN.toArray()).endl().toArray()
        )
    }
}