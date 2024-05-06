package com.kotoframework

import com.kotoframework.adapter.*
import com.kotoframework.enums.KLoggerType
import com.kotoframework.exceptions.KotoNoLoggerException
import com.kotoframework.i18n.Noun
import com.kotoframework.interfaces.KLogger
import com.kotoframework.beans.logging.BundledSimpleLoggerAdapter
import com.kotoframework.utils.DataSourceUtil.javaName
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

    private fun getKotoLoggerInstance(loggingClazz: Any): KLogger {
        val tag = loggingClazz.javaName
        return when (Kronos.defaultLoggerType) {
            KLoggerType.ANDROID_LOGGER -> AndroidUtilLoggerAdapter(tag)
            KLoggerType.COMMONS_LOGGER -> ApacheCommonsLoggerAdapter(tag)
            KLoggerType.JDK_LOGGER -> JavaUtilLoggerAdapter(tag)
            KLoggerType.SLF4J_LOGGER -> Slf4jLoggerAdapter(tag)
            KLoggerType.DEFAULT_LOGGER -> BundledSimpleLoggerAdapter(tag)
            else -> throw KotoNoLoggerException(Noun.noKLoggerMessage)
        }
    }

    fun detectLoggerImplementation() {
        Kronos.defaultLogger = { getKotoLoggerInstance(it) }
        val tag = "com.kotoframework.jdbcWrapper"
        var result: KLogger? = null

        infix fun KLoggerType.detect(init: () -> KLogger) {
            if (result == null) {
                try {
                    result = init()
                    Kronos.defaultLoggerType = this
                } catch (_: ClassNotFoundException) {
                    // ignored
                } catch (_: NoClassDefFoundError) {
                    // ignored
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