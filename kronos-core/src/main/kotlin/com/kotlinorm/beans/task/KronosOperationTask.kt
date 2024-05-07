package com.kotlinorm.beans.task

import com.kotlinorm.interfaces.KAtomicTask
import com.kotlinorm.interfaces.KronosSQLTask

/**
 * Created by ousc on 2022/4/18 11:01
 */
class KronosOperationTask(
    override val tasks: List<KAtomicTask>,
    override val async: Boolean = false
) : KronosSQLTask