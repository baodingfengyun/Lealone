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
package org.lealone.test.aose;

import org.junit.Test;
import org.lealone.db.index.standard.ValueDataType;
import org.lealone.db.index.standard.VersionedValue;
import org.lealone.db.index.standard.VersionedValueType;
import org.lealone.db.value.Value;
import org.lealone.db.value.ValueArray;
import org.lealone.db.value.ValueLong;
import org.lealone.db.value.ValueString;
import org.lealone.storage.IterationParameters;
import org.lealone.storage.StorageMap;
import org.lealone.storage.StorageMapCursor;
import org.lealone.storage.aose.AOStorage;
import org.lealone.storage.aose.btree.BTreeMap;
import org.lealone.storage.aose.btree.PageStorageMode;
import org.lealone.test.TestBase;
import org.lealone.transaction.aote.TransactionalValue;
import org.lealone.transaction.aote.TransactionalValueType;

public class PageStorageModeTest extends TestBase {

    private final int rowCount = 6000;
    private final int columnCount = 10;
    private final int pageSplitSize = 1024 * 1024;
    private final int cacheSize = 100 * 1024 * 1024; // 100M

    @Test
    public void run() {
        ValueDataType keyType = new ValueDataType(null, null, null);
        ValueDataType valueType = new ValueDataType(null, null, null);
        VersionedValueType vvType = new VersionedValueType(valueType, columnCount);
        TransactionalValueType tvType = new TransactionalValueType(vvType);

        testRowStorage(keyType, tvType);
        testColumnStorage(keyType, tvType);
    }

    private void testRowStorage(ValueDataType keyType, TransactionalValueType tvType) {
        testStorage(keyType, tvType, PageStorageMode.ROW_STORAGE, "testRowStorage");
    }

    private void testColumnStorage(ValueDataType keyType, TransactionalValueType tvType) {
        testStorage(keyType, tvType, PageStorageMode.COLUMN_STORAGE, "testColumnStorage");
    }

    private void putData(StorageMap<ValueLong, TransactionalValue> map) {
        if (!map.isEmpty())
            return;
        for (int row = 1; row <= rowCount; row++) {
            ValueLong key = ValueLong.get(row);
            Value[] columns = new Value[columnCount];
            for (int col = 0; col < columnCount; col++) {
                columns[col] = ValueString.get("value-row" + row + "-col" + (col + 1));
            }
            VersionedValue vv = new VersionedValue(row, ValueArray.get(columns));
            TransactionalValue tv = TransactionalValue.createCommitted(vv);
            map.put(key, tv);
        }
        map.save();
    }

    private void testStorage(ValueDataType keyType, TransactionalValueType tvType, PageStorageMode mode,
            String mapName) {
        AOStorage storage = AOStorageTest.openStorage(pageSplitSize, cacheSize);
        BTreeMap<ValueLong, TransactionalValue> map = storage.openBTreeMap(mapName, keyType, tvType, null);
        map.setPageStorageMode(mode);
        putData(map);

        ValueLong firstKey = map.firstKey();
        assertEquals(1, firstKey.getLong());

        int columnIndex = 2; // 索引要从0开始算

        ValueLong key = ValueLong.get(4000);
        TransactionalValue tv = map.get(key);
        VersionedValue vv = (VersionedValue) tv.getValue();
        Value columnValue = vv.value.getList()[columnIndex];
        assertEquals("value-row4000-col3", columnValue.getString());

        key = ValueLong.get(2);
        tv = map.get(key, columnIndex);
        vv = (VersionedValue) tv.getValue();
        columnValue = vv.value.getList()[columnIndex];
        assertEquals("value-row2-col3", columnValue.getString());

        key = ValueLong.get(2999);
        tv = map.get(key, columnIndex);
        vv = (VersionedValue) tv.getValue();
        columnValue = vv.value.getList()[columnIndex];
        assertEquals("value-row2999-col3", columnValue.getString());

        int rows = 0;
        ValueLong from = ValueLong.get(2000);
        StorageMapCursor<ValueLong, TransactionalValue> cursor = map
                .cursor(IterationParameters.create(from, columnIndex));
        while (cursor.hasNext()) {
            cursor.next();
            rows++;
        }
        assertEquals(rowCount - 2000 + 1, rows);
        map.close();
    }
}
