package com.kotoframework.beans.task

import com.kotoframework.enums.KotoAtomicOperationType
import com.kotoframework.interfaces.KotoSQLTask

/**
 * Created by ousc on 2022/4/18 11:01
 */
class KotoAsyncOperationTask(
    override val tasks: List<KotoAtomicTask>,
    override val async: Boolean = true
) : KotoSQLTask