import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_user")
data class ConditionUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo {
    fun column(name: String): Field {
        return kronosColumns().single { it.name == name }
    }
}

fun conditionUserWhere(user: ConditionUser, block: ToFilter<ConditionUser, Boolean?>): Criteria? {
    var result: Criteria? = null
    user.afterFilter {
        criteriaParamMap = user.toDataMap()
        block!!(it)
        result = criteria?.children?.singleOrNull()
    }
    return result
}

fun assertCondition(
    actual: Criteria?,
    fieldName: String,
    type: ConditionType,
    value: Any?,
    not: Boolean = false,
): String? {
    return when {
        actual == null -> "condition was null"
        actual.field.name != fieldName -> "field was ${actual.field.name}"
        actual.type != type -> "type was ${actual.type}"
        actual.not != not -> "not was ${actual.not}"
        actual.value != value -> "value was ${actual.value}"
        else -> null
    }
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ConditionUser(name = "Ada", age = 36)

    fun checkCondition(
        label: String,
        actual: Criteria?,
        fieldName: String,
        type: ConditionType,
        value: Any?,
        not: Boolean = false,
    ): String? {
        val failure = assertCondition(actual, fieldName, type, value, not)
        return if (failure == null) null else "Fail: $label $failure"
    }

    val checks = listOfNotNull(
        checkCondition(
            "equality",
            conditionUserWhere(user) { it.name == "Ada" },
            "name",
            ConditionType.EQUAL,
            "Ada",
        ),
        checkCondition(
            "greater-or-equal",
            conditionUserWhere(user) { it.age >= 18 },
            "age",
            ConditionType.GE,
            18,
        ),
        checkCondition(
            "notLike",
            conditionUserWhere(user) { it.name notLike "%bot%" },
            "name",
            ConditionType.LIKE,
            "%bot%",
            not = true,
        ),
    )

    return checks.firstOrNull() ?: "OK"
}
