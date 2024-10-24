package com.kotlinorm.functions

import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.database.SqlManager.quoted
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.UnSupportedFunctionException
import com.kotlinorm.interfaces.FunctionTransformer
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object BasicFunctionTransformer : FunctionTransformer {

    override val supportFunctionNames = listOf(
        "count", "average", "sum", "max", "min",
        "abs", "bin", "ceiling", "exp", "floor",
        "greatest", "least", "ln", "log", "mod",
        "pi", "rand", "round", "sign", "sqrt",
        "truncate", "groupConcat", "ascii", "bitLength", "concat",
        "concatWs", "insert", "findInSet", "lcase", "left",
        "length", "ltrim", "position", "quote", "repeat",
        "reverse", "right", "rtrim", "strcmp", "trim",
        "ucase", "curdate", "curtime", "dateAdd", "dateFormat",
        "dateSub", "dayOfWeek", "dayOfMonth", "dayOfYear", "dayName",
        "fromUnixTime", "hour", "minute", "month", "monthName",
        "now", "quarter", "week", "year", "periodDiff",
        "calculateAge", "aesEncrypt", "aesDecrypt", "decode", "encrypt",
        "encode", "md5", "password", "sha"
    )

    override val supportDatabase = listOf(
        DBType.Mysql,
        DBType.Postgres,
        DBType.SQLite,
        DBType.Oracle,
        DBType.Mssql
    )

    override fun transform(
        field: FunctionField,
        dataSource: KronosDataSourceWrapper,
        showTable: Boolean
    ): String {
        return when (field.functionName) {
            in this.supportFunctionNames -> {
                getFunctionSql(field, dataSource, showTable)
            }
            else -> {
                throw UnSupportedFunctionException(dataSource.dbType, field.functionName)
            }
        }
    }

    private fun getFunctionSql(
        field: FunctionField,
        dataSource: KronosDataSourceWrapper,
        showTable: Boolean
    ): String {
        when (dataSource.dbType) {
            DBType.Mysql, DBType.Postgres, DBType.SQLite, DBType.Oracle, DBType.Mssql -> {
                return "${field.functionName.uppercase()}(${
                    field.fields.joinToString(", ") {
                        it.first?.quoted(
                            dataSource,
                            showTable
                        ) ?: if (it.second is String) "'${it.second}'" else it.second.toString()
                    }
                })${
                    if (field.name.isNotEmpty()) " AS ${field.name}" else ""
                }"
            }

            else -> {
                throw UnSupportedFunctionException(dataSource.dbType, field.functionName)
            }
        }
    }

}