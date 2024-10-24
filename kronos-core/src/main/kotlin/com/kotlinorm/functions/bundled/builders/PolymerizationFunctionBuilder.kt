/**
 * Copyright 2022-2024 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.functions.bundled.builders

import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.enums.DBType
import com.kotlinorm.functions.bundled.builders.MathFunctionBuilder.buildFields
import com.kotlinorm.interfaces.FunctionBuilder
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object PolymerizationFunctionBuilder : FunctionBuilder {
    private val all = arrayOf(
        DBType.Mysql, DBType.Postgres, DBType.SQLite, DBType.Oracle, DBType.Mssql
    )

    override val supportFunctionNames: (String) -> Array<DBType> = {
        when (it) {
            /*
            * count
            * 返回某列的行数
            * return the number of rows in a column
            * exp: count(id) => 10
            * Mysql: COUNT(x) SQLite: COUNT(x) Oracle: COUNT(x) Postgres: COUNT(x) Mssql: COUNT(x)
            */
            "count" -> all
            /*
            * sum
            * 返回某列的总和
            * return the sum of a column
            * exp: sum(id) => 55
            * Mysql: SUM(x) SQLite: SUM(x) Oracle: SUM(x) Postgres: SUM(x) Mssql: SUM(x)
            */
            "sum" -> all
            /*
            * avg
            * 返回某列的平均值
            * return the average value of a column
            * exp: avg(id) => 5.5
            * Mysql: AVG(x) SQLite: AVG(x) Oracle: AVG(x) Postgres: AVG(x) Mssql: AVG(x)
            */
            "avg" -> all
            /*
            * max
            * 返回某列的最大值
            * return the maximum value of a column
            * exp: max(id) => 10
            * Mysql: MAX(x) SQLite: MAX(x) Oracle: MAX(x) Postgres: MAX(x) Mssql: MAX(x)
            */
            "max" -> all
            /*
            * min
            * 返回某列的最小值
            * return the minimum value of a column
            * exp: min(id) => 1
            * Mysql: MIN(x) SQLite: MIN(x) Oracle: MIN(x) Postgres: MIN(x) Mssql: MIN(x)
            */
            "min" -> all
            /*
            * groupConcat
            * 返回一组值的连接结果
            * return a concatenated string
            * exp: groupConcat(id) => 1,2,3,4,5,6,7,8,9,10
            * Mysql: GROUP_CONCAT(x) SQLite: GROUP_CONCAT(x) Oracle: GROUP_CONCAT(x) Postgres: STRING_AGG(x) Mssql: STRING_AGG(x)
            */
            "groupConcat" -> all
            else -> emptyArray()
        }
    }

    override fun transform(field: FunctionField, dataSource: KronosDataSourceWrapper, showTable: Boolean): String {
        field.functionName = when (field.functionName) {
            "groupConcat" -> {
                when (dataSource.dbType) {
                    DBType.Postgres, DBType.Mssql -> "STRING_AGG"
                    else -> "GROUP_CONCAT"
                }
            }

            else -> field.functionName.uppercase()
        }
        return buildFields(field.functionName, field.name, field.fields, dataSource, showTable)
    }
}