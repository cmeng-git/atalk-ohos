/*
 * aTalk, ohos VoIP and Instant Messaging client
 * Copyright 2024 Eng Chong Meng
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
package org.atalk.persistance.migrations;

import ohos.data.rdb.RdbStore;

public class Migrations {
    @SuppressWarnings("fallthrough")
    public static void upgradeDatabase(RdbStore db, MigrationsHelper migrationsHelper) {
        switch (db.getVersion()) {
            case 1:
                // OmemoDBCreate.createOmemoTables(db);
                break;
        }
    }
}
