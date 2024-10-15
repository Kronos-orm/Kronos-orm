package com.kotlinorm.beans.strategies

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.enums.ConditionType.Companion.Between
import com.kotlinorm.enums.ConditionType.Companion.Equal
import com.kotlinorm.enums.ConditionType.Companion.Gt
import com.kotlinorm.enums.ConditionType.Companion.Ge
import com.kotlinorm.enums.ConditionType.Companion.Lt
import com.kotlinorm.enums.ConditionType.Companion.Le
import com.kotlinorm.enums.ConditionType.Companion.In
import com.kotlinorm.enums.ConditionType.Companion.Like
import com.kotlinorm.enums.ConditionType.Companion.Regexp
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.KOperationType.UPDATE
import com.kotlinorm.enums.KOperationType.DELETE
import com.kotlinorm.enums.NoValueStrategyType
import com.kotlinorm.enums.NoValueStrategyType.JudgeNull
import com.kotlinorm.enums.NoValueStrategyType.Ignore
import com.kotlinorm.enums.NoValueStrategyType.False
import com.kotlinorm.interfaces.NoValueStrategy
import com.kotlinorm.utils.ConditionSqlBuilder.isEmptyArrayOrCollection

object DefaultNoValueStrategy : NoValueStrategy {
    override fun ifNoValue(kOperateType: KOperationType, criteria: Criteria): NoValueStrategyType {
        return when (kOperateType) {
            UPDATE, DELETE -> when (criteria.type) {
                Equal -> JudgeNull
                Like, In, Between, Regexp -> NoValueStrategyType.fromValue((criteria.not).toString())
                Gt, Ge, Lt, Le -> False
                else -> Ignore
            }

            else ->
                if (criteria.type == In && criteria.value.isEmptyArrayOrCollection())
                    NoValueStrategyType.fromValue((criteria.not).toString())
                else
                    Ignore
        }
    }
}