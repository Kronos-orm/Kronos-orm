import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.functions.FunctionManager.registerFunctionBuilder
import com.kotlinorm.functions.bundled.builders.PostgresFunctionBuilder
import com.kotlinorm.functions.bundled.exts.MathFunctions.add
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_function")
data class FunctionSelectUser(
    var id: Int? = null,
    var age: Int? = null,
) : KPojo

fun FunctionSelectUser.collectFields(block: ToSelect<FunctionSelectUser, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterSelect {
        block!!(it)
        result += fields
    }
    return result
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
        registerFunctionBuilder(PostgresFunctionBuilder)
    }

    val user = FunctionSelectUser()
    val fields = user.collectFields { [f.count(it.id).as_("cnt"), f.sum(it.age), it.age + 1] }

    return when {
        fields.size != 3 -> "Fail: size was ${fields.size}"
        fields[0].name != "cnt" -> "Fail: alias was ${fields[0].name}"
        fields[0] !is FunctionField -> "Fail: count should be FunctionField"
        fields[1] !is FunctionField -> "Fail: sum should be FunctionField"
        fields[2] !is FunctionField -> "Fail: arithmetic should be FunctionField"
        else -> "OK"
    }
}
