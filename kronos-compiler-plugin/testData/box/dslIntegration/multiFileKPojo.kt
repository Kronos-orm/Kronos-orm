// FILE: entities.kt
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table(name = "tb_multi_a")
data class MultiA(var id: Int? = null, var name: String? = null) : KPojo

@Table(name = "tb_multi_b")
data class MultiB(var id: Int? = null, var aId: Int? = null) : KPojo

// FILE: box.kt
import com.kotlinorm.Kronos
import com.kotlinorm.orm.select.select

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val a = MultiA(1, "Ada")
    val b = MultiB(2, 1)
    val aStatement = a.select { [it.id, it.name] }.toStatement()
    val bStatement = b.select { [it.id, it.aId] }.toStatement()

    return when {
        a.__tableName != "tb_multi_a" -> "Fail: A table was ${a.__tableName}"
        b.__tableName != "tb_multi_b" -> "Fail: B table was ${b.__tableName}"
        aStatement.selectList.size != 2 -> "Fail: A select size was ${aStatement.selectList.size}"
        bStatement.selectList.size != 2 -> "Fail: B select size was ${bStatement.selectList.size}"
        else -> "OK"
    }
}
