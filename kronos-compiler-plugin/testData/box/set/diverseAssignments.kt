import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSet
import java.math.BigDecimal

@Table(name = "tb_set_diverse")
data class DiverseSetUser(
    var id: Int? = null,
    var name: String? = null,
    var active: Boolean? = null,
    var amount: BigDecimal? = null,
) : KPojo {
    fun column(name: String): Field = kronosColumns().single { it.name == name }
}

fun DiverseSetUser.collectSet(block: ToSet<DiverseSetUser, Unit>): Pair<List<Field>, Map<Field, Any?>> {
    var result: Pair<List<Field>, Map<Field, Any?>>? = null
    afterSet {
        block!!(it)
        result = fields.toList() to fieldParamMap.toMap()
    }
    return result ?: error("set block did not run")
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = DiverseSetUser()
    val amount = BigDecimal("12.50")
    val (fields, params) = user.collectSet {
        it.name = "Ada"
        it.active = true
        it.amount = amount
    }

    return when {
        fields.map { it.name } != listOf("name", "active", "amount") -> "Fail: fields were ${fields.map { it.name }}"
        params[user.column("name")] != "Ada" -> "Fail: name was ${params[user.column("name")]}"
        params[user.column("active")] != true -> "Fail: active was ${params[user.column("active")]}"
        params[user.column("amount")] != amount -> "Fail: amount was ${params[user.column("amount")]}"
        else -> "OK"
    }
}
