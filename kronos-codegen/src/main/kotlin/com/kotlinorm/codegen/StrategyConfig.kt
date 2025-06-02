package com.kotlinorm.codegen

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.interfaces.KronosNamingStrategy

class StrategyConfig(
    var tableNamingStrategy: KronosNamingStrategy?,
    var fieldNamingStrategy: KronosNamingStrategy?,
    var createTimeStrategy: KronosCommonStrategy?,
    var updateTimeStrategy: KronosCommonStrategy?,
    var logicDeleteStrategy: KronosCommonStrategy?,
    var optimisticLockStrategy: KronosCommonStrategy?,
    var primaryKeyStrategy: KronosCommonStrategy?
)