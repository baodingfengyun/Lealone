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
package org.lealone.server.protocol.replication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lealone.net.NetInputStream;
import org.lealone.net.NetOutputStream;
import org.lealone.server.protocol.PacketDecoder;
import org.lealone.server.protocol.PacketType;
import org.lealone.server.protocol.statement.StatementUpdateAck;
import org.lealone.storage.replication.ReplicaCommand;
import org.lealone.storage.replication.ReplicationConflictType;

public class ReplicationUpdateAck extends StatementUpdateAck {

    public final long key;
    public final long first;
    public final List<String> uncommittedReplicationNames;
    public final ReplicationConflictType replicationConflictType;
    private ReplicaCommand replicaCommand;

    public ReplicationUpdateAck(int updateCount, long key, long first, List<String> uncommittedReplicationNames,
            ReplicationConflictType replicationConflictType) {
        super(updateCount);
        this.key = key;
        this.first = first;
        this.uncommittedReplicationNames = uncommittedReplicationNames;
        this.replicationConflictType = replicationConflictType == null ? ReplicationConflictType.NONE
                : replicationConflictType;
    }

    @Override
    public PacketType getType() {
        return PacketType.REPLICATION_UPDATE_ACK;
    }

    public ReplicaCommand getReplicaCommand() {
        return replicaCommand;
    }

    public void setReplicaCommand(ReplicaCommand replicaCommand) {
        this.replicaCommand = replicaCommand;
    }

    @Override
    public void encode(NetOutputStream out, int version) throws IOException {
        super.encode(out, version);
        out.writeLong(key);
        out.writeLong(first);
        if (uncommittedReplicationNames == null) {
            out.writeInt(0);
        } else {
            out.writeInt(uncommittedReplicationNames.size());
            for (String name : uncommittedReplicationNames)
                out.writeString(name);
        }
        out.writeInt(replicationConflictType.value);
    }

    public static final Decoder decoder = new Decoder();

    private static class Decoder implements PacketDecoder<ReplicationUpdateAck> {
        @Override
        public ReplicationUpdateAck decode(NetInputStream in, int version) throws IOException {
            return new ReplicationUpdateAck(in.readInt(), in.readLong(), in.readLong(),
                    readUncommittedReplicationNames(in), ReplicationConflictType.getType(in.readInt()));
        }
    }

    public static List<String> readUncommittedReplicationNames(NetInputStream in) throws IOException {
        int size = in.readInt();
        if (size == 0)
            return new ArrayList<>();
        ArrayList<String> uncommittedReplicationNames = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            uncommittedReplicationNames.add(in.readString());
        }
        return uncommittedReplicationNames;
    }
}
