package com.kotlinorm.beans.dsl

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.enums.JoinType

class KJoinable(
    val tableName: String,
    val joinType: JoinType,
    val condition: Criteria?,
    val logicDeleteStrategy: KronosCommonStrategy
)