package com.kotoframework.exceptions

import com.kotoframework.i18n.Noun.needUpdateConditionMessage

class NeedUpdateConditionException(message: String = needUpdateConditionMessage) : RuntimeException(message)