/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.lealone.db.index;

import java.util.List;
import java.util.Map;

import org.lealone.common.exceptions.DbException;
import org.lealone.db.api.ErrorCode;
import org.lealone.db.result.Row;
import org.lealone.db.result.SearchRow;
import org.lealone.db.result.SortOrder;
import org.lealone.db.schema.SchemaObject;
import org.lealone.db.session.ServerSession;
import org.lealone.db.table.Column;
import org.lealone.db.table.Table;
import org.lealone.storage.IterationParameters;
import org.lealone.storage.PageKey;
import org.lealone.transaction.Transaction;

/**
 * An index. Indexes are used to speed up searching data.
 * 
 * @author H2 Group
 * @author zhh
 */
public interface Index extends SchemaObject {

    /**
     * Get the table on which this index is based.
     *
     * @return the table
     */
    Table getTable();

    /**
     * Get the index type.
     *
     * @return the index type
     */
    IndexType getIndexType();

    /**
     * Get the indexed columns as index columns (with ordering information).
     *
     * @return the index columns
     */
    IndexColumn[] getIndexColumns();

    /**
     * Get the indexed columns.
     *
     * @return the columns
     */
    Column[] getColumns();

    /**
     * Get the indexed column ids.
     *
     * @return the column ids
     */
    int[] getColumnIds();

    /**
     * Get the index of a column in the list of index columns
     *
     * @param col the column
     * @return the index (0 meaning first column)
     */
    int getColumnIndex(Column col);

    /**
     * Get the message to show in a EXPLAIN statement.
     *
     * @return the plan
     */
    String getPlanSQL();

    default boolean supportsAsync() {
        return false;
    }

    /**
     * Add a row to the index.
     *
     * @param session the session to use
     * @param row the row to add
     */
    default void add(ServerSession session, Row row) {
        Transaction.Listener listener = Transaction.getTransactionListener();
        tryAdd(session, row, listener);
        listener.await();
    }

    default boolean tryAdd(ServerSession session, Row row, Transaction.Listener globalListener) {
        throw DbException.getUnsupportedException("add row");
    }

    default void update(ServerSession session, Row oldRow, Row newRow, List<Column> updateColumns) {
        Transaction.Listener listener = Transaction.getTransactionListener();
        int ret = tryUpdate(session, oldRow, newRow, updateColumns, listener);
        if (ret == Transaction.OPERATION_COMPLETE)
            listener.operationComplete();
        // 不能在这里调用operationUndo
        // else
        // listener.operationUndo();
        listener.await();
    }

    default int tryUpdate(ServerSession session, Row oldRow, Row newRow, List<Column> updateColumns,
            Transaction.Listener globalListener) {
        int ret = tryRemove(session, oldRow, globalListener);
        if (ret == Transaction.OPERATION_COMPLETE) {
            tryAdd(session, newRow, globalListener);
            // 等待tryAdd完成
            if (globalListener != null)
                ret = Transaction.OPERATION_NEED_WAIT;
        }
        return ret;
    }

    /**
     * Remove a row from the index.
     *
     * @param session the session
     * @param row the row
     */
    default void remove(ServerSession session, Row row) {
        if (tryRemove(session, row, null) != Transaction.OPERATION_COMPLETE)
            throw DbException.get(ErrorCode.CONCURRENT_UPDATE_1, getTable().getName());
    }

    default int tryRemove(ServerSession session, Row row, Transaction.Listener globalListener) {
        throw DbException.getUnsupportedException("remove row");
    }

    default boolean tryLock(ServerSession session, Row row) {
        return false;
    }

    /**
     * Find a row or a list of rows and create a cursor to iterate over the result.
     *
     * @param session the session
     * @param first the first row, or null for no limit
     * @param last the last row, or null for no limit
     * @return the cursor to iterate over the results
     */
    Cursor find(ServerSession session, SearchRow first, SearchRow last);

