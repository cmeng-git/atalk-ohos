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

import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.render.render3d.BuildConfig;
import ohos.app.Context;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.impl.protocol.jabber.ServiceDiscoveryHelper;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.crypto.omemo.SQLiteOmemoStore;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.persistance.migrations.OmemoDBCreate;
import org.atalk.service.fileaccess.FileCategory;
import org.atalk.service.libjitsi.LibJitsi;
import org.jivesoftware.smackx.avatar.vcardavatar.VCardAvatarManager;
import org.jivesoftware.smackx.omemo.OmemoService;
import org.jivesoftware.smackx.omemo.OmemoStore;

import timber.log.Timber;

import static net.java.sip.communicator.plugin.loggingutils.LogsCollector.LOGGING_DIR_NAME;

/**
 * Dialog allowing user to refresh persistent stores.
 *
 * @author Eng Chong Meng
 */
public class ServerPersistentStoresRefreshDialog extends Component {
    private final Component mDialog;
    private final Context mContext;

    public ServerPersistentStoresRefreshDialog(Context ctx) {
        super(ctx);
        mContext = ctx;
        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        mDialog = inflater.parse(ResourceTable.Layout_refresh_persistent_stores, null, false);
        if (BuildConfig.DEBUG) {
            mDialog.findComponentById(ResourceTable.Id_cb_export_database).setVisibility(Component.VISIBLE);
        }
    }

    /**
     * Displays create refresh store dialog. If the source wants to be notified about the result
     * should pass the listener here or <code>null</code> otherwise.
     */
    public void show() {
        DialogH.getInstance(mContext).showCustomDialog(mContext,
                mContext.getString(ResourceTable.String_refresh_store), mDialog,
                mContext.getString(ResourceTable.String_refresh_apply),
                new DialogListenerImpl(), null);
    }

    /**
     * Implements <code>DialogH.DialogListener</code> interface and handles refresh stores process.
     */
    class DialogListenerImpl implements DialogH.DialogListener {
        @Override
        public boolean onConfirmClicked(DialogH dialog) {
            Checkbox cbRoster = mDialog.findComponentById(ResourceTable.Id_cb_roster); // view.findComponentById(cb_roster);
            Checkbox cbCaps = mDialog.findComponentById(ResourceTable.Id_cb_caps);
            Checkbox cbDiscoInfo = mDialog.findComponentById(ResourceTable.Id_cb_discoInfo);
            Checkbox cbAvatar = mDialog.findComponentById(ResourceTable.Id_cb_avatar);
            Checkbox cbOmemo = mDialog.findComponentById(ResourceTable.Id_cb_omemo);
            Checkbox cbDebugLog = mDialog.findComponentById(ResourceTable.Id_cb_debug_log);
            Checkbox cbExportDB = mDialog.findComponentById(ResourceTable.Id_cb_export_database);
            Checkbox cbDeleteDB = mDialog.findComponentById(ResourceTable.Id_cb_del_database);

            if (cbRoster.isChecked()) {
                refreshRosterStore();
            }
            if (cbCaps.isChecked()) {
                ServiceDiscoveryHelper.refreshEntityCapsStore();
            }
            if (cbAvatar.isChecked()) {
                purgeAvatarStorage();
            }
            if (cbOmemo.isChecked()) {
                purgeOmemoStorage();
            }
            if (cbDebugLog.isChecked()) {
                purgeDebugLog();
            }
            if (cbExportDB.isChecked()) {
                exportDB();
            }
            if (cbDeleteDB.isChecked()) {
                deleteDB();
            }
            return true;
        }

        @Override
        public void onDialogCancelled(DialogH dialog) {
            dialog.destroy();
        }
    }

    /**
     * Process to refresh roster store for each registered account
     * Persistent Store for XEP-0237:Roster Versioning
     */
    private void refreshRosterStore() {
        Collection<ProtocolProviderService> ppServices = AccountUtils.getRegisteredProviders();
        for (ProtocolProviderService pps : ppServices) {
            ProtocolProviderServiceJabberImpl jabberProvider = (ProtocolProviderServiceJabberImpl) pps;

            File rosterStoreDirectory = jabberProvider.getRosterStoreDirectory();
            if ((rosterStoreDirectory != null) && rosterStoreDirectory.exists()) {
                try {
                    FileBackend.deleteRecursive(rosterStoreDirectory);
                } catch (IOException e) {
                    Timber.e("Failed to purge store for: %s", ResourceTable.String_refresh_store_roster);
                }
                jabberProvider.initRosterStore();
            }
        }
    }

    /**
     * Process to clear the VCard Avatar Index and purge persistent storage for all accounts
     * XEP-0153: vCard-Based Avatars
     */
    private void purgeAvatarStorage() {
        VCardAvatarManager.clearPersistentStorage();
    }

