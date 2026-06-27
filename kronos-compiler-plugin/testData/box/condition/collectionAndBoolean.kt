import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_group_user")
data class ConditionGroupUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

fun groupWhere(user: ConditionGroupUser, block: ToFilter<ConditionGroupUser, Boolean?>): Criteria? {
    var result: Criteria? = null
    user.afterFilter {
        criteriaParamMap = user.toDataMap()
        block!!(it)
        result = criteria
    }
    return result
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ConditionGroupUser()
    val ids = [1, 2, 3]
    val inCondition = groupWhere(user) { it.id in ids }?.children?.singleOrNull()
    if (inCondition?.field?.name != "id") return "Fail: in field was ${inCondition?.field?.name}"
    if (inCondition.type != ConditionType.IN) return "Fail: in type was ${inCondition.type}"
    if (inCondition.value != ids) return "Fail: in value was ${inCondition.value}"

    val grouped = groupWhere(user) { (it.name == "Ada") && (it.age > 18) }
    if (grouped?.children?.size != 1) return "Fail: root child size was ${grouped?.children?.size}"
    val andCondition = grouped.children.single()
    if (andCondition?.type != ConditionType.AND) return "Fail: group type was ${andCondition?.type}"
    if (andCondition.children.size != 2) return "Fail: group child size was ${andCondition.children.size}"
    if (andCondition.children[0]?.field?.name != "name") return "Fail: first child was ${andCondition.children[0]?.field?.name}"
    if (andCondition.children[1]?.field?.name != "age") return "Fail: second child was ${andCondition.children[1]?.field?.name}"

    return "OK"
}
