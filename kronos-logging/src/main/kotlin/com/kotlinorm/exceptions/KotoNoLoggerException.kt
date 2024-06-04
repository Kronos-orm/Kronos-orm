package com.kotlinorm.exceptions


/**
 * Koto No Logger Exception
 *
 * @property message Exception Message
 */
class KotoNoLoggerException(override val message: String) : Exception(message)