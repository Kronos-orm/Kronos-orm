package com.kotlinorm.utils

import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.interfaces.KPojo
import java.util.*

/**
 * Created by ousc on 2022/4/18 17:54
 */
data class TbUser(
    val id: Int? = null,
    val userName: String? = null,
    val password: String? = null,
    val nickname: String? = null,
    @Column("phone_number") val telephone: String? = null,
    @Column("email_address") val email: String? = null,
    @Column("birthday") val birthday: String? = null,
    val sex: String? = null,
    val age: Int? = null,
    val avatar: String? = null,
    @UpdateTime()
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val updateTime: Date? = null,
) : KPojo

data class Ha(
    val id: Int? = null
) : KPojo