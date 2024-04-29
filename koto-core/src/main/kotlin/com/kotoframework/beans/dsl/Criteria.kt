package com.kotoframework.beans.dsl

import com.kotoframework.enums.*

class Criteria(
    var field: Field = Field("", ""), // original parameter name
    var type: ConditionType, // condition type
    var not: Boolean = false, // whether the condition is not
    var value: Any? = null, // value
    val tableName: String? = "", // table name
    var noValueStrategy: NoValueStrategy = smart, // when the value is null, whether to generate sql,
    var children: MutableList<Criteria?> = mutableListOf()
) {
    init {
        if (type != ConditionType.EQUAL && noValueStrategy == ignore) {
            noValueStrategy = smart
        }
    }

    internal val valueAcceptable: Boolean
        get() = type != ISNULL && type != SQL && type != AND && type != OR

    fun addChild(criteria: Criteria?) {
        children.add(criteria)
    }

    override fun toString(): String {
        return "Criteria(field='$field', type=$type, not=$not, value=$value, tableName=$tableName, noValueStrategy=$noValueStrategy, children=$children, valueAcceptable=$valueAcceptable)"
    }

}