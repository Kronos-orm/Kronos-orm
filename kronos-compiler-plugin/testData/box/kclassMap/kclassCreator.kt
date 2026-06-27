import com.kotlinorm.Kronos
import com.kotlinorm.annotations.KronosInit
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.kClassCreator

@Table(name = "tb_creator_a")
data class CreatorA(var id: Int? = null) : KPojo

@Table(name = "tb_creator_b")
data class CreatorB(var name: String? = null) : KPojo

@KronosInit
fun customInit(block: Kronos.() -> Unit) = Kronos.apply(block)

fun box(): String {
    customInit {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val a1 = kClassCreator(CreatorA::class)
    val a2 = kClassCreator(CreatorA::class)
    val b = kClassCreator(CreatorB::class)

    return when {
        a1 !is CreatorA -> "Fail: a1 was ${a1?.let { it::class.simpleName }}"
        a2 !is CreatorA -> "Fail: a2 was ${a2?.let { it::class.simpleName }}"
        a1 === a2 -> "Fail: creator should return distinct instances"
        b !is CreatorB -> "Fail: b was ${b?.let { it::class.simpleName }}"
        else -> "OK"
    }
}
