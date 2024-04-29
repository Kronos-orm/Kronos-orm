package com.kotoframework.enums

import com.kotoframework.enums.*

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

val ignore = NoValueStrategy.Ignore
val alwaysFalse = NoValueStrategy.False
val alwaysTrue = NoValueStrategy.True
val judgeNull = NoValueStrategy.JudgeNull
val smart = NoValueStrategy.Smart

//val INNER_JOIN = JoinType.INNER_JOIN
//val LEFT_JOIN = JoinType.LEFT_JOIN
//val RIGHT_JOIN = JoinType.RIGHT_JOIN
//val FULL_JOIN = JoinType.FULL_JOIN