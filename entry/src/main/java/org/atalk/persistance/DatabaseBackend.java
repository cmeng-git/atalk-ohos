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
package org.atalk.persistance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import ohos.agp.render.render3d.BuildConfig;
import ohos.app.Context;
import ohos.data.DatabaseHelper;
import ohos.data.rdb.*;
import ohos.data.resultset.ResultSet;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;

import net.java.sip.communicator.impl.configuration.SQLiteConfigurationStore;
import net.java.sip.communicator.impl.msghistory.MessageSourceService;
import net.java.sip.communicator.service.callhistory.CallHistoryService;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.atalk.crypto.omemo.FingerprintStatus;
import org.atalk.crypto.omemo.SQLiteOmemoStore;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.chat.ChatSlice;
import org.atalk.ohos.gui.chat.ChatMessage;
import org.atalk.ohos.gui.chat.ChatSession;
import org.atalk.persistance.migrations.Migrations;
import org.atalk.persistance.migrations.MigrationsHelper;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import timber.log.Timber;

/**
 * The <code>DatabaseBackend</code> uses SQLite to store all the aTalk application data in the database "dbRecords.db"
 *
 * @author Eng Chong Meng
 */
public class DatabaseBackend extends DatabaseHelper {
    /**
     * Name of the database and its version number
     * Increment DATABASE_VERSION when there is a change in database records
     */
    public static final String DATABASE_NAME = "DbaTalk.db";
    private static final int DATABASE_VERSION = 1;
    static final HiLogLabel LABEL = new HiLogLabel(HiLog.LOG_APP, 0x00201, "MY_TAG");

    private static DatabaseBackend instance = null;

    private static RdbStore mRdbStore;

    private ProtocolProviderService mProvider = null;

    // Create preKeys table
    public static String CREATE_OMEMO_DEVICES_STATEMENT = "CREATE TABLE "
            + SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME + "("
            + SQLiteOmemoStore.OMEMO_JID + " TEXT, "
            + SQLiteOmemoStore.OMEMO_REG_ID + " INTEGER, "
            + SQLiteOmemoStore.CURRENT_SIGNED_PREKEY_ID + " INTEGER, "
            + SQLiteOmemoStore.LAST_PREKEY_ID + " INTEGER, UNIQUE("
            + SQLiteOmemoStore.OMEMO_JID
            + ") ON CONFLICT REPLACE);";

    // Create preKeys table
    public static String CREATE_PREKEYS_STATEMENT = "CREATE TABLE "
            + SQLiteOmemoStore.PREKEY_TABLE_NAME + "("
            + SQLiteOmemoStore.BARE_JID + " TEXT, "
            + SQLiteOmemoStore.DEVICE_ID + " INTEGER, "
            + SQLiteOmemoStore.PRE_KEY_ID + " INTEGER, "
            + SQLiteOmemoStore.PRE_KEYS + " TEXT, UNIQUE("
            + SQLiteOmemoStore.BARE_JID + ", " + SQLiteOmemoStore.DEVICE_ID + ", "
            + SQLiteOmemoStore.PRE_KEY_ID
            + ") ON CONFLICT REPLACE);";

    // Create signed preKeys table
    public static String CREATE_SIGNED_PREKEYS_STATEMENT = "CREATE TABLE "
            + SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME + "("
            + SQLiteOmemoStore.BARE_JID + " TEXT, "
            + SQLiteOmemoStore.DEVICE_ID + " INTEGER, "
            + SQLiteOmemoStore.SIGNED_PRE_KEY_ID + " INTEGER, "
            + SQLiteOmemoStore.SIGNED_PRE_KEYS + " TEXT, "
            + SQLiteOmemoStore.LAST_RENEWAL_DATE + " NUMBER, UNIQUE("
            + SQLiteOmemoStore.BARE_JID + ", " + SQLiteOmemoStore.DEVICE_ID + ", "
            + SQLiteOmemoStore.SIGNED_PRE_KEY_ID
            + ") ON CONFLICT REPLACE);";

    // Create identities table
    public static String CREATE_IDENTITIES_STATEMENT = "CREATE TABLE "
            + SQLiteOmemoStore.IDENTITIES_TABLE_NAME + "("
            + SQLiteOmemoStore.BARE_JID + " TEXT, "
            + SQLiteOmemoStore.DEVICE_ID + " INTEGER, "
            + SQLiteOmemoStore.FINGERPRINT + " TEXT, "
            + SQLiteOmemoStore.CERTIFICATE + " BLOB, "
            + SQLiteOmemoStore.TRUST + " TEXT, "
            + SQLiteOmemoStore.ACTIVE + " NUMBER, "
            + SQLiteOmemoStore.LAST_ACTIVATION + " NUMBER, "
            + SQLiteOmemoStore.LAST_DEVICE_ID_PUBLISH + " NUMBER, "
            + SQLiteOmemoStore.LAST_MESSAGE_RX + " NUMBER, "
            + SQLiteOmemoStore.MESSAGE_COUNTER + " INTEGER, "
            + SQLiteOmemoStore.IDENTITY_KEY + " TEXT, UNIQUE("
            + SQLiteOmemoStore.BARE_JID + ", " + SQLiteOmemoStore.DEVICE_ID
            + ") ON CONFLICT REPLACE);";

    // Create session table
    public static String CREATE_SESSIONS_STATEMENT = "CREATE TABLE "
            + SQLiteOmemoStore.SESSION_TABLE_NAME + "("
            + SQLiteOmemoStore.BARE_JID + " TEXT, "
            + SQLiteOmemoStore.DEVICE_ID + " INTEGER, "
            + SQLiteOmemoStore.SESSION_KEY + " TEXT, UNIQUE("
            + SQLiteOmemoStore.BARE_JID + ", " + SQLiteOmemoStore.DEVICE_ID
            + ") ON CONFLICT REPLACE);";

    // Chat session information table
    public static String CREATE_CHAT_SESSIONS_STATEMENT = "CREATE TABLE "
            + ChatSession.TABLE_NAME + " ("
            + ChatSession.SESSION_UUID + " TEXT PRIMARY KEY, "
            + ChatSession.ACCOUNT_UUID + " TEXT, "
            + ChatSession.ACCOUNT_UID + " TEXT, "
            + ChatSession.ENTITY_JID + " TEXT, "
            + ChatSession.CREATED + " NUMBER, "
            + ChatSession.STATUS + " NUMBER DEFAULT " + ChatSlice.MSGTYPE_OMEMO + ", "
            + ChatSession.MODE + " NUMBER, "
            + ChatSession.MAM_DATE + " NUMBER DEFAULT " + new Date().getTime() + ", "
            + ChatSession.ATTRIBUTES + " TEXT, FOREIGN KEY("
            + ChatSession.ACCOUNT_UUID + ") REFERENCES "
            + AccountID.TABLE_NAME + "(" + AccountID.ACCOUNT_UUID
            + ") ON DELETE CASCADE, UNIQUE(" + ChatSession.ACCOUNT_UUID
            + ", " + ChatSession.ENTITY_JID
            + ") ON CONFLICT REPLACE);";

