import com.kotlinorm.Kronos
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_minus")
data class MinusConditionUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
    @ColumnType(KColumnType.TINYINT)
    var status: Int? = null,
) : KPojo

fun minusWhere(user: MinusConditionUser, block: ToFilter<MinusConditionUser, Boolean?>): Criteria? {
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

    val user = MinusConditionUser(id = 1, name = "Ada", age = 36, status = 1)

    val deMorgan = minusWhere(user) { !(it.name == "Ada" && it.age > 18) }
    if (deMorgan?.type != ConditionType.OR) return "Fail: De Morgan type was ${deMorgan?.type}"
    if (deMorgan.children.size != 2) return "Fail: De Morgan child size was ${deMorgan.children.size}"
    if (deMorgan.children.any { it?.not != true }) return "Fail: De Morgan children should be negated"

    val excluded = minusWhere(user) { (it - it.status).eq }
    if (excluded?.type != ConditionType.AND) return "Fail: minus type was ${excluded?.type}"
    val names = excluded.children.mapNotNull { it?.field?.name }.toSet()
    if ("status" in names) return "Fail: status should be excluded"
    if (!names.containsAll(listOf("id", "name", "age"))) return "Fail: fields were $names"

    val takeIfFalse = minusWhere(user) { (it.id == 1).takeIf(false) }
    if (takeIfFalse != null) return "Fail: takeIf(false) should be null"

    return "OK"
}
