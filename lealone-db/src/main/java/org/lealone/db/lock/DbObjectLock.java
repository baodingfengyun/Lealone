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
package org.lealone.db.lock;

import org.lealone.db.DbObjectType;
import org.lealone.db.async.AsyncHandler;
import org.lealone.db.async.AsyncResult;
import org.lealone.db.session.ServerSession;

public interface DbObjectLock {

    DbObjectType getDbObjectType();

    void addHandler(AsyncHandler<AsyncResult<Boolean>> handler);

    boolean lock(ServerSession session, boolean exclusive);

    boolean trySharedLock(ServerSession session);

    boolean tryExclusiveLock(ServerSession session);

    void unlock(ServerSession session);

    void unlock(ServerSession session, boolean succeeded);

    void unlock(ServerSession oldSession, boolean succeeded, ServerSession newSession);

    boolean isLockedExclusively();

    boolean isLockedExclusivelyBy(ServerSession session);

}
