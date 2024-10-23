package com.kotlinorm.exceptions

import com.kotlinorm.enums.DBType

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/23 14:06
 **/
class UnSupportedFunctionException (dbType: DBType, funcName: String, message: String = "Unsupported function name: $funcName database type: $dbType.") : RuntimeException(message)