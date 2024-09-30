package com.kotlinorm.exceptions

import com.kotlinorm.enums.DBType

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/11 13:50
 **/
class UnsupportedDatabaseTypeException(dbType: DBType, message: String = "Unsupported database type: $dbType.") : RuntimeException(message)