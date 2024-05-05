import com.kotoframework.utils.Extensions.toKPojo
import com.kotoframework.utils.Extensions.toMap
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExtensionTest {

    private val user = TbUser(
        id = 9938,
        userName = "Leinbo",
        password = "Leinbo",
        nickname = "Leinbo",
        email = "mail@leinbo.com",
        age = 18,
        birthday = "2020-08-02",
        telephone = "13888888888",
        sex = "male",
        avatar = "https://cdn.leinbo.com/avatar.png"
    )

    private val map = mapOf(
        "id" to 9938,
        "userName" to "Leinbo",
        "password" to "Leinbo",
        "nickname" to "Leinbo",
        "email" to "mail@leinbo.com",
        "age" to 18,
        "birthday" to "2020-08-02",
        "telephone" to "13888888888",
        "sex" to "male",
        "avatar" to "https://cdn.leinbo.com/avatar.png",
        "updateTime" to null
    )

    @Test
    fun testKPojoToMap() {
        val a = user.toMap()
        assertEquals(map, a)
    }

    @Test
    fun testMapToKPojo() {
        val a = map.toKPojo<TbUser>()
        assertEquals(user, a)
    }
}