package tests.beans

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null
) : KPojo