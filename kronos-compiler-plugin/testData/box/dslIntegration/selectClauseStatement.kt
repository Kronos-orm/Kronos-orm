import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.ast.BinaryExpression
import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.ast.SelectItem
import com.kotlinorm.ast.SqlOperator
import com.kotlinorm.enums.SortType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table(name = "tb_integrated_user")
data class IntegratedUser(
    var id: Int? = null,
    @Column("user_name")
    var name: String? = null,
    var age: Int? = null,
) : KPojo

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = IntegratedUser(id = 7, name = "Ada", age = 36)
    val params = mutableMapOf<String, Any?>()
    val statement = user.select { [it.id, it.name] }
        .where { it.age >= 18 }
        .by { [it.id, it.name] }
        .groupBy { [it.id, it.name] }
        .orderBy { [it.name.desc(), it.id] }
        .distinct()
        .limit(5)
        .toStatement(parameterValues = params)

    val selectColumns = statement.selectList.mapNotNull { (it as? SelectItem.ColumnSelectItem)?.column?.columnName }
    val groupColumns = statement.groupBy?.mapNotNull { (it as? ColumnReference)?.columnName }

    return when {
        !statement.distinct -> "Fail: distinct was false"
        statement.limit?.limit != 5 -> "Fail: limit was ${statement.limit?.limit}"
        selectColumns != listOf("id", "user_name") -> "Fail: select columns were $selectColumns"
        groupColumns != listOf("id", "user_name") -> "Fail: group columns were $groupColumns"
        statement.orderBy?.map { it.direction } != listOf(SortType.DESC, SortType.ASC) -> "Fail: orderBy was ${statement.orderBy}"
        (statement.where as? BinaryExpression)?.operator != SqlOperator.AND -> "Fail: where operator was ${(statement.where as? BinaryExpression)?.operator}"
        18 !in params.values -> "Fail: params did not contain age value: $params"
        params["id"] != 7 -> "Fail: id param was ${params["id"]}"
        params["name"] != "Ada" -> "Fail: name param was ${params["name"]}"
        else -> "OK"
    }
}
