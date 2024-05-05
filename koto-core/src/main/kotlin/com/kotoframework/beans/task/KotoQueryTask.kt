package com.kotoframework.beans.task

import com.kotoframework.interfaces.KotoSQLTask

/**
 * Created by ousc on 2022/4/18 11:01
 */
class KotoQueryTask(
    override val tasks: List<KotoAtomicTask>,
    override val async: Boolean
) : KotoSQLTask