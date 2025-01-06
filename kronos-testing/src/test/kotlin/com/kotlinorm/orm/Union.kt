package com.kotlinorm.orm

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import org.junit.jupiter.api.Test

class Union {

    @Test
    fun testUnion() {
        union (
            User().select().where(),
            Email().select().where()
        )
    }

}