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
package org.lealone.storage.aose;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.lealone.common.util.CaseInsensitiveMap;
import org.lealone.common.util.DataUtils;
import org.lealone.db.DataBuffer;
import org.lealone.db.IDatabase;
import org.lealone.db.RunMode;
import org.lealone.db.session.Session;
import org.lealone.db.value.ValueString;
import org.lealone.net.NetNode;
import org.lealone.storage.PageOperationHandlerFactory;
import org.lealone.storage.StorageBase;
import org.lealone.storage.StorageCommand;
import org.lealone.storage.StorageMap;
import org.lealone.storage.aose.btree.BTreeMap;
import org.lealone.storage.aose.btree.BTreeMapBuilder;
import org.lealone.storage.fs.FilePath;
import org.lealone.storage.fs.FileUtils;
import org.lealone.storage.replication.ReplicationSession;
import org.lealone.storage.type.StorageDataType;

/**
 * Adaptive optimization storage
 * 
 * @author zhh
 */
public class AOStorage extends StorageBase {

    public static final String SUFFIX_AO_FILE = ".db";
    public static final int SUFFIX_AO_FILE_LENGTH = SUFFIX_AO_FILE.length();

    private final PageOperationHandlerFactory pohFactory;

    AOStorage(Map<String, Object> config, PageOperationHandlerFactory pohFactory) {
        super(config);
        this.pohFactory = pohFactory;
        String storagePath = getStoragePath();
        DataUtils.checkNotNull(storagePath, "storage path");
        if (!FileUtils.exists(storagePath))
            FileUtils.createDirectories(storagePath);
        FilePath dir = FilePath.get(storagePath);
        for (FilePath fp : dir.newDirectoryStream()) {
            String mapFullName = fp.getName();
            if (mapFullName.startsWith(TEMP_NAME_PREFIX)) {
                fp.delete();
            }
        }
    }

    public PageOperationHandlerFactory getPageOperationHandlerFactory() {
        return pohFactory;
    }

    @Override
    public <K, V> StorageMap<K, V> openMap(String name, StorageDataType keyType, StorageDataType valueType,
            Map<String, String> parameters) {
        String mapType = parameters == null ? null : parameters.get("mapType");
        return openMap(name, mapType, keyType, valueType, parameters);
    }

    public <K, V> StorageMap<K, V> openMap(String name, String mapType, StorageDataType keyType,
            StorageDataType valueType, Map<String, String> parameters) {
        if (mapType == null || mapType.equalsIgnoreCase("BTreeMap")) {
            return openBTreeMap(name, keyType, valueType, parameters);
        } else {
            throw DataUtils.newIllegalArgumentException("Unknow map type: {0}", mapType);
        }
    }

    public <K, V> BTreeMap<K, V> openBTreeMap(String name) {
        return openBTreeMap(name, null, null, null);
    }

    public <K, V> BTreeMap<K, V> openBTreeMap(String name, StorageDataType keyType, StorageDataType valueType,
            Map<String, String> parameters) {
        BTreeMapBuilder<K, V> builder = new BTreeMapBuilder<>();
        builder.keyType(keyType);
        builder.valueType(valueType);
        return openMap(name, builder, parameters);
    }

    @SuppressWarnings("unchecked")
    private <K, V> BTreeMap<K, V> openMap(String name, BTreeMapBuilder<K, V> builder, Map<String, String> parameters) {
        StorageMap<?, ?> map = maps.get(name);
        if (map == null) {
            synchronized (this) {
                map = maps.get(name);
                if (map == null) {
                    CaseInsensitiveMap<Object> c = new CaseInsensitiveMap<>(config);
                    if (parameters != null)
                        c.putAll(parameters);
                    builder.name(name).config(c).aoStorage(this);
                    map = builder.openMap();
                    maps.put(name, map);
                }
            }
        }
        return (BTreeMap<K, V>) map;
    }

    public boolean isReadOnly() {
        return config.containsKey("readOnly");
    }

