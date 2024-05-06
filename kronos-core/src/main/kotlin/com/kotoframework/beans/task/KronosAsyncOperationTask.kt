package com.kotoframework.beans.task

import com.kotoframework.interfaces.KAtomicTask
import com.kotoframework.interfaces.KronosSQLTask

/**
 * Created by ousc on 2022/4/18 11:01
 */
class KronosAsyncOperationTask(
    override val tasks: List<KAtomicTask>,
    override val async: Boolean = true
) : KronosSQLTask