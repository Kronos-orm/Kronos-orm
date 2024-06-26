package com.kotlinorm.orm.relationQuery.oneToOne

import com.kotlinorm.annotations.Reference
import com.kotlinorm.beans.dsl.KPojo

data class Address(
    val id: Long? = null,
    val street: String? = null,
    val city: String? = null,
    @Reference(["person_id"], ["id"], mapperBy = Person::class)
    var person: Person? = null,
    @Reference(["person_id2"], ["id"], mapperBy = Person::class)
    var secondPerson: Person? = null
) : KPojo()