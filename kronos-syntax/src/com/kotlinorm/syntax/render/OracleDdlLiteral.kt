/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.expr.SqlExpr

/** Converts a bound Oracle CTAS parameter into an SQL literal for the current platform. */
internal expect fun oracleDdlLiteral(value: Any?, parameterName: String): SqlExpr
