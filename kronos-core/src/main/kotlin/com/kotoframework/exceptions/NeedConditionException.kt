package com.kotoframework.exceptions

import com.kotoframework.i18n.Noun.needConditionMessage

class NeedConditionException(message: String = needConditionMessage) : RuntimeException(message)