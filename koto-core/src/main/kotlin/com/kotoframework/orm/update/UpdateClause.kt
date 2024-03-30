package com.kotoframework.orm.update

import com.kotoframework.interfaces.KPojo
import com.kotoframework.types.KTableConditionalField
import com.kotoframework.types.KTableField

class UpdateClause<T : KPojo>(kPojo: T, fields: KTableField<T, Unit> = null) {
    fun set(lambda: KTableField<T, Unit>): UpdateClause<T> {
        TODO()
    }

    fun by(lambda: KTableField<T, Unit>): UpdateClause<T> {
        TODO()
    }

    fun where(lambda: KTableConditionalField<T, Boolean?> = null): UpdateClause<T> {
        TODO()
    }

    fun execute() {
        TODO()
    }
}