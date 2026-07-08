package com.kotlinorm.integration.profiles

import com.kotlinorm.Kronos

abstract class BaseIntegrationScenarioProfile : IntegrationScenarioProfile

internal val activeDataSource
    get() = Kronos.dataSource()
