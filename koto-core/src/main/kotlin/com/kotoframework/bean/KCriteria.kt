package com.kotoframework.bean

import com.kotoframework.enums.*

class KCriteria(
    var parameterName: String = "", // original parameter name
    var type: ConditionType, // condition type
    var not: Boolean = false, // whether the condition is not
    var value: Any? = null, // value
    val tableName: String? = "", // table name
    var pos: MatchPosition? = MatchPosition.Never, // like position
    var sql: String = "", // sql
    private var noValueStrategy: NoValueStrategy = smart, // when the value is null, whether to generate sql
) {
    init {
        if (type != ConditionType.EQUAL && noValueStrategy == ignore) {
            noValueStrategy = smart
        }
    }

    internal val valueAcceptable: Boolean
        get() = type != ISNULL && type != SQL && type != AND && type != OR

    private var children: MutableList<KCriteria?> = mutableListOf()
    fun addCriteria(criteria: KCriteria?) {
        children.add(criteria)
    }
}