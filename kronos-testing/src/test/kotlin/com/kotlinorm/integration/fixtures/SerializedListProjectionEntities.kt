package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.NonNull
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table("kt_serialized_list_projection_user")
data class IntegrationSerializedListProjectionUser(
    @PrimaryKey(custom = true)
    var id: String? = null,

    var userName: String? = null,

    @NonNull
    var age: Long = 15,

    @Serialize
    var list: List<String?>? = null,
) : KPojo
