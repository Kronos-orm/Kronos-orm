package com.kotoframework

import com.kotoframework.annotations.Table
import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
import com.kotoframework.interfaces.KPojo
import com.kotoframework.orm.delete.delete

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null
) : KPojo

fun main() {
    Kronos.apply {
        fieldNamingStrategy = LineHumpNamingStrategy
        tableNamingStrategy = LineHumpNamingStrategy
    }

    val user = User(1)
    val testUser = User(1, "test")


    val (sql, paramMap) = user.delete().by { it.id }.build()
}