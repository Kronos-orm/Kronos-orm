/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.utils.Extensions.rmRedundantBlk

data class ConflictResolver(
    val tableName: String,
    val onFields: LinkedHashSet<Field>,
    val toUpdateFields: LinkedHashSet<Field>,
    val toInsertFields: LinkedHashSet<Field>
)

fun mysqlOnDuplicateSql(conflictResolver: ConflictResolver): String {
    val (tableName, _, toUpdateFields, toInsertFields) = conflictResolver
    return """
        INSERT INTO `$tableName` 
            (${toInsertFields.joinToString { it.quoted() }})
        VALUES 
            (${toInsertFields.joinToString(", ") { ":$it" }}) 
        ON DUPLICATE KEY UPDATE 
            ${toUpdateFields.joinToString(", ") { it.equation() }}
    """.rmRedundantBlk()
}

fun mysqlOnExistSql(conflictResolver: ConflictResolver): String {
    val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
    return """
        INSERT INTO `$tableName` 
            (${toInsertFields.joinToString { it.quoted() }})
        SELECT 
            ${toInsertFields.joinToString(", ") { ":$it" }}
        FROM DUAL
        WHERE 
            NOT EXISTS 
                (SELECT 1 FROM 
                    `$tableName`
                 WHERE 
                    ${onFields.joinToString(" AND ") { it.equation() }});
        UPDATE `$tableName`
        SET
            ${toUpdateFields.joinToString(", ") { it.equation() }}
        WHERE
            ${onFields.joinToString(" AND ") { it.equation() }}
    """.rmRedundantBlk()
}

fun sqlServerOnExistSql(conflictResolver: ConflictResolver): String {
    val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
    return """
        IF EXISTS (SELECT 1 FROM $tableName WHERE ${onFields.joinToString(" AND ") { it.equation() }})
            BEGIN 
                UPDATE $tableName SET ${toUpdateFields.joinToString { it.equation() }}
            END
        ELSE 
            BEGIN
                INSERT INTO $tableName (${toInsertFields.joinToString { it.quoted() }})
                VALUES (${toInsertFields.joinToString(", ") { ":$it" }})
            END
    """.rmRedundantBlk()
}

fun postgresOnExistSql(conflictResolver: ConflictResolver): String {
    val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
    return """
        INSERT INTO $tableName 
            (${toInsertFields.joinToString { it.quoted() }})
        SELECT 
           ${toInsertFields.joinToString(", ") { ":$it" }}
        WHERE NOT EXISTS ( 
           SELECT 1 FROM $tableName
           WHERE ${onFields.joinToString(" AND ") { it.equation() }}
        );
        
        UPDATE $tableName
        SET
           ${toUpdateFields.joinToString(", ") { it.equation() }}
        WHERE
           ${onFields.joinToString(" AND ") { it.equation() }};
    """.rmRedundantBlk()
}

fun oracleOnExistSql(conflictResolver: ConflictResolver): String {
    val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
    return """
        UPDATE $tableName 
            SET 
                ${toUpdateFields.joinToString(", ") { it.equation() }}
            WHERE
                ${onFields.joinToString(" AND ") { it.equation() }};
        IF (sql%notfound) THEN
            INSERT INTO $tableName 
                (${toInsertFields.joinToString { it.quoted() }})
            VALUES 
                (${toInsertFields.joinToString(", ") { ":$it" }})
        END IF;
    """.rmRedundantBlk()
}

fun oracleOnConflictSql(conflictResolver: ConflictResolver): String {
    val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
    return """
        BEGIN
            INSERT INTO "$tableName" 
                (${toInsertFields.joinToString { it.quoted() }})
            VALUES 
            (${toInsertFields.joinToString(", ") { ":$it" }}) 
            EXCEPTION 
                WHEN 
                    DUP_VAL_ON_INDEX 
                THEN 
                    UPDATE "$tableName"
                    SET 
                        ${toUpdateFields.joinToString(", ") { it.equation() }}
                    WHERE 
                        ${onFields.joinToString(" AND ") { it.equation() }};
        END;
    """.rmRedundantBlk()
}

fun sqliteOnConflictSql(conflictResolver: ConflictResolver): String {
    val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
    return """
        INSERT OR REPLACE INTO "$tableName" 
            (${toInsertFields.joinToString { it.quoted() }}) 
        VALUES 
            (${toInsertFields.joinToString(", ") { ":$it" }}) 
        ON CONFLICT 
            (${onFields.joinToString(", ") { it.quoted() }})
        DO UPDATE SET
            ${toUpdateFields.joinToString(", ") { it.equation() }}
    """.rmRedundantBlk()
}