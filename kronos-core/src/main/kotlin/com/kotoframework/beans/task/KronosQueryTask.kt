package com.kotoframework.beans.task

import com.kotoframework.interfaces.KronosSQLTask

/**
 * Created by ousc on 2022/4/18 11:01
 */
class KronosQueryTask(
    override val tasks: List<KronosAtomicTask>,
    override val async: Boolean
) : KronosSQLTask