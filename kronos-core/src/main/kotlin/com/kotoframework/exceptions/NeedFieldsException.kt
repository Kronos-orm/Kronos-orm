package com.kotoframework.exceptions

import com.kotoframework.i18n.Noun.needFieldsMessage

class NeedFieldsException(message: String = needFieldsMessage) : RuntimeException(message)