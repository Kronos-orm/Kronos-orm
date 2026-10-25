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
import com.kotlinorm.utils.RuntimeReentrantLock

/** Shared logging controls for every Kronos logger, including custom factories. */
class KronosLoggingConfig {
    private val lock = RuntimeReentrantLock()
    private var outputEnabled = true
    private var destinations = arrayOf("console")
    private var adapterType = KLoggerType.DEFAULT_LOGGER
    private var level = KLogLevel.TRACE
    private var customFactory: KLoggerFactory? = null
    /** Disabling output preserves the selected adapter, destinations and factory. */
    var enabled: Boolean
        get() = lock.withLock { outputEnabled }
        set(value) { lock.withLock { outputEnabled = value } }

    /** Bundled destinations copied on assignment/read; `console` selects standard output. */
    var path: Array<String>
        get() = lock.withLock { destinations.copyOf() }
        set(value) { lock.withLock { destinations = value.copyOf() } }

    /** Adapter selection used by kronos-logging after installing its platform integration. */
    var type: KLoggerType
        get() = lock.withLock { adapterType }
        set(value) { lock.withLock { adapterType = value } }

    /** Messages must also pass the adapter's own level checks. */
    var minimumLevel: KLogLevel
        get() = lock.withLock { level }
        set(value) { lock.withLock { level = value } }

    /** Optional custom adapter factory. Set to null to use the selected platform adapter. */
    var factory: KLoggerFactory?
        get() = lock.withLock { customFactory }
        set(value) { lock.withLock { customFactory = value } }

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
        lock.withLock { platformFactory = adapterFactory }
    }

    internal fun accepts(level: KLogLevel): Boolean = lock.withLock { outputEnabled && level >= this.level }

    internal fun resolve(source: Any): KLogger {
        val selected = lock.withLock { customFactory ?: platformFactory }
        // User factories may configure logging or wait for work on another thread.
        return selected(source)
    }
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
