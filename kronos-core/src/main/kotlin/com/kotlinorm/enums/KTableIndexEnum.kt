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

package com.kotlinorm.enums

/**
 * This object represents the MySQL database type.
 *
 * It contains two nested objects, KIndexMethod and KIndexType, which define the index methods and types supported by MySQL.
 */
object Mysql {
    /**
     * This property represents the type of the database, which is MySQL in this case.
     */
    val type = DBType.Mysql

    /**
     * This object represents the index methods supported by MySQL.
     *
     * It contains two constants, BTREE and HASH, which represent the B-tree and hash index methods respectively.
     */
    object KIndexMethod {
        /**
         * This constant represents the B-tree index method.
         */
        const val BTREE = "BTREE"

        /**
         * This constant represents the hash index method.
         */
        const val HASH = "HASH"
    }

    /**
     * This object represents the index types supported by MySQL.
     *
     * It contains six constants, NORMAL, UNIQUE, FULLTEXT, SPATIAL, BTREE, and HASH, which represent the normal, unique, full-text, spatial, B-tree, and hash index types respectively.
     */
    object KIndexType {
        /**
         * This constant represents the normal index type.
         */
        const val NORMAL = "NORMAL"

        /**
         * This constant represents the unique index type.
         */
        const val UNIQUE = "UNIQUE"

        /**
         * This constant represents the full-text index type.
         */
        const val FULLTEXT = "FULLTEXT"

        /**
         * This constant represents the spatial index type.
         */
        const val SPATIAL = "SPATIAL"

        /**
         * This constant represents the B-tree index type.
         */
        const val BTREE = "BTREE"

        /**
         * This constant represents the hash index type.
         */
        const val HASH = "HASH"
    }
}

/**
 * This object represents the Oracle database type.
 *
 * It contains two nested objects, KIndexMethod and KIndexType, which define the index methods and types supported by Oracle.
 */
object Oracle {
    /**
     * This property represents the type of the database, which is Oracle in this case.
     */
    val type = DBType.Oracle

    /**
     * This object represents the index methods supported by Oracle.
     *
     * It contains two constants, NORMAL and BITMAP, which represent the normal and bitmap index methods respectively.
     */
    object KIndexMethod {
        /**
         * This constant represents the normal index method.
         */
        const val BITMAP = "BITMAP"
    }

    /**
     * This object represents the index types supported by Oracle.
     *
     * It contains three constants, NORMAL, UNIQUE, and BITMAP, which represent the normal, unique, and bitmap index types respectively.
     */
    object KIndexType {
        /**
         * This constant represents the normal index type.
         */
        const val NORMAL = "NORMAL"

        /**
         * This constant represents the unique index type.
         */
        const val UNIQUE = "UNIQUE"

        /**
         * This constant represents the bitmap index type.
         */
        const val BITMAP = "BITMAP"
    }
}

/**
 * This object represents the SQL Server database type.
 *
 * It contains two nested objects, KIndexMethod and KIndexType, which define the index methods and types supported by SQL Server.
 */
object SqlServer {
    /**
     * This property represents the type of the database, which is SQL Server in this case.
     */
    val type = DBType.Mssql

    /**
     * This object represents the index methods supported by SQL Server.
     *
     * It contains two constants, CLUSTERED and NONCLUSTERED, which represent the clustered and non-clustered index methods respectively.
     */
    object KIndexMethod {
        /**
         * This constant represents the clustered index method.
         */
        const val CLUSTERED = "CLUSTERED"

        /**
         * This constant represents the non-clustered index method.
         */
        const val NONCLUSTERED = "NONCLUSTERED"
    }

    /**
     * This object represents the index types supported by SQL Server.
     *
     * It contains two constants, NORMAL and UNIQUE, which represent the normal and unique index types respectively.
     */
    object KIndexType {
        /**
         * This constant represents the normal index type.
         */
        const val NORMAL = "NORMAL"

        /**
         * This constant represents the unique index type.
         */
        const val UNIQUE = "UNIQUE"

        /**
         * This constant represents the clustered index type.
         */
        const val CLUSTERED = "CLUSTERED"

        /**
         * This constant represents the non-clustered index type.
         */
        const val NONCLUSTERED = "NONCLUSTERED"
    }
}

/**
 * This object represents the PostgreSQL database type.
 *
 * It contains two nested objects, KIndexMethod and KIndexType, which define the index methods and types supported by PostgreSQL.
 */
object Postgres {
    /**
     * This property represents the type of the database, which is PostgreSQL in this case.
     */
    val type = DBType.Postgres

    /**
     * This object represents the index methods supported by PostgreSQL.
     *
     * It contains five constants, BTREE, HASH, GIST, SPGIST, and GIN, which represent the B-tree, hash, GiST, SP-GiST, and GIN index methods respectively.
     */
    object KIndexMethod {
        /**
         * This constant represents the B-tree index method.
         */
        const val BTREE = "BTREE"

        /**
         * This constant represents the hash index method.
         */
        const val HASH = "HASH"

        /**
         * This constant represents the GiST index method.
         */
        const val GIST = "GIST"
        const val SPGIST = "SPGIST"
        const val GIN = "GIN"
        const val BRIN = "BRIN"
    }

    object KIndexType {
        const val NORMAL = "NORMAL"
        const val UNIQUE = "UNIQUE"
        const val BTREE = "BTREE"
        const val HASH = "HASH"
        const val GIST = "GIST"
        const val SPGIST = "SPGIST"
        const val GIN = "GIN"
        const val BRIN = "BRIN"
    }
}

/**
 * This object represents the SQLite database type.
 *
 * It contains two nested objects, KIndexMethod and KIndexType, which define the index methods and types supported by SQLite.
 */
object SQLite {
    /**
     * This property represents the type of the database, which is SQLite in this case.
     */
    val type = DBType.SQLite

    /**
     * This object represents the index methods supported by SQLite.
     *
     * It contains two constants, BTREE and HASH, which represent the B-tree and hash index methods respectively.
     */
    object KIndexMethod {
        /**
         * This constant represents the B-tree index method.
         */
        const val UNIQUE = "UNIQUE"
    }

    /**
     * This object represents the index types supported by SQLite.
     *
     * It contains two constants, NORMAL and UNIQUE, which represent the normal and unique index types respectively.
     */
    object KIndexType {
        /**
         * This constant represents the normal index type.
         */
        const val BINARY = "BINARY"

        /**
         * This constant represents the unique index type.
         */
        const val NOCASE = "NOCASE"

        /**
         * This constant represents the unique index type.
         */
        const val RTRIM = "RTRIM"
    }
}