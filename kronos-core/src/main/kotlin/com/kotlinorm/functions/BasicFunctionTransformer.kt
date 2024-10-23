package com.kotlinorm.functions

import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.database.SqlManager.quoted
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.UnSupportedFunctionException
import com.kotlinorm.interfaces.FunctionTransformer
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object BasicFunctionTransformer : FunctionTransformer {

    override val supportFunctionNames = listOf(
        "count", "average", "sum", "max", "min"
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
        if (field.fields.firstOrNull()?.first == null) throw IllegalArgumentException("The ${field.functionName} function needs to accept a column as an argument")
        when (field.functionName) {
            "count", "average", "sum", "max", "min" -> {
                when (dataSource.dbType) {
                    DBType.Mysql, DBType.Postgres, DBType.SQLite, DBType.Oracle, DBType.Mssql -> {
                        return "${field.functionName.uppercase()}(${
                            field.fields.first().first!!.quoted(dataSource, showTable)
                        })${
                            if (field.name.isNotEmpty()) " AS ${field.name}" else ""
                        }"
                    }

                    else -> {
                        throw UnSupportedFunctionException(dataSource.dbType, field.functionName)
                    }
                }
            }

            else -> {
                throw UnSupportedFunctionException(dataSource.dbType, field.functionName)
            }
        }
    }

}