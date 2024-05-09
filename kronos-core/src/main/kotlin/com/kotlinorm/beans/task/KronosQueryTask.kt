package com.kotlinorm.beans.task

import com.kotlinorm.interfaces.KronosSQLTask

/**
 * Created by OUSC on 2024/4/18 23:05
 */
class KronosQueryTask(
    override val tasks: List<KronosAtomicTask>,
    override val async: Boolean
) : KronosSQLTask