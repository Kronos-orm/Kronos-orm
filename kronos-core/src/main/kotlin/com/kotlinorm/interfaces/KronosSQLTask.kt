package com.kotlinorm.interfaces

interface KronosSQLTask {
    val tasks: List<KAtomicTask>
    val async: Boolean
}