package com.kotoframework.interfaces

import com.kotoframework.beans.task.KotoAtomicTask

interface KotoSQLTask {
    val tasks: List<KotoAtomicTask>
    val async: Boolean
}