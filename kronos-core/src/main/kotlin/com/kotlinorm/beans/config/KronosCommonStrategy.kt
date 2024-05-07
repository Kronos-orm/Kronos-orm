package com.kotlinorm.beans.config

import com.kotlinorm.beans.dsl.Field

class KronosCommonStrategy(
    var enabled: Boolean = false,
    var field: Field,
    var config: Any? = null
)