    Cursor find(ServerSession session, IterationParameters<SearchRow> parameters);

    /**
     * Check if the index can directly look up the lowest or highest value of a
     * column.
     *
     * @return true if it can
     */
    boolean canGetFirstOrLast();

    /**
     * Find the first (or last) value of this index. The cursor returned is
     * positioned on the correct row, or on null if no row has been found.
     *
     * @param session the session
     * @param first true if the first (lowest for ascending indexes) or last
     *            value should be returned
     * @return a cursor (never null)
     */
    Cursor findFirstOrLast(ServerSession session, boolean first);

    /**
     * Check if the index supports distinct query.
     *
     * @return true if it supports
     */
    boolean supportsDistinctQuery();

    /**
     * Find a distinct list of rows and create a cursor to iterate over the
     * result.
     *
     * @param session the session
     * @param first the first row, or null for no limit
     * @param last the last row, or null for no limit
     * @return the cursor to iterate over the results
     */
    Cursor findDistinct(ServerSession session, SearchRow first, SearchRow last);

    /**
     * Can this index iterate over all rows?
     *
     * @return true if it can
     */
    boolean canScan();

    /**
     * Does this index support lookup by row id?
     *
     * @return true if it does
     */
    boolean isRowIdIndex();

    /**
     * Get the row with the given key.
     *
     * @param session the session
     * @param key the unique key
     * @return the row
     */
    Row getRow(ServerSession session, long key);

    /**
     * Compare two rows.
     *
     * @param rowData the first row
     * @param compare the second row
     * @return 0 if both rows are equal, -1 if the first row is smaller, otherwise 1
     */
    int compareRows(SearchRow rowData, SearchRow compare);

    /**
     * Estimate the cost to search for rows given the search mask.
     * There is one element per column in the search mask.
     * For possible search masks, see IndexCondition.
     *
     * @param session the session
     * @param masks per-column comparison bit masks, null means 'always false',
     *              see constants in IndexCondition
     * @param filter the table filter
     * @param sortOrder the sort order
     * @return the estimated cost
     */
    double getCost(ServerSession session, int[] masks, SortOrder sortOrder);

    /**
     * Get the row count of this table, for the given session.
     *
     * @param session the session
     * @return the row count
     */
    long getRowCount(ServerSession session);

    /**
     * Get the approximated row count for this table.
     *
     * @return the approximated row count
     */
    long getRowCountApproximation();

    /**
     * Close this index.
     *
     * @param session the session used to write data
     */
    void close(ServerSession session);

    /**
     * Remove the index.
     *
     * @param session the session
     */
    void remove(ServerSession session);

    /**
     * Remove all rows from the index.
     *
     * @param session the session
     */
    void truncate(ServerSession session);

    /**
     * Get the used disk space for this index.
     *
     * @return the estimated number of bytes
     */
    long getDiskSpaceUsed();

    /**
     * Get the used memory space for this index.
     *
     * @return the estimated number of bytes
     */
    long getMemorySpaceUsed();

    /**
     * Check if the index needs to be rebuilt.
     * This method is called after opening an index.
     *
     * @return true if a rebuild is required.
     */
    boolean needRebuild();

    boolean isInMemory();

    /**
     * Add the rows to a temporary storage (not to the index yet). The rows are
     * sorted by the index columns. This is to more quickly build the index.
     *
     * @param rows the rows
     * @param bufferName the name of the temporary storage
     */
    void addRowsToBuffer(ServerSession session, List<Row> rows, String bufferName);

    /**
     * Add all the index data from the buffers to the index. The index will
     * typically use merge sort to add the data more quickly in sorted order.
     *
     * @param bufferNames the names of the temporary storage
     */
    void addBufferedRows(ServerSession session, List<String> bufferNames);

    Map<String, List<PageKey>> getNodeToPageKeyMap(ServerSession session, SearchRow first, SearchRow last);
}
