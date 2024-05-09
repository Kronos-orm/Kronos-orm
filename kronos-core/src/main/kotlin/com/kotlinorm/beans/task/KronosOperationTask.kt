package com.kotlinorm.beans.task

import com.kotlinorm.interfaces.KAtomicTask
import com.kotlinorm.interfaces.KronosSQLTask

/**
 * Created by OUSC on 2024/4/18 23:08
 */
class KronosOperationTask(
    override val tasks: List<KAtomicTask>,
    override val async: Boolean = false
) : KronosSQLTask