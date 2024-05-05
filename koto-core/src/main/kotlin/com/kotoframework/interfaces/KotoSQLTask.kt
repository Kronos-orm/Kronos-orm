package com.kotoframework.interfaces

interface KotoSQLTask {
    val tasks: List<KAtomicTask>
    val async: Boolean
}