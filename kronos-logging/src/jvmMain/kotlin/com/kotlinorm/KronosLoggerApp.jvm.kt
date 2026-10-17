/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm

import com.kotlinorm.adapter.AndroidUtilLoggerAdapter
import com.kotlinorm.adapter.ApacheCommonsLoggerAdapter
import com.kotlinorm.adapter.JavaUtilLoggerAdapter
import com.kotlinorm.adapter.Slf4jLoggerAdapter
import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.exceptions.KotoNoLoggerException
import com.kotlinorm.i18n.Noun
import com.kotlinorm.interfaces.KLogger

internal actual fun installPlatformLogger() {
    // NONE denotes an explicitly configured factory. Preserve that choice.
    if (Kronos.loggerType == KLoggerType.NONE) return

    val tag = "com.kotlinorm.core"
    var result: KLogger? = null
    var detectedType: KLoggerType? = null

    infix fun KLoggerType.detect(init: () -> KLogger) {
        if (result == null) {
            try {
                result = init()
                detectedType = this
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

    if (result != null && detectedType != null) {
        Kronos.defaultLogger = { getKotoLoggerInstance(it) }
        Kronos.loggerType = detectedType
    }
}

private fun getKotoLoggerInstance(loggingClazz: Any): KLogger {
    val tag = loggingClazz as? String ?: loggingClazz::class.simpleName!!

    return when (Kronos.loggerType) {
        KLoggerType.ANDROID_LOGGER -> AndroidUtilLoggerAdapter(tag)
        KLoggerType.COMMONS_LOGGER -> ApacheCommonsLoggerAdapter(tag)
        KLoggerType.JDK_LOGGER -> JavaUtilLoggerAdapter(tag)
        KLoggerType.SLF4J_LOGGER -> Slf4jLoggerAdapter(tag)
        KLoggerType.DEFAULT_LOGGER -> BundledSimpleLoggerAdapter(tag)
        else -> throw KotoNoLoggerException(Noun.noKLoggerMessage)
    }
}
