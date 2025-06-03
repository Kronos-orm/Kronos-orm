/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.beans.generator

import com.kotlinorm.interfaces.KIdGenerator

object UUIDGenerator : KIdGenerator<String> {
    override fun nextId(): String = java.util.UUID.randomUUID().toString()
}

object SnowflakeIdGenerator : KIdGenerator<Long> {
    private const val SEQUENCE_BITS = 12
    private const val WORKER_ID_BITS = 5
    private const val DATACENTER_ID_BITS = 5

    private const val MAX_WORKER_ID = (1 shl WORKER_ID_BITS) - 1
    private const val MAX_DATACENTER_ID = (1 shl DATACENTER_ID_BITS) - 1

    private const val TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS
    private const val DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS
    private const val WORKER_ID_SHIFT = SEQUENCE_BITS

    private var sequence: Long = 0
    private var lastTimestamp: Long = -1
    private var _datacenterId: Long = 1
    private var _workerId: Long = 1

    /**
     * The unique id of the datacenter in which the application is running.
     * This value must be unique for each datacenter.
     *
     * The default value is 1.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var datacenterId
        get() = _datacenterId
        set(value) = run {
            require(value in 0..MAX_DATACENTER_ID) { "Datacenter id must be less than $MAX_DATACENTER_ID" }
            _datacenterId = value
        }

    /**
     * The unique id of the worker in which the application is running.
     * This value must be unique for each worker.
     *
     * The default value is 1.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var workerId
        get() = _workerId
        set(value) = run {
            require(value in 0..MAX_WORKER_ID) { "Worker id must be less than $MAX_WORKER_ID" }
            _workerId = value
        }

    @Synchronized
    override fun nextId(): Long {
        var timestamp = System.currentTimeMillis()

        if (timestamp < lastTimestamp) {
            throw IllegalStateException("Clock moved backwards. Refusing to generate id for ${lastTimestamp - timestamp} milliseconds.")
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) and (((1 shl SEQUENCE_BITS) - 1).toLong())
            if (sequence == 0L) {
                timestamp = waitNextMillis(lastTimestamp)
            }
        } else {
            sequence = 0
        }

        lastTimestamp = timestamp

        return ((timestamp shl TIMESTAMP_LEFT_SHIFT) or
                (datacenterId shl DATACENTER_ID_SHIFT) or
                (workerId shl WORKER_ID_SHIFT) or
                sequence)
    }

    private fun waitNextMillis(lastTimestamp: Long): Long {
        var timestamp = System.currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }
}

var customIdGenerator: KIdGenerator<*>? = null