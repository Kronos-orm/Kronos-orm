package com.kotoframework.interfaces

interface KronosSQLTask {
    val tasks: List<KAtomicTask>
    val async: Boolean
}