package com.kotlinorm.exceptions

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/11 13:50
 **/
class UnsupportedDatabaseTypeException(message: String = "Unsupported database type.") : RuntimeException(message)