import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_user")
data class SelectUser(
    var id: Int? = null,
    var name: String? = null,
    @Column("phone_number")
    var phone: String? = null,
    var age: Int? = null,
) : KPojo {
    fun column(name: String): Field {
        return __columns.single { it.name == name }
    }
}

data class SelectCapture(
    val fields: List<Field>,
    val selectItems: List<SqlSelectItem>
)

fun SelectUser.collectSelect(block: ToSelect<SelectUser, Any?>): SelectCapture {
    val resultFields = mutableListOf<Field>()
    val resultItems = mutableListOf<SqlSelectItem>()
    afterSelect {
        block!!(it)
        resultFields += fields
        resultItems += selectItems
    }
    return SelectCapture(resultFields, resultItems)
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = SelectUser()
    val capture = user.collectSelect { [it.id, it.name, it.phone.alias("mobile"), "1"] }
    val fields = capture.fields
    val rawItem = capture.selectItems.singleOrNull() as? SqlSelectItem.Expr
    val rawExpr = rawItem?.expr as? SqlExpr.NumberLiteral

    return when {
        fields.size != 3 -> "Fail: size was ${fields.size}"
        capture.selectItems.size != 1 -> "Fail: select item size was ${capture.selectItems.size}"
        fields[0] != user.column("id") -> "Fail: first field was ${fields[0].name}"
        fields[1] != user.column("name") -> "Fail: second field was ${fields[1].name}"
        fields[2].name != "mobile" -> "Fail: alias was ${fields[2].name}"
        fields[2].columnName != "phone_number" -> "Fail: columnName was ${fields[2].columnName}"
        rawExpr?.number != "1" -> "Fail: raw expression was ${rawItem?.expr}"
        else -> "OK"
    }
}
