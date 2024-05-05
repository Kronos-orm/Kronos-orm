package com.kotoframework.beans.task

import com.kotoframework.interfaces.KAtomicTask
import com.kotoframework.interfaces.KotoSQLTask

/**
 * Created by ousc on 2022/4/18 11:01
 */
class KotoOperationTask(
    override val tasks: List<KAtomicTask>,
    override val async: Boolean = false
) : KotoSQLTask