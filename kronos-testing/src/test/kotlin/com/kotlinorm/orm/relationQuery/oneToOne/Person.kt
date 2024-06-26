package com.kotlinorm.orm.relationQuery.oneToOne

import com.kotlinorm.beans.dsl.KPojo

data class Person(
    val id: Long? = null,
    val name: String? = null,
    val familyAddress: Address? = null,
    val mailingAddress: Address? = null
) : KPojo()
