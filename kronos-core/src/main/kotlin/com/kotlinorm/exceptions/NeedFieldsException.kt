package com.kotlinorm.exceptions

import com.kotlinorm.i18n.Noun.needFieldsMessage

class NeedFieldsException(message: String = needFieldsMessage) : RuntimeException(message)