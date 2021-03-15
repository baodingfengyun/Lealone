/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lealone.db;

/**
 * The list of database setting for a SET statement.
 */
// database级的大多需要保存为Setting
public enum DbSetting {

    ALIAS_COLUMN_NAME,
    ANALYZE_AUTO,
    ANALYZE_SAMPLE,
    DATABASE_TO_UPPER,
    DB_CLOSE_ON_EXIT,
    DEFAULT_ESCAPE,
    DROP_RESTRICT,
    EARLY_FILTER,
    ESTIMATED_FUNCTION_TABLE_ROWS,
    FUNCTIONS_IN_SCHEMA,
    LARGE_RESULT_BUFFER_SIZE,
    // LARGE_TRANSACTIONS, //暂时用不到
    // MAX_COMPACT_TIME, //暂时用不到
    MAX_QUERY_TIMEOUT,
    NESTED_JOINS,
    OPTIMIZE_DISTINCT,
    OPTIMIZE_EVALUATABLE_SUBQUERIES,
    OPTIMIZE_INSERT_FROM_SELECT,
    OPTIMIZE_IN_LIST,
    OPTIMIZE_IN_SELECT,
    OPTIMIZE_IS_NULL,
    OPTIMIZE_OR,
    OPTIMIZE_TWO_EQUALS,
    QUERY_CACHE_SIZE,
    RECOMPILE_ALWAYS,
    ROWID,
    SELECT_FOR_UPDATE_MVCC,
    DEFAULT_STORAGE_ENGINE,
    DEFAULT_SQL_ENGINE,
    DEFAULT_TRANSACTION_ENGINE,
    COMPRESS,
    PERSISTENT,
    PAGE_SIZE,
    CIPHER,
    FILE_PASSWORD_HASH,
    FILE_ENCRYPTION_KEY,
    READ_ONLY,

    ALLOW_LITERALS,
    CACHE_SIZE,
    COLLATION,
    BINARY_COLLATION,
    LOB_COMPRESSION_ALGORITHM,
    DATABASE_EVENT_LISTENER,
    DB_CLOSE_DELAY,
    DEFAULT_LOCK_TIMEOUT,
    DEFAULT_TABLE_TYPE,
    EXCLUSIVE,
    IGNORECASE,
    LOCK_MODE,
    MAX_LENGTH_INPLACE_LOB,
    MAX_MEMORY_ROWS,
    MAX_MEMORY_UNDO,
    MAX_OPERATION_MEMORY,
    MODE,
    OPTIMIZE_REUSE_RESULTS,
    REFERENTIAL_INTEGRITY,
    QUERY_STATISTICS,
    QUERY_STATISTICS_MAX_ENTRIES,
    TRACE_LEVEL_SYSTEM_OUT,
    TRACE_LEVEL_FILE,
    TRACE_MAX_FILE_SIZE,
    WRITE_DELAY;

    public String getName() {
        return name();
    }

    public static boolean contains(String name) {
        try {
            DbSetting.valueOf(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
