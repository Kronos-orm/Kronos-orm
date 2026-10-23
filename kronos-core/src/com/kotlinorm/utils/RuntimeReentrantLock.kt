package com.kotlinorm.utils

internal expect class RuntimeReentrantLock() { fun <T> withLock(block: () -> T): T }
