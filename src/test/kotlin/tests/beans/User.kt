package tests.beans

import com.kotoframework.annotations.Table
import com.kotoframework.interfaces.KPojo

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null
) : KPojo