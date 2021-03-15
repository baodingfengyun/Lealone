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
package org.lealone.storage.replication;

import java.nio.ByteBuffer;

import org.lealone.db.async.Future;
import org.lealone.storage.StorageCommand;

public interface ReplicaStorageCommand extends ReplicaCommand, StorageCommand {

    Future<Object> executeReplicaPut(String replicationName, String mapName, ByteBuffer key, ByteBuffer value,
            boolean raw, boolean addIfAbsent);

    Future<Object> executeReplicaAppend(String replicationName, String mapName, ByteBuffer value);

    Future<Boolean> executeReplicaReplace(String replicationName, String mapName, ByteBuffer key, ByteBuffer oldValue,
            ByteBuffer newValue);

    Future<Object> executeReplicaRemove(String replicationName, String mapName, ByteBuffer key);

}
