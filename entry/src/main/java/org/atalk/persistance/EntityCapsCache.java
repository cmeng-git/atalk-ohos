/*
 * aTalk, android VoIP and Instant Messaging client
 * Copyright 2014~2022 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.persistance;

import ohos.data.rdb.RdbPredicates;
import ohos.data.rdb.RdbStore;
import ohos.data.rdb.ValuesBucket;
import ohos.data.resultset.ResultSet;

import org.apache.http.util.TextUtils;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.caps.cache.EntityCapsPersistentCache;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import timber.log.Timber;

/**
 * Simple implementation of an EntityCapsPersistentCache that uses
 * MySQLite to store the Caps information in record for every known node.
 *
 * @author Eng Chong Meng
 */
public class EntityCapsCache implements EntityCapsPersistentCache {
    public static final String TABLE_NAME = "entityCaps";
    public static final String ENTITY_NODE_VER = "nodeVer";
    public static final String ENTITY_DISC_INFO = "discInfo";

    private final RdbStore mRdbStore;

    /**
     * Creates a new EntityCapsCache Object.
     */
    public EntityCapsCache() {
        mRdbStore = DatabaseBackend.getRdbStore();
    }

    /**
     * Writes the DiscoverInfo stanza to an file
     *
     * @param nodeVer Entity nodeVersion for key reference in DB
     * @param info discoInto to save to DB
     */
    @Override
    public void addDiscoverInfoByNodePersistent(String nodeVer, DiscoverInfo info) {
        ValuesBucket values = new ValuesBucket();

        values.putString(ENTITY_NODE_VER, nodeVer);
        values.putString(ENTITY_DISC_INFO, info.toXML().toString());
        mRdbStore.insert(TABLE_NAME, values);
    }

    /**
     * Restore an DiscoverInfo stanza from DB.
     *
     * @param nodeVer Entity nodeVersion for retrieving from DB
     *
     * @return the restored DiscoverInfo
     */
    @Override
    public DiscoverInfo lookup(String nodeVer) {
        String content = null;
        String[] columns = {ENTITY_DISC_INFO};

        RdbPredicates rdbPredicates = new RdbPredicates(TABLE_NAME)
                .equalTo(ENTITY_NODE_VER, nodeVer);
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);
        while (resultSet.goToNextRow()) {
            content = resultSet.getString(0);
        }
        resultSet.close();

       DiscoverInfo discInfo = null;
        if (!TextUtils.isEmpty(content)) {
            try {
                discInfo = PacketParserUtils.parseStanza(content);
            } catch (Exception e) {
                Timber.w("Could not restore discInfo from DB: %s", e.getMessage());
            }
        }
        return discInfo;
    }

    @Override
    public void emptyCache() {
        RdbPredicates rdbPredicates = new RdbPredicates(TABLE_NAME);
        mRdbStore.delete(rdbPredicates);
    }
}