    @Override
    public void replicateFrom(ByteBuffer data) {
        boolean containsSysMap = data.get() == 1;
        int size = data.getInt();
        for (int i = 0; i < size; i++) {
            String mapName = ValueString.type.read(data);
            BTreeMap<?, ?> map = (BTreeMap<?, ?>) getMap(mapName);
            map.readRootPageFrom(data);
            // 执行db.copy会把SYS表的数据完整复制过来，然后执行SYS表中的所有DDL语句，
            // 这些DDL语句包括建表语句，执行完建表语句就能得到所要的空MAP了，
            // 最后为每个MAP复制root page即可
            if (containsSysMap && i == 0) {
                IDatabase db = (IDatabase) config.get("db");
                db = db.copy();
                config.put("db", db);
                map.setDatabase(db);
            }
        }
    }

    @Override
    public void scaleOut(IDatabase db, RunMode oldRunMode, RunMode newRunMode, String[] oldNodes, String[] newNodes) {
        List<NetNode> replicationNodes = BTreeMap.getReplicationNodes(db, newNodes);
        // 用最高权限的用户移动页面，因为目标节点上可能还没有对应的数据库
        Session session = db.createInternalSession(true);
        ReplicationSession rs = db.createReplicationSession(session, replicationNodes);
        String sysMapName = db.getSysMapName();
        try (DataBuffer buff = DataBuffer.create(); StorageCommand c = rs.createStorageCommand()) {
            HashMap<String, StorageMap<?, ?>> maps = new HashMap<>(this.maps);
            boolean containsSysMap = maps.containsKey(sysMapName);
            buff.put(containsSysMap ? (byte) 1 : (byte) 0);
            Collection<StorageMap<?, ?>> values = maps.values();
            buff.putInt(values.size());
            if (containsSysMap) {
                // SYS表放在前面，并且总是使用CLIENT_SERVER模式
                StorageMap<?, ?> sysMap = maps.remove(sysMapName);
                replicateRootPage(db, sysMap, buff, oldNodes, RunMode.CLIENT_SERVER);
            }
            for (StorageMap<?, ?> map : values) {
                replicateRootPage(db, map, buff, oldNodes, newRunMode);
            }
            ByteBuffer pageBuffer = buff.getAndFlipBuffer();
            c.replicatePages(db.getShortName(), AOStorageEngine.NAME, pageBuffer);
        }
    }

    private void replicateRootPage(IDatabase db, StorageMap<?, ?> map, DataBuffer p, String[] oldNodes,
            RunMode runMode) {
        String mapName = map.getName();
        ValueString.type.write(p, mapName);

        BTreeMap<?, ?> btreeMap = (BTreeMap<?, ?>) map;
        setBTreeMap(btreeMap, db, runMode, oldNodes);
        btreeMap.replicateRootPage(p);
    }

    @Override
    public void scaleIn(IDatabase db, RunMode oldRunMode, RunMode newRunMode, String[] oldNodes, String[] newNodes) {
        // 当oldRunMode是CLIENT_SERVER或REPLICATION时不需要做什么，调用者会在上层进行处理，
        // 存储层只需要处理SHARDING的场景
        if (oldRunMode == RunMode.SHARDING) {
            String localHostId = NetNode.getLocalTcpNode().getHostAndPort();
            HashSet<String> oldSet = new HashSet<>(Arrays.asList(oldNodes));
            if (oldSet.contains(localHostId)) {
                for (StorageMap<?, ?> map : maps.values()) {
                    BTreeMap<?, ?> btreeMap = (BTreeMap<?, ?>) map;
                    setBTreeMap(btreeMap, db, newRunMode, oldNodes);
                    // 直接把当前节点上的所有LeafPage移到别的节点
                    btreeMap.moveAllLocalLeafPages(oldNodes, newNodes, newRunMode);
                }
            }
        }
    }

    private static void setBTreeMap(BTreeMap<?, ?> btreeMap, IDatabase db, RunMode runMode, String[] oldNodes) {
        btreeMap.setDatabase(db);
        btreeMap.setRunMode(runMode);
        btreeMap.setOldNodes(oldNodes);
    }
}
