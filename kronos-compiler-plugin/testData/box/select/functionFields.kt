import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.functions.bundled.exts.MathFunctions.add
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_function")
data class FunctionSelectUser(
    var id: Int? = null,
    var age: Int = 0,
) : KPojo

fun FunctionSelectUser.collectSelectItems(block: ToSelect<FunctionSelectUser, Any?>): List<SqlSelectItem> {
    val result = mutableListOf<SqlSelectItem>()
    afterSelect {
        block!!(it)
        result += selectItems
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val items = FunctionSelectUser().collectSelectItems { [f.count(it.id).alias("cnt"), f.sum(it.age).alias("sumAge")] }
    val count = items.getOrNull(0) as? SqlSelectItem.Expr
    val sum = items.getOrNull(1) as? SqlSelectItem.Expr

    return when {
        items.size != 2 -> "Fail: size was ${items.size}"
        count?.alias != "cnt" -> "Fail: alias was ${count?.alias}"
        (count?.expr as? SqlExpr.Function)?.name?.last != "COUNT" -> "Fail: count expr was ${count?.expr}"
        (sum?.expr as? SqlExpr.Function)?.name?.last != "SUM" -> "Fail: sum expr was ${sum?.expr}"
        sum?.alias != "sumAge" -> "Fail: second alias was ${sum?.alias}"
        else -> "OK"
    }
}
