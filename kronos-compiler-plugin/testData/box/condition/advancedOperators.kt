import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.enums.NoValueStrategyType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_advanced")
data class AdvancedConditionUser(
    var id: Int? = null,
    @Column("user_name")
    var name: String? = null,
    var age: Int? = null,
    var status: String? = null,
) : KPojo

fun advancedWhere(user: AdvancedConditionUser, block: ToFilter<AdvancedConditionUser, Boolean?>): Criteria? {
    var result: Criteria? = null
    user.afterFilter {
        criteriaParamMap = user.toDataMap()
        block!!(it)
        result = criteria?.children?.singleOrNull()
    }
    return result
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = AdvancedConditionUser(name = "Ada", age = 36, status = "^A.*")

    val between = advancedWhere(user) { it.age between 18..40 }
    if (between?.type != ConditionType.BETWEEN) return "Fail: between type was ${between?.type}"
    if (between.field.name != "age") return "Fail: between field was ${between.field.name}"

    val regexp = advancedWhere(user) { it.status.regexp }
    if (regexp?.type != ConditionType.REGEXP) return "Fail: regexp type was ${regexp?.type}"
    if (regexp.not) return "Fail: regexp should not be negated"

    val noValue = advancedWhere(user) { it.name.eq.ifNoValue(NoValueStrategyType.Ignore) }
    if (noValue?.noValueStrategyType != NoValueStrategyType.Ignore) {
        return "Fail: noValue strategy was ${noValue?.noValueStrategyType}"
    }

    val customColumn = advancedWhere(user) { it.name.startsWith("A") }
    if (customColumn?.field?.columnName != "user_name") return "Fail: columnName was ${customColumn?.field?.columnName}"
    if (customColumn.value != "A%") return "Fail: startsWith value was ${customColumn.value}"

    return "OK"
}
