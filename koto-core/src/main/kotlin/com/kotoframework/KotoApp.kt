package com.kotoframework

import com.kotoframework.enums.LoggerType
import com.kotoframework.interfaces.KotoJdbcWrapper
import com.kotoframework.interfaces.KotoNamingStrategy

object KotoApp {
    var defaultDataSource: KotoJdbcWrapper? = null
    var defaultNamingStrategy: KotoNamingStrategy? = null
    var defaultLogger: LoggerType? = LoggerType.NONE
}