package com.kotoframework

import com.kotoframework.KotoApp.defaultDataSource
import com.kotoframework.adapter.*
import com.kotoframework.enums.KLoggerType
import com.kotoframework.exceptions.KotoNoLoggerException
import com.kotoframework.i18n.Noun
import com.kotoframework.interfaces.KLogger
import com.kotoframework.interfaces.KotoDataSourceWrapper
import com.kotoframework.utils.BundledSimpleLoggerAdapter
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

object KotoLoggerApp {
    /**
     * Call the [Method.invoke] method, catching [InvocationTargetException], and rethrowing the target exception.
     */
    @Suppress("SwallowedException")
    internal fun Method.invoke0(obj: Any?, vararg args: Any?): Any? {
        try {
            return this.invoke(obj, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    fun getKotoLoggerInstance(kdsWrapper: KotoDataSourceWrapper? = defaultDataSource): KLogger {
        val tag = kdsWrapper!!::class.java.name
        return when (KotoApp.defaultLoggerType) {
            KLoggerType.ANDROID_LOGGER -> AndroidUtilLoggerAdapter(tag)
            KLoggerType.COMMONS_LOGGER -> ApacheCommonsLoggerAdapter(tag)
            KLoggerType.JDK_LOGGER -> JavaUtilLoggerAdapter(tag)
            KLoggerType.SLF4J_LOGGER -> Slf4jLoggerAdapter(tag)
            KLoggerType.DEFAULT_LOGGER -> BundledSimpleLoggerAdapter(tag)
            else -> throw KotoNoLoggerException(Noun.noKLoggerMessage)
        }
    }

    fun detectLoggerImplementation() {
        KotoApp.defaultLogger = { getKotoLoggerInstance(it) }
        val tag = "com.kotoframework.jdbcWrapper"
        var result: KLogger? = null

        infix fun KLoggerType.detect(init: () -> KLogger) {
            if (result == null) {
                try {
                    result = init()
                    KotoApp.defaultLoggerType = this
                } catch (_: ClassNotFoundException) {
                } catch (_: NoClassDefFoundError) {
                }
            }
        }

        KLoggerType.ANDROID_LOGGER detect { AndroidUtilLoggerAdapter(tag) }
        KLoggerType.COMMONS_LOGGER detect { ApacheCommonsLoggerAdapter(tag) }
        KLoggerType.JDK_LOGGER detect { JavaUtilLoggerAdapter(tag) }
        KLoggerType.SLF4J_LOGGER detect { Slf4jLoggerAdapter(tag) }
        KLoggerType.DEFAULT_LOGGER detect { BundledSimpleLoggerAdapter(tag) }
    }
}