/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.account;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.ProgressBar;
import ohos.agp.components.ToggleButton;

import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.ServiceUtils;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.account.settings.AccountPreferenceAbility;
import org.atalk.ohos.gui.contactlist.AddGroupDialog;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.gui.dialogs.PopupMenu;
import org.atalk.ohos.plugin.certconfig.TLS_Configuration;
import org.atalk.persistance.FileBackend;
import org.atalk.persistance.ServerPersistentStoresRefreshDialog;
import org.jivesoftware.smackx.avatar.vcardavatar.VCardAvatarManager;
import org.jxmpp.jid.BareJid;
import org.jxmpp.stringprep.XmppStringprepException;

import timber.log.Timber;

/**
 * The activity display list of currently stored accounts showing the associated protocol and current status.
 *
 * @author Eng Chong Meng
 */
public class AccountsListAbility extends BaseAbility {
    /**
     * The list adapter for accounts
     */
    private AccountStatusListProvider listAdapter;
    /**
     * The {@link AccountManager} used to operate on {@link AccountID}s
     */
    private AccountManager accountManager;

    /**
     * Stores clicked account in member field, as context info is not available. That's because account
     * list contains on/off buttons and that prevents from "normal" list item clicks / long clicks handling.
     */
    private Account clickedAccount;

    /**
     * Keeps track of displayed "in progress" dialog during account registration.
     */
    // private static long progressDialog;

    private ProgressBar mProgressBar;

    /**
     * Keeps track of thread used to register accounts and prevents from starting multiple at one time.
     */
    private static AccountEnableThread accEnableThread;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setMainTitle(ResourceTable.String_account);

