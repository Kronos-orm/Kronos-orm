package com.kotlinorm.kronosWrapper.jdbcDriverWrapper

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KPojo

@Table("mysql_user")
data class MysqlUser(
    @PrimaryKey(identity = true)
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null,
) : KPojo