    /**
     * Process to purge persistent storage for OMEMO_Store
     * XEP-0384: OMEMO Encryption
     */
    private void purgeOmemoStorage() {
        // accountID omemo key attributes
        String ZSONKEY_REGISTRATION_ID = "omemoRegId";
        String ZSONKEY_CURRENT_PREKEY_ID = "omemoCurPreKeyId";
        Context ctx = aTalkApp.getInstance();

        OmemoStore<?, ?, ?, ?, ?, ?, ?, ?, ?> omemoStore = OmemoService.getInstance().getOmemoStoreBackend();
        Collection<ProtocolProviderService> ppServices = AccountUtils.getRegisteredProviders();
        if (omemoStore instanceof SQLiteOmemoStore) {
            DatabaseBackend db = DatabaseBackend.getInstance(ctx);
            for (ProtocolProviderService pps : ppServices) {
                AccountID accountId = pps.getAccountID();
                accountId.unsetKey(ZSONKEY_CURRENT_PREKEY_ID);
                accountId.unsetKey(ZSONKEY_REGISTRATION_ID);
                db.updateAccount(accountId);
            }
            OmemoDBCreate.createOmemoTables(DatabaseBackend.getRdbStore());

            // start to regenerate all Omemo data for registered accounts - has exception
            // SQLiteOmemoStore.loadOmemoSignedPreKey().371 There is no SignedPreKeyRecord for: 0
            // SignedPreKeyRecord.getKeyPair()' on a null object reference
            for (ProtocolProviderService pps : ppServices) {
                AccountID accountId = pps.getAccountID();
                ((SQLiteOmemoStore) omemoStore).regenerate(accountId);
            }
        }

        // This is here for file-based implementation and not use anymore
        else {
            String OMEMO_Store = "OMEMO_Store";
            File omemoDir = new File(ctx.getFilesDir(), OMEMO_Store);
            if (omemoDir.exists()) {
                try {
                    FileBackend.deleteRecursive(omemoDir);
                } catch (IOException e) {
                    Timber.w("Exception %s", e.getMessage());
                }
            }
        }
        Timber.i("### Omemo store has been refreshed!");
    }

    private void exportDB() {
        String clFileName = "contactlist.xml";
        String OMEMO_Store = "OMEMO_Store";
        String database = "databases";
        String sharedPrefs = "shared_prefs";
        String history = "history_ver1.0";

        File appFilesDir = aTalkApp.getInstance().getFilesDir();
        File appRootDir = appFilesDir.getParentFile();

        File appDBDir = new File(appRootDir, database);
        File appSPDir = new File(appRootDir, sharedPrefs);
        File appHistoryDir = new File(appFilesDir, history);
        File appOmemoDir = new File(appFilesDir, OMEMO_Store);
        File appXmlFP = new File(appRootDir, clFileName);

        File atalkExportDir = FileBackend.getaTalkStore(FileBackend.EXPROT_DB, true);
        if (atalkExportDir == null)
            return;

        try {
            // Clean up old contents before create new
            FileBackend.deleteRecursive(atalkExportDir);
            if (!atalkExportDir.mkdirs()) {
                Timber.e("Could not create atalk dir: %s", atalkExportDir);
            }
            // To copy everything under files (large amount of data).
            // FileBackend.copyRecursive(appDBDir, atalkDLDir, null);

            FileBackend.copyRecursive(appDBDir, atalkExportDir, database);
            FileBackend.copyRecursive(appSPDir, atalkExportDir, sharedPrefs);

            if (appOmemoDir.exists()) {
                FileBackend.copyRecursive(appOmemoDir, atalkExportDir, OMEMO_Store);
            }
            if (appHistoryDir.exists()) {
                FileBackend.copyRecursive(appHistoryDir, atalkExportDir, history);
            }
            if (appXmlFP.exists()) {
                FileBackend.copyRecursive(appXmlFP, atalkExportDir, clFileName);
            }
        } catch (Exception e) {
            Timber.w("Export database exception: %s", e.getMessage());
        }
    }

    /**
     * Warn: Delete the aTalk dataBase
     * Static access from other module
     */
    public static void deleteDB() {
        Context ctx = aTalkApp.getInstance();
        DatabaseBackend.getInstance(ctx).deleteRdbStore(DatabaseBackend.DATABASE_NAME);
    }

    /**
     * Process to purge all debug log files in case it gets too large to handle
     * Static access from other module
     */
    public static void purgeDebugLog() {
        File logDir;
        try {
            logDir = LibJitsi.getFileAccessService().getPrivatePersistentDirectory(LOGGING_DIR_NAME, FileCategory.LOG);
            if ((logDir != null) && logDir.exists()) {
                final File[] files = logDir.listFiles();
                for (File file : files) {
                    if (!file.delete())
                        Timber.w("Couldn't delete log file: %s", file.getName());
                }
            }
        } catch (Exception ex) {
            Timber.e(ex, "Couldn't delete log file directory.");
        }
    }
}
