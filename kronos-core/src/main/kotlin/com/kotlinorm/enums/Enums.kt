package com.kotlinorm.enums

import com.kotlinorm.enums.*

/**
 * Created by ousc on 2022/4/18 10:49
 */

val Root = ConditionType.ROOT
val Like = ConditionType.LIKE
val Equal = ConditionType.EQUAL
val In = ConditionType.IN
val ISNULL = ConditionType.ISNULL
val SQL = ConditionType.SQL
val GT = ConditionType.GT
val GE = ConditionType.GE
val LT = ConditionType.LT
val LE = ConditionType.LE
val BETWEEN = ConditionType.BETWEEN
val AND = ConditionType.AND
val OR = ConditionType.OR

val ASC = SortType.ASC
val DESC = SortType.DESC
//
//val MySql = DBType.MySql
//val Oracle = DBType.Oracle
//val MSSql = DBType.MSSql
//val PostgreSQL = DBType.PostgreSQL
//val SQLite = DBType.SQLite
//val OceanBase = DBType.OceanBase

val ignore = com.kotlinorm.enums.NoValueStrategy.Ignore
val alwaysFalse = com.kotlinorm.enums.NoValueStrategy.False
val alwaysTrue = com.kotlinorm.enums.NoValueStrategy.True
val judgeNull = com.kotlinorm.enums.NoValueStrategy.JudgeNull
val smart = com.kotlinorm.enums.NoValueStrategy.Smart

//val INNER_JOIN = JoinType.INNER_JOIN
//val LEFT_JOIN = JoinType.LEFT_JOIN
//val RIGHT_JOIN = JoinType.RIGHT_JOIN
//val FULL_JOIN = JoinType.FULL_JOIN