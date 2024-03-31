package beans

import com.kotoframework.interfaces.KPojo

data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null
) : KPojo