    public static String CREATE_ENTITY_CAPS_STATEMENT = "CREATE TABLE "
            + EntityCapsCache.TABLE_NAME + "("
            + EntityCapsCache.ENTITY_NODE_VER + " TEXT, "
            + EntityCapsCache.ENTITY_DISC_INFO + " TEXT, UNIQUE ("
            + EntityCapsCache.ENTITY_NODE_VER
            + ") ON CONFLICT REPLACE);";

    private DatabaseBackend(Context context) {
        // super(context, DATABASE_NAME, null, DATABASE_VERSION);
        super(context);
        initDb(context);
    }

    /**
     * Get an instance of the DataBaseBackend and create one if new
     *
     * @param context context
     *
     * @return DatabaseBackend instance
     */
    public static synchronized DatabaseBackend getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseBackend(context);
        }
        return instance;
    }

    private void initDb(Context context) {
        StoreConfig config = StoreConfig.newDefaultConfig(DATABASE_NAME);

        final RdbOpenCallback rdbCallback = new RdbOpenCallback() {
            /**
             * Create all the required virgin database tables and perform initial data migration
             * a. System properties
             * b. Account Tables: accountID & accountProperties
             * c. Group Tables: metaContactGroup & childContacts
             * d. contacts
             * e. chatSessions
             * f. chatMessages
             * g. callHistory
             * f. recentMessages
     * i. Entity Caps
     * j. Axolotl tables: identities, sessions, preKeys, signed_preKeys
             * <p>
             * # Initialize and initial data migration
             *
             * @param store SQLite database
             */
            @Override
            public void onCreate(RdbStore store) {
                // store.execSQL("PRAGMA foreign_keys=ON;");
                String query = String.format("PRAGMA foreign_keys =%s", "ON");
                store.executeSql(query);

                // System properties table
                store.executeSql("CREATE TABLE " + SQLiteConfigurationStore.TABLE_NAME + "("
                        + SQLiteConfigurationStore.COLUMN_NAME + " TEXT PRIMARY KEY, "
                        + SQLiteConfigurationStore.COLUMN_VALUE + " TEXT, UNIQUE("
                        + SQLiteConfigurationStore.COLUMN_NAME
                        + ") ON CONFLICT REPLACE);");

                // Account info table
                store.executeSql("CREATE TABLE " + AccountID.TABLE_NAME + "("
                        + AccountID.ACCOUNT_UUID + " TEXT PRIMARY KEY, "
                        + AccountID.PROTOCOL + " TEXT DEFAULT " + AccountID.PROTOCOL_DEFAULT + ", "
                        + AccountID.USER_ID + " TEXT, "
                        + AccountID.ACCOUNT_UID + " TEXT, "
                        + AccountID.KEYS + " TEXT, UNIQUE(" + AccountID.ACCOUNT_UID
                        + ") ON CONFLICT REPLACE);");

                // Account properties table
                store.executeSql("CREATE TABLE " + AccountID.TBL_PROPERTIES + "("
                        + AccountID.ACCOUNT_UUID + " TEXT, "
                        + AccountID.COLUMN_NAME + " TEXT, "
                        + AccountID.COLUMN_VALUE + " TEXT, PRIMARY KEY("
                        + AccountID.ACCOUNT_UUID + ", "
                        + AccountID.COLUMN_NAME + "), FOREIGN KEY("
                        + AccountID.ACCOUNT_UUID + ") REFERENCES "
                        + AccountID.TABLE_NAME + "(" + AccountID.ACCOUNT_UUID
                        + ") ON DELETE CASCADE);");

                // Meta contact groups table
                store.executeSql("CREATE TABLE " + MetaContactGroup.TABLE_NAME + "("
                        + MetaContactGroup.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + MetaContactGroup.ACCOUNT_UUID + " TEXT, "
                        + MetaContactGroup.MC_GROUP_NAME + " TEXT, "
                        + MetaContactGroup.MC_GROUP_UID + " TEXT, "
                        + MetaContactGroup.PARENT_PROTO_GROUP_UID + " TEXT, "
                        + MetaContactGroup.PROTO_GROUP_UID + " TEXT, "
                        + MetaContactGroup.PERSISTENT_DATA + " TEXT, FOREIGN KEY("
                        + MetaContactGroup.ACCOUNT_UUID + ") REFERENCES "
                        + AccountID.TABLE_NAME + "(" + AccountID.ACCOUNT_UUID
                        + ") ON DELETE CASCADE, UNIQUE(" + MetaContactGroup.ACCOUNT_UUID + ", "
                        + MetaContactGroup.MC_GROUP_UID + ", " + MetaContactGroup.PARENT_PROTO_GROUP_UID
                        + ") ON CONFLICT REPLACE);");

                /*
                 * Meta contact group members table. The entries in the table are linked to the
                 * MetaContactGroup.TABLE_NAME each entry by ACCOUNT_UUID && PROTO_GROUP_UID
                 */
                store.executeSql("CREATE TABLE " + MetaContactGroup.TBL_CHILD_CONTACTS + "("
                        + MetaContactGroup.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + MetaContactGroup.MC_UID + " TEXT, "
                        + MetaContactGroup.ACCOUNT_UUID + " TEXT, "
                        + MetaContactGroup.PROTO_GROUP_UID + " TEXT, "
                        + MetaContactGroup.CONTACT_JID + " TEXT, "
                        + MetaContactGroup.MC_DISPLAY_NAME + " TEXT, "
                        + MetaContactGroup.MC_USER_DEFINED + " TEXT DEFAULT 'false',"
                        + MetaContactGroup.PERSISTENT_DATA + " TEXT, "
                        + MetaContactGroup.MC_DETAILS + " TEXT, FOREIGN KEY("
                        + MetaContactGroup.ACCOUNT_UUID + ") REFERENCES "
                        + AccountID.TABLE_NAME + "(" + AccountID.ACCOUNT_UUID
                        + ") ON DELETE CASCADE, UNIQUE(" + MetaContactGroup.ACCOUNT_UUID + ", "
                        + MetaContactGroup.PROTO_GROUP_UID + ", " + MetaContactGroup.CONTACT_JID
                        + ") ON CONFLICT REPLACE);");

                // Contacts information table
                store.executeSql("CREATE TABLE " + Contact.TABLE_NAME + "("
                        + Contact.CONTACT_UUID + " TEXT PRIMARY KEY, "
                        + Contact.PROTOCOL_PROVIDER + " TEXT, "
                        + Contact.CONTACT_JID + " TEXT, "
                        + Contact.SVR_DISPLAY_NAME + " TEXT, "
                        + Contact.OPTIONS + " NUMBER, "
                        + Contact.PHOTO_URI + " TEXT, "
                        + Contact.AVATAR_HASH + " TEXT, "
                        + Contact.LAST_PRESENCE + " TEXT, "
                        + Contact.PRESENCE_STATUS + " INTEGER, "
                        + Contact.LAST_SEEN + " NUMBER,"
                        + Contact.KEYS + " TEXT, UNIQUE("
                        + Contact.PROTOCOL_PROVIDER + ", " + Contact.CONTACT_JID
                        + ") ON CONFLICT IGNORE);");

                // Chat session information table
                store.executeSql(CREATE_CHAT_SESSIONS_STATEMENT);

                // chat / MUC message table
                store.executeSql("CREATE TABLE " + ChatMessage.TABLE_NAME + "( "
                        + ChatMessage.UUID + " TEXT, "
                        + ChatMessage.SESSION_UUID + " TEXT, "
                        + ChatMessage.TIME_STAMP + " NUMBER, "
                        + ChatMessage.ENTITY_JID + " TEXT,"
                        + ChatMessage.JID + " TEXT, "
                        + ChatMessage.MSG_BODY + " TEXT, "
                        + ChatMessage.ENC_TYPE + " TEXT, "
                        + ChatMessage.MSG_TYPE + " TEXT, "
                        + ChatMessage.DIRECTION + " TEXT, "
                        + ChatMessage.STATUS + " TEXT,"
                        + ChatMessage.FILE_PATH + " TEXT, "
                        + ChatMessage.FINGERPRINT + " TEXT, "
                        + ChatMessage.STEALTH_TIMER + "  INTEGER DEFAULT 0, "
                        + ChatMessage.CARBON + " INTEGER DEFAULT 0, "
                        + ChatMessage.READ + " INTEGER DEFAULT 0, "
                        + ChatMessage.OOB + " INTEGER DEFAULT 0, "
                        + ChatMessage.ERROR_MSG + " TEXT, "
                        + ChatMessage.SERVER_MSG_ID + " TEXT, "
                        + ChatMessage.REMOTE_MSG_ID + " TEXT, FOREIGN KEY("
                        + ChatMessage.SESSION_UUID + ") REFERENCES "
                        + ChatSession.TABLE_NAME + "(" + ChatSession.SESSION_UUID
                        + ") ON DELETE CASCADE, UNIQUE(" + ChatMessage.UUID
                        + ") ON CONFLICT REPLACE);");

                // Call history table
                store.executeSql("CREATE TABLE " + CallHistoryService.TABLE_NAME + " ("
                        + CallHistoryService.UUID + " TEXT PRIMARY KEY, "
                        + CallHistoryService.TIME_STAMP + " NUMBER, "
                        + CallHistoryService.ACCOUNT_UID + " TEXT, "
                        + CallHistoryService.CALL_START + " NUMBER, "
                        + CallHistoryService.CALL_END + " NUMBER, "
                        + CallHistoryService.DIRECTION + " TEXT, "
                        + CallHistoryService.ENTITY_FULL_JID + " TEXT, "
                        + CallHistoryService.ENTITY_CALL_START + " NUMBER, "
                        + CallHistoryService.ENTITY_CALL_END + " NUMBER, "
                        + CallHistoryService.ENTITY_CALL_STATE + " TEXT, "
                        + CallHistoryService.CALL_END_REASON + " TEXT, "
                        + CallHistoryService.ENTITY_JID + " TEXT, "
                        + CallHistoryService.SEC_ENTITY_ID + " TEXT, FOREIGN KEY("
                        + CallHistoryService.ACCOUNT_UID + ") REFERENCES "
                        + AccountID.TABLE_NAME + "(" + AccountID.ACCOUNT_UID
                        + ") ON DELETE CASCADE);");

                // Recent message table
                store.executeSql("CREATE TABLE " + MessageSourceService.TABLE_NAME + " ("
                        + MessageSourceService.UUID + " TEXT PRIMARY KEY, "
                        + MessageSourceService.ACCOUNT_UID + " TEXT, "
                        + MessageSourceService.ENTITY_JID + " TEXT, "
                        + MessageSourceService.TIME_STAMP + " NUMBER, "
                        + MessageSourceService.VERSION + " TEXT, FOREIGN KEY("
                        + MessageSourceService.ACCOUNT_UID + ") REFERENCES "
                        + AccountID.TABLE_NAME + "(" + AccountID.ACCOUNT_UID
                        + ") ON DELETE CASCADE);");

                // Create Entity Caps DB
                store.executeSql(CREATE_ENTITY_CAPS_STATEMENT);

                // Create all relevant tables for OMEMO support
                store.executeSql(CREATE_OMEMO_DEVICES_STATEMENT);
                store.executeSql(CREATE_PREKEYS_STATEMENT);
                store.executeSql(CREATE_SIGNED_PREKEYS_STATEMENT);
                store.executeSql(CREATE_IDENTITIES_STATEMENT);
                store.executeSql(CREATE_SESSIONS_STATEMENT);

                // Perform the first data migration to SQLite database
                initDatabase(store);
            }

            @Override
            public void onUpgrade(final RdbStore store, int oldVersion, int newVersion) {
                // Timber.i("Upgrading database from version %s to version %s", oldVersion, newVersion);
                HiLog.info(LABEL, "Upgrading database from version %{public} to version %{public}", oldVersion, newVersion);

                store.beginTransaction();
                try {
                    // cmeng: mProvider == null currently not use - must fixed if use
                    mProvider = null;
                    RealMigrationsHelper migrationsHelper = new RealMigrationsHelper(mProvider);
                    Migrations.upgradeDatabase(store, migrationsHelper);
                    // store.setTransactionSuccessful();
                } catch (Exception e) {
                    // Timber.e("Exception while upgrading database. Resetting the DB to original: %s", e.getMessage());
                    HiLog.error(LABEL, "Exception while upgrading database. Resetting the DB to original: %{public}", e.getMessage());
                    store.setVersion(oldVersion);

                    if (BuildConfig.DEBUG) {
                        store.endTransaction();
                        throw new Error("Database upgrade failed! Exception: ", e);
                    }
                } finally {
                    store.endTransaction();
                }
            }
        };

        DatabaseHelper helper = new DatabaseHelper(context);
        mRdbStore = helper.getRdbStore(config, DATABASE_VERSION, rdbCallback, null);
    }

    public static RdbStore getRdbStore() {
        return mRdbStore;
    }

    /**
     * Initialize, migrate and fill the database from old data implementation
     */
    private void initDatabase(RdbStore store) {
        Timber.i("### Starting Database migration! ###");
        store.beginTransaction();
        try {
            // store.setTransactionSuccessful();
            Timber.i("### Completed SQLite DataBase migration successfully! ###");
        } finally {
            store.endTransaction();
        }
    }

    /**
     * Create or update the AccountID table for a specified accountId
     *
     * @param accountId AccountID to be replaced/inserted
     */
    public void createAccount(AccountID accountId) {
        mRdbStore.replace(AccountID.TABLE_NAME, accountId.getValuesBucket());
    }

    public List<String> getAllAccountIDs() {
        List<String> userIDs = new ArrayList<>();
        String[] columns = {AccountID.USER_ID};

        RdbPredicates rdbPredicates = new RdbPredicates(AccountID.TABLE_NAME);
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);
        while (resultSet.goToNextRow()) {
            userIDs.add(resultSet.getString(0));
        }
        resultSet.close();
        return userIDs;
    }

    public List<AccountID> getAccounts(ProtocolProviderFactory factory) {
        List<AccountID> accountIDs = new ArrayList<>();

        RdbPredicates rdbPredicates = new RdbPredicates(AccountID.TABLE_NAME)
                .equalTo(AccountID.PROTOCOL, factory.getProtocolName());
        ResultSet resultSet = mRdbStore.query(rdbPredicates, null);

        while (resultSet.goToNextRow()) {
            accountIDs.add(AccountID.fromResultSet(mRdbStore, resultSet, factory));
        }
        resultSet.close();
        return accountIDs;
    }

    public boolean updateAccount(AccountID accountId) {
        RdbPredicates rdbPredicates = new RdbPredicates(AccountID.TABLE_NAME)
                .equalTo(AccountID.ACCOUNT_UUID, accountId.getAccountUuid());

        final int rows = mRdbStore.update(accountId.getValuesBucket(), rdbPredicates);
        return (rows == 1);
    }

    public boolean deleteAccount(AccountID accountId) {
        RdbPredicates rdbPredicates = new RdbPredicates(AccountID.TABLE_NAME)
                .equalTo(AccountID.ACCOUNT_UUID, accountId.getAccountUuid());

        final int rows = mRdbStore.delete(rdbPredicates);
        return (rows == 1);
    }

    // ========= OMEMO Devices =========
    public SortedSet<Integer> loadDeviceIdsOf(BareJid user) {
        SortedSet<Integer> deviceIds = new TreeSet<>();
        int registrationId;
        String[] columns = {SQLiteOmemoStore.OMEMO_REG_ID};

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.OMEMO_JID, user.toString())
                .orderByAsc(SQLiteOmemoStore.OMEMO_REG_ID);
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        while (resultSet.goToNextRow()) {
            registrationId = resultSet.getInt(0);
            deviceIds.add(registrationId);
        }
        resultSet.close();
        return deviceIds;
    }

    public HashMap<String, Integer> loadAllOmemoRegIds() {
        HashMap<String, Integer> registrationIds = new HashMap<>();
        String[] columns = {SQLiteOmemoStore.OMEMO_JID, SQLiteOmemoStore.OMEMO_REG_ID};

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME);
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        while (resultSet.goToNextRow()) {
            registrationIds.put(resultSet.getString(0), resultSet.getInt(1));
        }
        resultSet.close();
        return registrationIds;
    }

    public void storeOmemoRegId(BareJid user, int defaultDeviceId) {
        ValuesBucket values = new ValuesBucket();
        values.putString(SQLiteOmemoStore.OMEMO_JID, user.toString());
        values.putInteger(SQLiteOmemoStore.OMEMO_REG_ID, defaultDeviceId);
        values.putInteger(SQLiteOmemoStore.CURRENT_SIGNED_PREKEY_ID, 0);

        long row = mRdbStore.insert(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME, values);
        if (row > 0)
            Timber.i("### Omemo device added for: %s; %s", user, defaultDeviceId);
        else
            Timber.e("### Error in creating Omemo device for: %s: %s", user, defaultDeviceId);
    }

    public int loadCurrentSignedPKeyId(OmemoManager omemoManager) {
        int currentSignedPKeyId = getCurrentSignedPreKeyId(omemoManager);
        OmemoDevice device = omemoManager.getOwnDevice();

        String[] columns = {SQLiteOmemoStore.CURRENT_SIGNED_PREKEY_ID};
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.OMEMO_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.OMEMO_REG_ID, device.getDeviceId());
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            currentSignedPKeyId = resultSet.getInt(resultSet.getColumnIndexForName(SQLiteOmemoStore.CURRENT_SIGNED_PREKEY_ID));
        }
        resultSet.close();
        return currentSignedPKeyId;
    }

    public void storeCurrentSignedPKeyId(OmemoManager omemoManager, int currentSignedPreKeyId) {
        OmemoDevice device = omemoManager.getOwnDevice();

        ValuesBucket values = new ValuesBucket();
        values.putString(SQLiteOmemoStore.OMEMO_JID, device.getJid().toString());
        values.putInteger(SQLiteOmemoStore.OMEMO_REG_ID, device.getDeviceId());
        values.putInteger(SQLiteOmemoStore.CURRENT_SIGNED_PREKEY_ID, currentSignedPreKeyId);

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.OMEMO_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.OMEMO_REG_ID, device.getDeviceId());

        int row = mRdbStore.update(values, rdbPredicates);
        if (row == 0) {
            mRdbStore.insert(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME, values);
        }
    }

    // cmeng: encountered getLastPreKeyId not equal to store lastPreKey causing omemo msg problem!
    // To reset stored lastPreKey???
    public int loadLastPreKeyId(OmemoManager omemoManager) {
        int lastPKeyId = getLastPreKeyId(omemoManager);
        OmemoDevice device = omemoManager.getOwnDevice();

        String[] columns = {SQLiteOmemoStore.LAST_PREKEY_ID};
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.OMEMO_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.OMEMO_REG_ID, device.getDeviceId());
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            lastPKeyId = resultSet.getInt(resultSet.getColumnIndexForName(SQLiteOmemoStore.LAST_PREKEY_ID));
        }
        resultSet.close();
        return lastPKeyId;
    }

    public void storeLastPreKeyId(OmemoManager omemoManager, int lastPreKeyId) {
        OmemoDevice device = omemoManager.getOwnDevice();

        ValuesBucket values = new ValuesBucket();
        values.putString(SQLiteOmemoStore.OMEMO_JID, device.getJid().toString());
        values.putInteger(SQLiteOmemoStore.OMEMO_REG_ID, device.getDeviceId());
        values.putInteger(SQLiteOmemoStore.LAST_PREKEY_ID, lastPreKeyId);

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.OMEMO_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.OMEMO_REG_ID, device.getDeviceId());
        int row = mRdbStore.update(values, rdbPredicates);

        if (row == 0) {
            mRdbStore.insert(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME, values);
        }
    }

    // ========= OMEMO PreKey =========
    private ResultSet getResultSetForPreKey(OmemoDevice userDevice, int preKeyId) {
        String[] columns = {SQLiteOmemoStore.PRE_KEYS};

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, userDevice.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, userDevice.getDeviceId())
                .and().equalTo(SQLiteOmemoStore.PRE_KEY_ID, preKeyId);
        return mRdbStore.query(rdbPredicates, columns);
    }

    public TreeMap<Integer, PreKeyRecord> loadPreKeys(OmemoDevice userDevice) {
        int preKeyId;
        PreKeyRecord preKeyRecord;
        TreeMap<Integer, PreKeyRecord> PreKeyRecords = new TreeMap<>();
        String[] columns = {SQLiteOmemoStore.PRE_KEY_ID, SQLiteOmemoStore.PRE_KEYS};

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, userDevice.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, userDevice.getDeviceId())
                .orderByAsc(SQLiteOmemoStore.PRE_KEY_ID);
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        while (resultSet.goToNextRow()) {
            preKeyId = resultSet.getInt(0);
            try {
                preKeyRecord = new PreKeyRecord(Base64.getDecoder().decode(resultSet.getString(1)));
                PreKeyRecords.put(preKeyId, preKeyRecord);
            } catch (IOException e) {
                Timber.w("Failed to deserialize preKey from store preky: %s: %s", preKeyId, e.getMessage());
            }
        }
        resultSet.close();
        return PreKeyRecords;
    }

    public PreKeyRecord loadPreKey(OmemoDevice userDevice, int preKeyId) {
        PreKeyRecord record = null;
        ResultSet resultSet = getResultSetForPreKey(userDevice, preKeyId);
        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            try {
                record = new PreKeyRecord(Base64.getDecoder().decode(
                        resultSet.getString(resultSet.getColumnIndexForName(SQLiteOmemoStore.PRE_KEYS))));
            } catch (IOException e) {
                Timber.w("Failed to deserialize preKey from mStore. %s", e.getMessage());
            }
        }
        resultSet.close();
        return record;
    }

    public void storePreKey(OmemoDevice userDevice, int preKeyId, PreKeyRecord record) {
        ValuesBucket values = new ValuesBucket();
        values.putString(SQLiteOmemoStore.BARE_JID, userDevice.getJid().toString());
        values.putInteger(SQLiteOmemoStore.DEVICE_ID, userDevice.getDeviceId());
        values.putInteger(SQLiteOmemoStore.PRE_KEY_ID, preKeyId);
        values.putString(SQLiteOmemoStore.PRE_KEYS, Base64.getEncoder().encodeToString(record.serialize()));
        mRdbStore.insert(SQLiteOmemoStore.PREKEY_TABLE_NAME, values);
    }

    public void deletePreKey(OmemoDevice userDevice, int preKeyId) {
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, userDevice.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, userDevice.getDeviceId())
                .and().equalTo(SQLiteOmemoStore.PRE_KEY_ID, preKeyId);
        mRdbStore.delete(rdbPredicates);
    }

    public int getLastPreKeyId(OmemoManager omemoManager) {
        int lastPreKeyId = 0;
        OmemoDevice device = omemoManager.getOwnDevice();

        String[] columns = {SQLiteOmemoStore.PRE_KEY_ID};
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId())
                .orderByDesc(SQLiteOmemoStore.PRE_KEY_ID)
                .limit(1);
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            lastPreKeyId = resultSet.getInt(resultSet.getColumnIndexForName(SQLiteOmemoStore.PRE_KEY_ID));
        }
        resultSet.close();
        return lastPreKeyId;
    }

    // ========= OMEMO Signed PreKey =========
    private ResultSet getResultSetForSignedPreKey(OmemoDevice userDevice, int signedPreKeyId) {
        String[] columns = {SQLiteOmemoStore.SIGNED_PRE_KEYS};
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, userDevice.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, userDevice.getDeviceId())
                .and().equalTo(SQLiteOmemoStore.SIGNED_PRE_KEY_ID, signedPreKeyId);

        return mRdbStore.query(rdbPredicates, columns);
    }

    public SignedPreKeyRecord loadSignedPreKey(OmemoDevice userDevice, int signedPreKeyId) {
        SignedPreKeyRecord record = null;
        ResultSet resultSet = getResultSetForSignedPreKey(userDevice, signedPreKeyId);
        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            try {
                record = new SignedPreKeyRecord(Base64.getDecoder().decode(
                        resultSet.getString(resultSet.getColumnIndexForName(SQLiteOmemoStore.SIGNED_PRE_KEYS))));
            } catch (IOException e) {
                Timber.w("Could not deserialize signed preKey for %s: %s", userDevice, e.getMessage());
            }
        }
        resultSet.close();
        return record;
    }

    public TreeMap<Integer, SignedPreKeyRecord> loadSignedPreKeys(OmemoDevice device) {
        int preKeyId;
        SignedPreKeyRecord signedPreKeysRecord;
        TreeMap<Integer, SignedPreKeyRecord> preKeys = new TreeMap<>();

        String[] columns = {SQLiteOmemoStore.SIGNED_PRE_KEY_ID, SQLiteOmemoStore.SIGNED_PRE_KEYS};
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId());
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        while (resultSet.goToNextRow()) {
            try {
                preKeyId = resultSet.getInt(resultSet.getColumnIndexForName(SQLiteOmemoStore.SIGNED_PRE_KEY_ID));
                signedPreKeysRecord = new SignedPreKeyRecord(Base64.getDecoder().decode(resultSet.getString(
                        resultSet.getColumnIndexForName(SQLiteOmemoStore.SIGNED_PRE_KEYS))));
                preKeys.put(preKeyId, signedPreKeysRecord);
            } catch (IOException e) {
                Timber.w("Could not deserialize signed preKey for %s: %s", device, e.getMessage());
            }
        }
        resultSet.close();
        return preKeys;
    }

    public void storeSignedPreKey(OmemoDevice device, int signedPreKeyId, SignedPreKeyRecord record) {
        ValuesBucket values = new ValuesBucket();
        values.putString(SQLiteOmemoStore.BARE_JID, device.getJid().toString());
        values.putInteger(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId());
        values.putInteger(SQLiteOmemoStore.SIGNED_PRE_KEY_ID, signedPreKeyId);
        values.putString(SQLiteOmemoStore.SIGNED_PRE_KEYS, Base64.getEncoder().encodeToString(record.serialize()));
        values.putLong(SQLiteOmemoStore.LAST_RENEWAL_DATE, record.getTimestamp());
        mRdbStore.insert(SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME, values);
    }

    public void deleteSignedPreKey(OmemoDevice userDevice, int signedPreKeyId) {
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, userDevice.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, userDevice.getDeviceId())
                .and().equalTo(SQLiteOmemoStore.SIGNED_PRE_KEY_ID, signedPreKeyId);
        ;
        mRdbStore.delete(rdbPredicates);
    }

    public void setLastSignedPreKeyRenewal(OmemoDevice userDevice, Date date) {
        ValuesBucket values = new ValuesBucket();
        values.putLong(SQLiteOmemoStore.LAST_RENEWAL_DATE, date.getTime());

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, userDevice.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, userDevice.getDeviceId());
        mRdbStore.update(values, rdbPredicates);
    }

    public Date getLastSignedPreKeyRenewal(OmemoDevice userDevice) {
        String[] columns = {SQLiteOmemoStore.LAST_RENEWAL_DATE};
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, userDevice.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, userDevice.getDeviceId());
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            Long ts = resultSet.getLong(resultSet.getColumnIndexForName(SQLiteOmemoStore.LAST_RENEWAL_DATE));
            resultSet.close();
            return (ts != null && ts > 0) ? new Date(ts) : null;
        }
        return null;
    }

    private int getCurrentSignedPreKeyId(OmemoManager omemoManager) {
        int currentSignedPKId = 1;
        OmemoDevice device = omemoManager.getOwnDevice();

        String[] columns = {SQLiteOmemoStore.SIGNED_PRE_KEY_ID};
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId());
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            currentSignedPKId = resultSet.getInt(resultSet.getColumnIndexForName(SQLiteOmemoStore.SIGNED_PRE_KEY_ID));
        }
        resultSet.close();
        return currentSignedPKId;
    }

    // ========= OMEMO Identity =========
    private ResultSet getIdentityKeyResultSet(OmemoDevice device, String fingerprint) {
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId());
        if (fingerprint != null) {
            rdbPredicates.and().equalTo(SQLiteOmemoStore.FINGERPRINT, fingerprint);
        }
        return mRdbStore.query(rdbPredicates, null);
    }

    public IdentityKeyPair loadIdentityKeyPair(OmemoDevice device)
            throws CorruptedOmemoKeyException {
        IdentityKeyPair identityKeyPair = null;
        ResultSet resultSet = getIdentityKeyResultSet(device, null);
        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            String identityKP = resultSet.getString(resultSet.getColumnIndexForName(SQLiteOmemoStore.IDENTITY_KEY));
            resultSet.close();
            try {
                if (StringUtils.isNotEmpty(identityKP)) {
                    identityKeyPair = new IdentityKeyPair(Base64.getDecoder().decode(identityKP));
                }
            } catch (InvalidKeyException e) {
                // deleteIdentityKey(device); // may corrupt DB and out of sync with other data
                String msg = aTalkApp.getResString(ResourceTable.String_omemo_identity_keypairs_invalid, device, e.getMessage());
                throw new CorruptedOmemoKeyException(msg);
            }
        }
        return identityKeyPair;
    }

    public IdentityKey loadIdentityKey(OmemoDevice device)
            throws CorruptedOmemoKeyException {
        IdentityKey identityKey = null;
        ResultSet resultSet = getIdentityKeyResultSet(device, null);
        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            String key = resultSet.getString(resultSet.getColumnIndexForName(SQLiteOmemoStore.IDENTITY_KEY));
            resultSet.close();
            try {
                if (StringUtils.isNotEmpty(key)) {
                    identityKey = new IdentityKey(Base64.getDecoder().decode(key), 0);
                }
            } catch (InvalidKeyException e) {
                // Delete corrupted identityKey, let omemo rebuilt this
                deleteIdentityKey(device);
                String msg = aTalkApp.getResString(ResourceTable.String_omemo_identity_key_invalid, device, e.getMessage());
                throw new CorruptedOmemoKeyException(msg);
            }
        }
        return identityKey;
    }

    // Use this to delete the device corrupted identityKeyPair/identityKey
    // - Later identityKeyPair gets rebuilt when device restart
    public void deleteIdentityKey(OmemoDevice device) {
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId());
        mRdbStore.delete(rdbPredicates);
    }

    public void storeIdentityKeyPair(OmemoDevice userDevice, IdentityKeyPair identityKeyPair, String fingerprint) {
        storeIdentityKey(userDevice, fingerprint, Base64.getEncoder().encodeToString(identityKeyPair.serialize()),
                FingerprintStatus.createActiveVerified(false));
    }

    public void storeIdentityKey(OmemoDevice device, IdentityKey identityKey, String fingerprint, FingerprintStatus status) {
        storeIdentityKey(device, fingerprint, Base64.getEncoder().encodeToString(identityKey.serialize()), status);
    }

    private void storeIdentityKey(OmemoDevice device, String fingerprint, String base64Serialized, FingerprintStatus status) {
        String bareJid = device.getJid().toString();
        int deviceId = device.getDeviceId();

        ValuesBucket values = new ValuesBucket();
        values.putString(SQLiteOmemoStore.BARE_JID, bareJid);
        values.putInteger(SQLiteOmemoStore.DEVICE_ID, deviceId);
        values.putString(SQLiteOmemoStore.FINGERPRINT, fingerprint);
        values.putString(SQLiteOmemoStore.IDENTITY_KEY, base64Serialized);
        values.putValues(status.toValuesBucket());

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, bareJid)
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, deviceId);
        int rows = mRdbStore.update(values, rdbPredicates);
        if (rows == 0) {
            mRdbStore.insert(SQLiteOmemoStore.IDENTITIES_TABLE_NAME, values);
        }
    }

    public Set<IdentityKey> loadIdentityKeys(OmemoDevice device) {
        return loadIdentityKeys(device, null);
    }

    public Set<IdentityKey> loadIdentityKeys(OmemoDevice device, FingerprintStatus status) {
        Set<IdentityKey> identityKeys = new HashSet<>();
        String key;
        ResultSet resultSet = getIdentityKeyResultSet(device, null);

        while (resultSet.goToNextRow()) {
            if (status != null && !status.equals(FingerprintStatus.fromResultSet(resultSet))) {
                continue;
            }
            try {
                key = resultSet.getString(resultSet.getColumnIndexForName(SQLiteOmemoStore.IDENTITY_KEY));
                if (StringUtils.isNotEmpty(key)) {
                    identityKeys.add(new IdentityKey(Base64.getDecoder().decode(key), 0));
                } else {
                    Timber.d("Missing key (possibly pre-verified) in database for account: %s", device.getJid());
                }
            } catch (InvalidKeyException e) {
                Timber.d("Encountered invalid IdentityKey in DB for omemoDevice: %s", device);
            }
        }
        resultSet.close();
        return identityKeys;
    }

    public OmemoCachedDeviceList loadCachedDeviceList(BareJid contact) {
        if (contact == null) {
            return null;
        }

        OmemoCachedDeviceList cachedDeviceList = new OmemoCachedDeviceList();
        String[] columns = {SQLiteOmemoStore.DEVICE_ID, SQLiteOmemoStore.ACTIVE};
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, contact.toString());
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        Set<Integer> activeDevices = cachedDeviceList.getActiveDevices();
        Set<Integer> inActiveDevices = cachedDeviceList.getInactiveDevices();
        while (resultSet.goToNextRow()) {
            int deviceId = resultSet.getInt(resultSet.getColumnIndexForName(SQLiteOmemoStore.DEVICE_ID));
            if (resultSet.getInt(resultSet.getColumnIndexForName(SQLiteOmemoStore.ACTIVE)) == 1) {
                activeDevices.add(deviceId);
            } else {
                inActiveDevices.add(deviceId);
            }
        }
        resultSet.close();
        return cachedDeviceList;
    }

    public void storeCachedDeviceList(OmemoDevice userDevice, BareJid contact, OmemoCachedDeviceList deviceList) {
        if (contact == null) {
            return;
        }

        // Active devices
        ValuesBucket values = new ValuesBucket();
        values.putInteger(SQLiteOmemoStore.ACTIVE, 1);
        Set<Integer> activeDevices = deviceList.getActiveDevices();
        // Timber.d("Identities table - updating for activeDevice: %s:%s", contact, activeDevices);
        for (int deviceId : activeDevices) {
            RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                    .equalTo(SQLiteOmemoStore.BARE_JID, contact.toString())
                    .and().equalTo(SQLiteOmemoStore.DEVICE_ID, deviceId);
            int row = mRdbStore.update(values, rdbPredicates);

            if (row == 0) {
                /*
                 * Just logged the error. Any missing buddy identityKey will be handled by
                 * AndroidOmemoService#buddyDeviceListUpdateListener()
                 */
                Timber.d("Identities table - create new activeDevice: %s:%s ", contact, deviceId);
                values.putString(SQLiteOmemoStore.BARE_JID, contact.toString());
                values.putInteger(SQLiteOmemoStore.DEVICE_ID, deviceId);
                mRdbStore.insert(SQLiteOmemoStore.IDENTITIES_TABLE_NAME, values);
            }
        }

        /*
         * Inactive devices:
         * Important: Must clear values before use, otherwise update exiting deviceID with new deviceID but still keeping
         * old fingerPrint and identityKey. This forbids update of the fingerPrint and IdentityKey for the new deviceID,
         * Worst it causes aTalk to crash on next access to omemo chat with the identity
         */
        values.clear();
        values.putInteger(SQLiteOmemoStore.ACTIVE, 0);
        Set<Integer> inActiveDevices = deviceList.getInactiveDevices();
        // Timber.i("Identities table updated for inactiveDevice: %s:%s", contact, inActiveDevices);
        for (int deviceId : inActiveDevices) {
            RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                    .equalTo(SQLiteOmemoStore.BARE_JID, contact.toString())
                    .and().equalTo(SQLiteOmemoStore.DEVICE_ID, deviceId);
            int row = mRdbStore.update(values, rdbPredicates);

            if (row == 0) {
                Timber.w("Identities table contains no inactiveDevice (create new): %s:%s", contact, deviceId);
                values.putString(SQLiteOmemoStore.BARE_JID, contact.toString());
                values.putInteger(SQLiteOmemoStore.DEVICE_ID, deviceId);
                mRdbStore.insert(SQLiteOmemoStore.IDENTITIES_TABLE_NAME, values);
            }
        }
    }

    public int deleteNullIdentityKeyDevices() {
        return mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .isNull(SQLiteOmemoStore.IDENTITY_KEY));
    }

    public void setLastDeviceIdPublicationDate(OmemoDevice device, Date date) {
        ValuesBucket values = new ValuesBucket();
        values.putLong(SQLiteOmemoStore.LAST_MESSAGE_RX, date.getTime());

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId());
        mRdbStore.update(values, rdbPredicates);
    }

    public Date getLastDeviceIdPublicationDate(OmemoDevice device) {
        ResultSet resultSet = getIdentityKeyResultSet(device, null);
        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            Long ts = resultSet.getLong(resultSet.getColumnIndexForName(SQLiteOmemoStore.LAST_MESSAGE_RX));
            resultSet.close();
            return (ts != null && ts > 0) ? new Date(ts) : null;
        }
        return null;
    }

    public void setLastMessageReceiveDate(OmemoDevice device, Date date) {
        ValuesBucket values = new ValuesBucket();
        values.putLong(SQLiteOmemoStore.LAST_MESSAGE_RX, date.getTime());

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId());
        mRdbStore.update(values, rdbPredicates);
    }

    public Date getLastMessageReceiveDate(OmemoDevice device) {
        ResultSet resultSet = getIdentityKeyResultSet(device, null);
        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            long ts = resultSet.getLong(resultSet.getColumnIndexForName(SQLiteOmemoStore.LAST_MESSAGE_RX));
            resultSet.close();
            return (ts > 0) ? new Date(ts) : null;
        }
        return null;
    }

    public void setOmemoMessageCounter(OmemoDevice device, int count) {
        ValuesBucket values = new ValuesBucket();
        values.putInteger(SQLiteOmemoStore.MESSAGE_COUNTER, count);

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId());
        mRdbStore.update(values, rdbPredicates);
    }

    public int getOmemoMessageCounter(OmemoDevice device) {
        ResultSet resultSet = getIdentityKeyResultSet(device, null);
        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            int count = resultSet.getInt(resultSet.getColumnIndexForName(SQLiteOmemoStore.MESSAGE_COUNTER));
            resultSet.close();
            return count;
        }
        return 0;
    }


    // ========= Fingerprint =========
    public FingerprintStatus getFingerprintStatus(OmemoDevice device, String fingerprint) {
        ResultSet resultSet = getIdentityKeyResultSet(device, fingerprint);
        final FingerprintStatus status;
        if (resultSet.getRowCount() > 0) {
            resultSet.goToFirstRow();
            status = FingerprintStatus.fromResultSet(resultSet);
        }
        else {
            status = null;
        }
        resultSet.close();
        return status;
    }

    public long numTrustedKeys(String bareJid) {
//        DatabaseUtils.queryNumEntries(mStore, SQLiteOmemoStore.IDENTITIES_TABLE_NAME,
//                SQLiteOmemoStore.BARE_JID + "=? AND ("
//                        + SQLiteOmemoStore.TRUST + "=? OR "
//                        + SQLiteOmemoStore.TRUST + "=? OR "
//                        + SQLiteOmemoStore.TRUST + "=?) AND "
//                        + SQLiteOmemoStore.ACTIVE + ">0", args
//        );

        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, bareJid)
                .and().greaterThan(SQLiteOmemoStore.ACTIVE, 0)
                .and()
                .or().equalTo(SQLiteOmemoStore.TRUST, FingerprintStatus.Trust.TRUSTED.toString())
                .or().equalTo(SQLiteOmemoStore.TRUST, FingerprintStatus.Trust.VERIFIED.toString())
                .or().equalTo(SQLiteOmemoStore.TRUST, FingerprintStatus.Trust.VERIFIED_X509.toString());

        return mRdbStore.count(rdbPredicates);
    }

    public void storePreVerification(OmemoDevice device, String fingerprint, FingerprintStatus status) {
        ValuesBucket values = new ValuesBucket();
        values.putString(SQLiteOmemoStore.BARE_JID, device.getJid().toString());
        values.putInteger(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId());
        values.putString(SQLiteOmemoStore.FINGERPRINT, fingerprint);
        values.putValues(status.toValuesBucket());
        mRdbStore.insert(SQLiteOmemoStore.IDENTITIES_TABLE_NAME, values);
    }

    public boolean setIdentityKeyTrust(OmemoDevice device, String fingerprint, FingerprintStatus fingerprintStatus) {
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, device.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, device.getDeviceId())
                .and().equalTo(SQLiteOmemoStore.FINGERPRINT, fingerprint);
        int rows = mRdbStore.update(fingerprintStatus.toValuesBucket(), rdbPredicates);
        return (rows == 1);
    }

    // ========= OMEMO session =========
    private ResultSet getResultSetForSession(OmemoDevice omemoContact) {
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, omemoContact.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, omemoContact.getDeviceId());
        return mRdbStore.query(rdbPredicates, null);
    }

    public SessionRecord loadSession(OmemoDevice omemoContact) {
        SessionRecord sessionRecord = null;
        ResultSet resultSet = getResultSetForSession(omemoContact);
        if (resultSet.getRowCount() != 0) {
            resultSet.goToFirstRow();
            try {
                sessionRecord = new SessionRecord(Base64.getDecoder().decode(
                        resultSet.getString(resultSet.getColumnIndexForName(SQLiteOmemoStore.SESSION_KEY))));
            } catch (IOException e) {
                Timber.w("Could not deserialize raw session. %s", e.getMessage());
            }
        }
        resultSet.close();
        return sessionRecord;
    }

    public HashMap<Integer, SessionRecord> getSubDeviceSessions(BareJid contact) {
        int deviceId;
        SessionRecord session = null;
        HashMap<Integer, SessionRecord> deviceSessions = new HashMap<>();

        String[] columns = {SQLiteOmemoStore.DEVICE_ID, SQLiteOmemoStore.SESSION_KEY};
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.SESSION_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, contact.toString());
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        while (resultSet.goToNextRow()) {
            deviceId = resultSet.getInt(resultSet.getColumnIndexForName(SQLiteOmemoStore.DEVICE_ID));
            String sessionKey = resultSet.getString(resultSet.getColumnIndexForName(SQLiteOmemoStore.SESSION_KEY));
            if (StringUtils.isNotEmpty(sessionKey)) {
                try {
                    session = new SessionRecord(Base64.getDecoder().decode(sessionKey));
                } catch (IOException e) {
                    Timber.w("Could not deserialize raw session. %s", e.getMessage());
                }
                deviceSessions.put(deviceId, session);
            }
        }
        resultSet.close();
        return deviceSessions;
    }


    public HashMap<OmemoDevice, SessionRecord> getAllDeviceSessions() {
        OmemoDevice omemoDevice;
        BareJid bareJid;
        int deviceId;
        String sJid;
        SessionRecord session;
        HashMap<OmemoDevice, SessionRecord> deviceSessions = new HashMap<>();

        String[] columns = {SQLiteOmemoStore.BARE_JID, SQLiteOmemoStore.DEVICE_ID, SQLiteOmemoStore.SESSION_KEY};
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.SESSION_TABLE_NAME);
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        while (resultSet.goToNextRow()) {
            String sessionKey = resultSet.getString(resultSet.getColumnIndexForName(SQLiteOmemoStore.SESSION_KEY));
            if (StringUtils.isNotEmpty(sessionKey)) {
                try {
                    session = new SessionRecord(Base64.getDecoder().decode(sessionKey));
                } catch (IOException e) {
                    Timber.w("Could not deserialize raw session! %s", e.getMessage());
                    continue;
                }

                deviceId = resultSet.getInt(resultSet.getColumnIndexForName(SQLiteOmemoStore.DEVICE_ID));
                sJid = resultSet.getString(resultSet.getColumnIndexForName(SQLiteOmemoStore.BARE_JID));
                try {
                    bareJid = JidCreate.bareFrom(sJid);
                    omemoDevice = new OmemoDevice(bareJid, deviceId);
                    deviceSessions.put(omemoDevice, session);
                } catch (XmppStringprepException e) {
                    Timber.w("Jid creation error for: %s", sJid);
                }
            }
        }
        resultSet.close();
        return deviceSessions;
    }

    public void storeSession(OmemoDevice omemoContact, SessionRecord session) {
        ValuesBucket values = new ValuesBucket();
        values.putString(SQLiteOmemoStore.BARE_JID, omemoContact.getJid().toString());
        values.putInteger(SQLiteOmemoStore.DEVICE_ID, omemoContact.getDeviceId());
        values.putString(SQLiteOmemoStore.SESSION_KEY, Base64.getEncoder().encodeToString(session.serialize()));
        mRdbStore.insert(SQLiteOmemoStore.SESSION_TABLE_NAME, values);
    }

    public void deleteSession(OmemoDevice omemoContact) {
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.SESSION_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, omemoContact.getJid().toString())
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, omemoContact.getDeviceId());
        mRdbStore.delete(rdbPredicates);
    }

    public void deleteAllSessions(BareJid contact) {
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.SESSION_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, contact.toString());
        mRdbStore.delete(rdbPredicates);
    }

    public boolean containsSession(OmemoDevice omemoContact) {
        ResultSet resultSet = getResultSetForSession(omemoContact);
        int count = resultSet.getRowCount();
        resultSet.close();
        return (count != 0);
    }

    // ========= Purge OMEMO dataBase =========

    /**
     * Call by OMEMO regeneration or onAccount deleted to perform clean up for:
     * 1. purge own Omemo deviceId
     * 2. All the preKey records for the deviceId
     * 3. Singed preKey data
     * 4. All the identities and sessions that are associated with the accountUuid
     *
     * @param accountId the specified AccountID to regenerate
     */
    public void purgeOmemoDb(AccountID accountId) {
        String accountJid = accountId.getAccountJid();
        // Timber.d(">>> Wiping OMEMO database for account : %s", accountJid);

        mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.OMEMO_JID, accountJid));
        mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, accountJid));
        mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, accountJid));

        // Cleanup all the session and identities records for own resources, and the contacts
        List<String> identityJids = getContactsForAccount(accountId.getAccountUuid());
        identityJids.add(0, accountJid); // first item to be deleted
        for (String identityJid : identityJids) {
            mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.SESSION_TABLE_NAME)
                    .equalTo(SQLiteOmemoStore.BARE_JID, identityJid));
            mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                    .equalTo(SQLiteOmemoStore.BARE_JID, identityJid));
        }
    }

    /**
     * Call by OMEMO purgeOwnDeviceKeys, it will clean up:
     * 1. purge own Omemo deviceId
     * 2. All the preKey records for own deviceId
     * 3. Singed preKey data
     * 4. The identities and sessions for the specified omemoDevice
     *
     * @param device the specified omemoDevice for cleanup
     */
    public void purgeOmemoDb(OmemoDevice device) {
        // Timber.d(">>> Wiping OMEMO database for device : %s", device);
        String bareJid = device.getJid().toString();
        int deviceId = device.getDeviceId();

        mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.OMEMO_DEVICES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.OMEMO_JID, bareJid)
                .and().equalTo(SQLiteOmemoStore.OMEMO_REG_ID, deviceId));

        mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, bareJid)
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, deviceId));

        mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.SIGNED_PREKEY_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, bareJid)
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, deviceId));

        mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, bareJid)
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, deviceId));

        mRdbStore.delete(new RdbPredicates(SQLiteOmemoStore.SESSION_TABLE_NAME)
                .equalTo(SQLiteOmemoStore.BARE_JID, bareJid)
                .and().equalTo(SQLiteOmemoStore.DEVICE_ID, deviceId));
    }

    /**
     * Fetch all the contacts of the specified accountUuid
     *
     * @param accountUuid Account Uuid
     * @return List of contacts for the specified accountUuid
     */
    public List<String> getContactsForAccount(String accountUuid) {
        List<String> childContacts = new ArrayList<>();

        String[] columns = {MetaContactGroup.CONTACT_JID};
        RdbPredicates rdbPredicates = new RdbPredicates(MetaContactGroup.TBL_CHILD_CONTACTS)
                .equalTo(MetaContactGroup.ACCOUNT_UUID, accountUuid);
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        while (resultSet.goToNextRow()) {
            String contact = resultSet.getString(0);
            if (!TextUtils.isEmpty(contact))
                childContacts.add(contact);
        }
        resultSet.close();
        return childContacts;
    }


    private static class RealMigrationsHelper implements MigrationsHelper {
        ProtocolProviderService mProvider;

        public RealMigrationsHelper(ProtocolProviderService provider) {
            mProvider = provider;
        }

        @Override
        public AccountID getAccountId() {
            return mProvider.getAccountID();
        }

        @Override
        public Context getContext() {
            return aTalkApp.getInstance();
        }
    }
}
