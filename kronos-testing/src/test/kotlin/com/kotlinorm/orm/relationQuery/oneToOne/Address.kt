package com.kotlinorm.orm.relationQuery.oneToOne

import com.kotlinorm.annotations.Reference
import com.kotlinorm.beans.dsl.KPojo

data class Address(
    val id: Long? = null,
    val street: String? = null,
    val city: String? = null,
    @Reference(["person_id"], ["id"], mapperBy = ["person.family_address"])
    var person: Person? = null,
    @Reference(["person_id2"], ["id"], mapperBy = ["person.mailing_address"])
    var secondPerson: Person? = null
) : KPojo()