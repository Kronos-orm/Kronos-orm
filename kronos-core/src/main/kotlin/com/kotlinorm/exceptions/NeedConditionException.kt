package com.kotlinorm.exceptions

import com.kotlinorm.i18n.Noun.needConditionMessage

class NeedConditionException(message: String = needConditionMessage) : RuntimeException(message)