import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.beans.dsl.Field
import com.kotoframework.enums.AND
import com.kotoframework.enums.ConditionType
import com.kotoframework.enums.OR

infix fun Field.eq(value: Any?): Criteria {
    return Criteria(this, ConditionType.EQUAL, false, value)
}

infix fun Field.notEq(value: Any?): Criteria {
    return Criteria(this, ConditionType.EQUAL, true, value)
}

infix fun Field.like(value: Any?): Criteria {
    return Criteria(this, ConditionType.LIKE, false, value)
}

infix fun Field.notLike(value: Any?): Criteria {
    return Criteria(this, ConditionType.LIKE, true, value)
}

infix fun Field.gt(value: Any?): Criteria {
    return Criteria(this, ConditionType.GT, false, value)
}

infix fun Field.ge(value: Any?): Criteria {
    return Criteria(this, ConditionType.GE, false, value)
}

infix fun Field.lt(value: Any?): Criteria {
    return Criteria(this, ConditionType.LT, false, value)
}

infix fun Field.le(value: Any?): Criteria {
    return Criteria(this, ConditionType.LE, false, value)
}

infix fun Field.between(value: ClosedRange<*>): Criteria {
    return Criteria(this, ConditionType.BETWEEN, false, value)
}

infix fun Field.notBetween(value: ClosedRange<*>): Criteria {
    return Criteria(this, ConditionType.BETWEEN, true, value)
}

infix fun Field.isIn(value: Collection<*>): Criteria {
    return Criteria(this, ConditionType.IN, false, value)
}

fun Field.isIn(vararg values: Any): Criteria {
    return Criteria(this, ConditionType.IN, false, values)
}

infix fun Field.notIn(value: Collection<*>): Criteria {
    return Criteria(this, ConditionType.IN, true, value)
}

fun Field.notIn(vararg values: Any): Criteria {
    return Criteria(this, ConditionType.IN, true, values)
}

infix fun Field.sql(sql: String): Criteria {
    return Criteria(this, ConditionType.SQL, false, sql)
}

fun Field.isNull(): Criteria {
    return Criteria(this, ConditionType.ISNULL, false, null)
}

fun Field.isNotNull(): Criteria {
    return Criteria(this, ConditionType.ISNULL, true, null)
}

infix fun Criteria.and(criteria: Criteria): Criteria {
    return Criteria(type = AND, children = mutableListOf(this, criteria))
}

infix fun Criteria.or(criteria: Criteria): Criteria {
    return Criteria(type = OR, children = mutableListOf(this, criteria))
}

infix fun String.eq(value: Any?): Criteria {
    return Criteria(Field(this), ConditionType.EQUAL, false, value)
}

infix fun String.notEq(value: Any?): Criteria {
    return Criteria(Field(this), ConditionType.EQUAL, true, value)
}

infix fun String.like(value: Any?): Criteria {
    return Criteria(Field(this), ConditionType.LIKE, false, value)
}

infix fun String.notLike(value: Any?): Criteria {
    return Criteria(Field(this), ConditionType.LIKE, true, value)
}

infix fun String.gt(value: Any?): Criteria {
    return Criteria(Field(this), ConditionType.GT, false, value)
}

infix fun String.ge(value: Any?): Criteria {
    return Criteria(Field(this), ConditionType.GE, false, value)
}

infix fun String.lt(value: Any?): Criteria {
    return Criteria(Field(this), ConditionType.LT, false, value)
}

infix fun String.le(value: Any?): Criteria {
    return Criteria(Field(this), ConditionType.LE, false, value)
}

infix fun String.between(value: ClosedRange<*>): Criteria {
    return Criteria(Field(this), ConditionType.BETWEEN, false, value)
}

infix fun String.notBetween(value: ClosedRange<*>): Criteria {
    return Criteria(Field(this), ConditionType.BETWEEN, true, value)
}

infix fun String.isIn(value: Collection<*>?): Criteria {
    return Criteria(Field(this), ConditionType.IN, false, value)
}

fun String.isIn(vararg values: Any): Criteria {
    return Criteria(Field(this), ConditionType.IN, false, values)
}

infix fun String.notIn(value: Collection<*>?): Criteria {
    return Criteria(Field(this), ConditionType.IN, true, value)
}

fun String.notIn(vararg values: Any): Criteria {
    return Criteria(Field(this), ConditionType.IN, true, values)
}

infix fun String.sql(sql: String): Criteria {
    return Criteria(Field(this), ConditionType.SQL, false, sql)
}

fun String.isNull(): Criteria {
    return Criteria(Field(this), ConditionType.ISNULL, false, null)
}

fun String.isNotNull(): Criteria {
    return Criteria(Field(this), ConditionType.ISNULL, true, null)
}