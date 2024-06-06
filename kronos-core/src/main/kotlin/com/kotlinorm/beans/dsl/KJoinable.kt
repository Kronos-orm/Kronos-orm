package com.kotlinorm.beans.dsl

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.enums.JoinType

/**
 * KJoinable
 *
 * KJoinable are used to construct SQL where clause with join
 *
 * @property tableName The name of the table of the joint table
 * @property joinType The type of the table joint
 * @property condition Conditions for joining tables
 * @property logicDeleteStrategy the logic delete strategy of the joint table
 */
class KJoinable (
    val tableName: String,
    val joinType: JoinType,
    val condition: Criteria?,
    val logicDeleteStrategy: KronosCommonStrategy
)