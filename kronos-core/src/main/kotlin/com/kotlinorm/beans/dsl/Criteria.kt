package com.kotlinorm.beans.dsl

import com.kotlinorm.enums.ConditionType
import com.kotlinorm.enums.ConditionType.Companion.And
import com.kotlinorm.enums.ConditionType.Companion.IsNull
import com.kotlinorm.enums.ConditionType.Companion.Or
import com.kotlinorm.enums.ConditionType.Companion.Root
import com.kotlinorm.enums.NoValueStrategy
import com.kotlinorm.enums.smart

/**
 * Criteria
 *
 * Criteria are used to construct SQL where clause
 *
 * @property field the field for the condition
 * @property type the type of the condition
 * @property not whether the condition is not
 * @property value the value
 * @property tableName the table name
 * @property noValueStrategy when the value is null, whether to generate sql
 * @property children the children criteria
 */
class Criteria(
    var field: Field = Field("", ""), // original parameter name
    var type: ConditionType, // condition type
    var not: Boolean = false, // whether the condition is not
    var value: Any? = null, // value
    val tableName: String? = "", // table name
    var noValueStrategy: NoValueStrategy = smart, // when the value is null, whether to generate sql,
    var children: MutableList<Criteria?> = mutableListOf()
) {

    internal val valueAcceptable: Boolean
        get() = type != IsNull && type != And && type != Or && type != Root

    /**
     * Adds a child criteria to the list of children.
     *
     * @param criteria the criteria to be added as a child, or null to remove the last child
     */
    fun addChild(criteria: Criteria?) {
        children.add(criteria)
    }
}