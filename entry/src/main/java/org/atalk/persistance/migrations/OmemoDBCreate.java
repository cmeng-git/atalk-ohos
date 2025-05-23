package org.atalk.persistance.migrations;

import ohos.data.rdb.RdbStore;

import org.atalk.crypto.omemo.SQLiteOmemoStore;

import static org.atalk.persistance.DatabaseBackend.*;

public class OmemoDBCreate
{
    // Create all relevant tables for OMEMO crypto support
    public static void createOmemoTables(RdbStore db) {
        db.executeSql("DROP TABLE IF EXISTS " + SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME);
        db.executeSql(CREATE_OMEMO_DEVICES_STATEMENT);

        db.executeSql("DROP TABLE IF EXISTS " + SQLiteOmemoStore.PREKEY_TABLE_NAME);
        db.executeSql(CREATE_PREKEYS_STATEMENT);

        db.executeSql("DROP TABLE IF EXISTS " + SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME);
        db.executeSql(CREATE_SIGNED_PREKEYS_STATEMENT);

        db.executeSql("DROP TABLE IF EXISTS " + SQLiteOmemoStore.IDENTITIES_TABLE_NAME);
        db.executeSql(CREATE_IDENTITIES_STATEMENT);

        db.executeSql("DROP TABLE IF EXISTS " + SQLiteOmemoStore.SESSION_TABLE_NAME);
        db.executeSql(CREATE_SESSIONS_STATEMENT);
    }
}
