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

package com.kotlinorm.exceptions

import com.kotlinorm.i18n.Noun.needFieldsMessage

/**
 * Need Fields Exception
 *
 * Exception thrown when need fields are not provided.
 * @param message the exception message
 * @author OUSC
 */
class EmptyFieldsException(message: String = needFieldsMessage) : RuntimeException(message)