        if (AppGUIActivator.bundleContext == null) {
            // No OSGi Exists
            Timber.e("OSGi not initialized");
            terminateAbility();
            return;
        }
        setUIContent(ResourceTable.Layout_account_list);
        this.accountManager = ServiceUtils.getService(AppGUIActivator.bundleContext, AccountManager.class);
    }

    @Override
    protected void onActive() {
        super.onActive();

        // Need to refresh the list each time in case account might be removed in other Ability.
        // Also it can't be removed on "unregistered" event, because on/off buttons will cause the account to disappear
        accountsInit();
    }

    @Override
    protected void onStop() {
        // Unregisters presence status listeners
        if (listAdapter != null) {
            listAdapter.deinitStatusListeners();
        }
        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().parse(ResourceTable.Layout_menu_account_settings, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getId()) {
            case ResourceTable.Id_add_account:
                Intent intent = new Intent();
                Operation operation = new Intent.OperationBuilder()
                        .withDeviceId("")
                        .withBundleName(getBundleName())
                        .withAbilityName(AccountLoginAbility.class)
                        .build();
                intent.setOperation(operation);
                startAbility(intent);
                return true;

            case ResourceTable.Id_add_group:
                new AddGroupDialog(this).show(null);
                return true;

            case ResourceTable.Id_TLS_Configuration:
                TLS_Configuration tlsConfiguration = new TLS_Configuration();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                ft.replace(android.ResourceTable.Id_content, tlsConfiguration).commit();
                return true;

            case ResourceTable.Id_refresh_database:
                new ServerPersistentStoresRefreshDialog(this).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Initializes the accounts table.
     */
    private void accountsInit() {
        // Create accounts array
        Collection<AccountID> accountIDCollection = AccountUtils.getStoredAccounts();

        // Create account list adapter
        listAdapter = new AccountStatusListProvider(accountIDCollection);

        // Puts the adapter into accounts ListContainer
        ListContainer lv = findComponentById(ResourceTable.Id_accountListContainer);
        lv.setItemProvider(listAdapter);
    }

    public void showPopupMenu(Component accountView, Account clickAccount) {
        PopupMenu menu = new PopupMenu(getContext(), accountView);

        menu.setupMenu(ResourceTable.Layout_menu_account);
        menu.setHeaderTitle(clickedAccount.getAccountName());

        boolean isRegistered = clickedAccount.getProtocolProvider() != null;
        menu.setVisible(ResourceTable.Id_account_settings, isRegistered);
        menu.setVisible(ResourceTable.Id_account_info, isRegistered);

        menu.setVisible(ResourceTable.Id_remove, true);
        menu.setVisible(ResourceTable.Id_cancel, true);

        menu.setMenuItemClickedListener(new PopupMenuItemClick(clickAccount));
        menu.show();
    }

    private class PopupMenuItemClick implements PopupMenu.MenuItemClickedListener {
        private final Account mAccount;

        PopupMenuItemClick(Account account) {
            mAccount = account;
        }

        /**
         * This method will be invoked when a menu item is clicked if the item
         * itself did not already handle the event.
         *
         * @param item the menu item that was clicked
         *
         * @return {@code true} if the event was handled, {@code false} otherwise
         */
        @Override
        public boolean onMenuItemClick(Component item) {
            switch (item.getId()) {
                case ResourceTable.Id_remove:
                    AccountDeleteDialog.create(getContext(), clickedAccount, account -> listAdapter.remove(account));
                    return true;

                case ResourceTable.Id_account_settings:
                    startPreferenceActivity(clickedAccount);
                    return true;

                case ResourceTable.Id_account_info:
                    startPresenceActivity(clickedAccount);
                    return true;

                case ResourceTable.Id_account_cancel:
                    return true;
            }
            return false;
        }
    }

    /**
     * Starts the {@link AccountPreferenceAbility} for clicked {@link Account}
     *
     * @param account the <code>Account</code> for which preference settings will be opened.
     */
    private void startPreferenceActivity(Account account) {
        Intent preferences = AccountPreferenceAbility.getIntent(this, account.getAccountID());
        startAbility(preferences);
    }

    /**
     * Starts the {@link AccountInfoPresenceAbility} for clicked {@link Account}
     *
     * @param account the <code>Account</code> for which settings will be opened.
     */
    private void startPresenceActivity(Account account) {
        Intent statusIntent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(getBundleName())
                .withAbilityName(AccountInfoPresenceAbility.class)
                .build();
        statusIntent.setOperation(operation);
        statusIntent.setParam(AccountInfoPresenceAbility.INTENT_ACCOUNT_ID, account.getAccountID().getAccountUid());
        startAbility(statusIntent);
    }

    /**
     * Removes the account persistent storage from the device
     *
     * @param accountId the {@link AccountID} for whom the persistent to be purged from the device
     */
    public static void removeAccountPersistentStore(AccountID accountId) {
        ProtocolProviderService pps = accountId.getProtocolProvider();
        if (pps instanceof ProtocolProviderServiceJabberImpl) {
            ProtocolProviderServiceJabberImpl jabberProvider = (ProtocolProviderServiceJabberImpl) pps;

            // Purge avatarHash and avatarImages of all contacts belong to the account roster
            BareJid userJid = accountId.getEntityBareJid();
            try {
                VCardAvatarManager.clearPersistentStorage(userJid);
            } catch (XmppStringprepException e) {
                Timber.e("Failed to purge store for: %s", ResourceTable.String_refresh_store_avatar);
            }

            File rosterStoreDirectory = jabberProvider.getRosterStoreDirectory();
            try {
                if (rosterStoreDirectory != null)
                    FileBackend.deleteRecursive(rosterStoreDirectory);
            } catch (IOException e) {
                Timber.e("Failed to purge store for: %s", ResourceTable.String_refresh_store_roster);
            }

            // Account in unRegistering so discoveryInfoManager == null
            // ServiceDiscoveryManager discoveryInfoManager = jabberProvider.getDiscoveryManager();
            // File discoInfoStoreDirectory = discoveryInfoManager.getDiscoInfoPersistentStore();
            File discoInfoStoreDirectory = new File(aTalkApp.getInstance().getFilesDir()
                    + "/discoInfoStore_" + userJid);
            try {
                FileBackend.deleteRecursive(discoInfoStoreDirectory);
            } catch (IOException e) {
                Timber.e("Failed to purge store for: %s", ResourceTable.String_refresh_store_disc_info);
            }
        }
    }

    /**
     * Class responsible for creating list row Views
     */
    class AccountStatusListProvider extends AccountsListProvider {
        /**
         * Creates new instance of {@link AccountStatusListProvider}
         *
         * @param accounts array of currently stored accounts
         */
        AccountStatusListProvider(Collection<AccountID> accounts) {
            super(AccountsListAbility.this, ResourceTable.Layout_account_list_row, -1, accounts, false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Component getComponent(boolean isDropDown, final Account account, ComponentContainer parent, LayoutScatter inflater) {
            // Creates the list view
            Component rowView = super.getComponent(isDropDown, account, parent, inflater);

            rowView.setClickable(true);
            rowView.setClickedListener(v -> {
                // Start only for registered accounts
                if (account.getProtocolProvider() != null) {
                    startPreferenceActivity(account);
                }
                else {
                    String msg = getString(R.string.account_unregistered, account.getAccountName());
                    if (offlineToast == null) {
                        offlineToast = Toast.makeText(AccountsListActivity.this, msg, Toast.LENGTH_SHORT);
                    }
                    else {
                        offlineToast.setText(msg);
                    }
                    offlineToast.show();
                }
            });
            rowView.setLongClickedListener(v -> {
                clickedAccount = account;
                showPopupMenu(v, account);
            });

            ToggleButton button = rowView.findComponentById(ResourceTable.Id_accountToggleButton);
            button.setChecked(account.isEnabled());

            button.setCheckedStateChangedListener((chkButton, enable) -> {
                if (accEnableThread != null) {
                    Timber.e("Ongoing operation in progress");
                    return;
                }
                Timber.d("Toggle %s -> %s", account, enable);

                // Prevents from switching the state after key pressed. Refresh will be
                // triggered by the thread when it finishes the operation.
                chkButton.setChecked(account.isEnabled());

                accEnableThread = new AccountEnableThread(account.getAccountID(), enable);
                String message = enable ? getString(ResourceTable.String_connecting_, account.getAccountName())
                        : getString(ResourceTable.String_disconnecting_, account.getAccountName());
                mProgressBar = new ProgressBar(getContext());
                mProgressBar.setIndeterminate(true);
                mProgressBar.setProgressHintText(message);
                accEnableThread.start();
            });
            return rowView;
        }
    }

    /**
     * The thread that runs enable/disable operations
     */
    class AccountEnableThread extends Thread {
        /**
         * The {@link AccountID} that will be enabled or disabled
         */
        private final AccountID account;
        /**
         * Flag decides whether account shall be disabled or enabled
         */
        private final boolean enable;

        /**
         * Creates new instance of {@link AccountEnableThread}
         *
         * @param account the {@link AccountID} that will be enabled or disabled
         * @param enable flag indicates if this is enable or disable operation
         */
        AccountEnableThread(AccountID account, boolean enable) {
            this.account = account;
            this.enable = enable;
        }

        @Override
        public void run() {
            try {
                if (enable)
                    accountManager.loadAccount(account);
                else {
                    accountManager.unloadAccount(account);
                }
            } catch (OperationFailedException e) {
                String message = "Failed to " + (enable ? "load" : "unload") + " " + account;
                BaseAbility.runOnUiThread(() -> {
                    new DialogA.Builder(AccountsListAbility.this)
                            .setTitle(ResourceTable.String_error)
                            .setContent(message)
                            .setPositiveButton(ResourceTable.String_ok, null)
                            .create()
                            .show();
                });
                Timber.e("Account de/activate Exception: %s", e.getMessage());
            } finally {
                if (mProgressBar.isBoundToWindow())
                mProgressBar.release();
                else {
                    Timber.e("Failed to wait for the dialog: %s", mProgressBar);
                }
                accEnableThread = null;
            }
        }
    }
}
