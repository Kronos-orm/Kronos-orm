package com.kotlinorm.interfaces

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.NoValueStrategyType

interface NoValueStrategy {
    fun ifNoValue(kOperateType: KOperationType, criteria: Criteria): NoValueStrategyType
}