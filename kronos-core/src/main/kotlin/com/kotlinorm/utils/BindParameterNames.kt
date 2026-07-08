/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.utils

internal fun allocateBindParameterName(
    baseName: String,
    parameterValues: Map<String, Any?>,
    paramNameCounter: MutableMap<String, Int>
): String {
    if (!parameterValues.containsKey(baseName)) {
        return baseName
    }

    var count = paramNameCounter.getOrDefault(baseName, 0)
    var candidate: String
    do {
        count++
        candidate = "$baseName@$count"
    } while (parameterValues.containsKey(candidate))
    paramNameCounter[baseName] = count
    return candidate
}
