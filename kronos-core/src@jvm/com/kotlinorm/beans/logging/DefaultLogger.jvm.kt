package com.kotlinorm.beans.logging

internal actual fun platformDefaultLogger(source: Any): com.kotlinorm.interfaces.KLogger =
    BundledSimpleLoggerAdapter(source as? String ?: source::class.simpleName ?: "Kronos")
