import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.statement.SqlQuery

@Table("tb_symbol_user")
data class SymbolUser(var id: Int? = null, var name: String? = null) : KPojo

@Table("tb_symbol_post")
data class SymbolPost(var id: Int? = null, var userId: Int? = null, var title: String? = null) : KPojo

@Table("tb_symbol_comment")
data class SymbolComment(var id: Int? = null, var postId: Int? = null, var content: String? = null) : KPojo

fun box(): String {
    with(Kronos) {}

    val user = SymbolUser(1, "Ada")
    val post = SymbolPost(2, 1, "Notes")
    val comment = SymbolComment(3, 2, "OK")
    val userSelect = user.select { [it.id, it.name] }.toSqlQuery() as SqlQuery.Select
    val postSelect = post.select { [it.id, it.userId, it.title] }.toSqlQuery() as SqlQuery.Select
    val commentMap = comment.toDataMap()

    return when {
        user.kClass() != SymbolUser::class -> "Fail: user kClass was ${user.kClass()}"
        post.kClass() != SymbolPost::class -> "Fail: post kClass was ${post.kClass()}"
        comment.kClass() != SymbolComment::class -> "Fail: comment kClass was ${comment.kClass()}"
        user.kronosColumns().map { it.name } != ["id", "name"] -> "Fail: user columns were ${user.kronosColumns().map { it.name }}"
        post.kronosColumns().map { it.name } != ["id", "userId", "title"] -> "Fail: post columns were ${post.kronosColumns().map { it.name }}"
        comment.kronosColumns().map { it.name } != ["id", "postId", "content"] -> "Fail: comment columns were ${comment.kronosColumns().map { it.name }}"
        userSelect.select.size != 2 -> "Fail: user select size was ${userSelect.select.size}"
        postSelect.select.size != 3 -> "Fail: post select size was ${postSelect.select.size}"
        commentMap["content"] != "OK" -> "Fail: comment map was $commentMap"
        else -> "OK"
    }
}
