/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.logging

import com.kotlinorm.annotations.InternalKronosApi
import com.kotlinorm.enums.KLogLevel
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.interfaces.KLogger
import com.kotlinorm.types.KLoggerFactory

/** Shared logging controls for every Kronos logger, including custom factories. */
class KronosLoggingConfig {
    /** Disabling output preserves the selected adapter, destinations and factory. */
    var enabled: Boolean = true

    /** Bundled logger destinations; `console` selects standard output. */
    var path: Array<String> = arrayOf("console")

    /** Adapter selection used by kronos-logging after installing its platform integration. */
    var type: KLoggerType = KLoggerType.DEFAULT_LOGGER

    /** Messages must also pass the adapter's own level checks. */
    var minimumLevel: KLogLevel = KLogLevel.TRACE

    /** Optional custom adapter factory. Set to null to use the selected platform adapter. */
    var factory: KLoggerFactory? = null

    private var platformFactory: KLoggerFactory = { source ->
        check(type == KLoggerType.DEFAULT_LOGGER) {
            "Logging adapter $type is not installed. Initialize kronos-logging or configure logging.factory."
        }
        platformDefaultLogger(source)
    }

    /** Returns a logger that observes current controls even after it has been retained. */
    fun logger(source: Any): KLogger = ConfiguredLogger(this, source)

    /** Used by the platform integration without changing the user's custom factory. */
    @InternalKronosApi
    fun installAdapterFactory(adapterFactory: KLoggerFactory) {
        platformFactory = adapterFactory
    }

    internal fun accepts(level: KLogLevel): Boolean = enabled && level >= minimumLevel

    internal fun resolve(source: Any): KLogger = (factory ?: platformFactory)(source)
}

private class ConfiguredLogger(private val config: KronosLoggingConfig, private val source: Any) : KLogger {
    private fun accepted(level: KLogLevel, check: KLogger.() -> Boolean): KLogger? {
        if (!config.accepts(level)) return null
        return config.resolve(source).takeIf(check)
    }

    override fun isTraceEnabled() = accepted(KLogLevel.TRACE, KLogger::isTraceEnabled) != null
    override fun isDebugEnabled() = accepted(KLogLevel.DEBUG, KLogger::isDebugEnabled) != null
    override fun isInfoEnabled() = accepted(KLogLevel.INFO, KLogger::isInfoEnabled) != null
    override fun isWarnEnabled() = accepted(KLogLevel.WARN, KLogger::isWarnEnabled) != null
    override fun isErrorEnabled() = accepted(KLogLevel.ERROR, KLogger::isErrorEnabled) != null

    override fun trace(messages: Array<KLogMessage>, e: Throwable?) {
        accepted(KLogLevel.TRACE, KLogger::isTraceEnabled)?.trace(messages, e)
    }

    override fun debug(messages: Array<KLogMessage>, e: Throwable?) {
        accepted(KLogLevel.DEBUG, KLogger::isDebugEnabled)?.debug(messages, e)
    }

    override fun info(messages: Array<KLogMessage>, e: Throwable?) {
        accepted(KLogLevel.INFO, KLogger::isInfoEnabled)?.info(messages, e)
    }

    override fun warn(messages: Array<KLogMessage>, e: Throwable?) {
        accepted(KLogLevel.WARN, KLogger::isWarnEnabled)?.warn(messages, e)
    }

    override fun error(messages: Array<KLogMessage>, e: Throwable?) {
        accepted(KLogLevel.ERROR, KLogger::isErrorEnabled)?.error(messages, e)
    }
}
