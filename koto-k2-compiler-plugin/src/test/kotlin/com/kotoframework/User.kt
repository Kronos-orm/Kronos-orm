package com.kotoframework

import com.kotoframework.KotoApp
import com.kotoframework.annotations.Table
import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
import com.kotoframework.interfaces.KPojo
import com.kotoframework.orm.update.update

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null
) : KPojo

fun main() {
    KotoApp.apply {
        fieldNamingStrategy = LineHumpNamingStrategy()
        tableNamingStrategy = LineHumpNamingStrategy()
    }

    val user = User(1)
    val testUser = User(1, "test")

    testUser.update { it.id + it.username }
        .where { it.id < 1 }
        .execute()
}