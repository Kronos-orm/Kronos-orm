package com.kotlinorm.orm.database

import com.kotlinorm.interfaces.KronosDataSourceWrapper

val KronosDataSourceWrapper.table get() = TableOperation(this)
val (()->KronosDataSourceWrapper).table get() = TableOperation(this())