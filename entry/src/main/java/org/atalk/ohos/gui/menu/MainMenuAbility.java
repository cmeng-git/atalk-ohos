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

package org.atalk.ohos.gui.menu;

import java.util.ArrayList;
import java.util.List;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.ProgressBar;
import ohos.agp.components.SearchBar;
import ohos.agp.utils.Color;
import ohos.app.Context;

import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetVideoBridge;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusListener;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusEnum;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.impl.osgi.framework.BundleImpl;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuInflater;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.account.AccountsListAbility;
import org.atalk.ohos.gui.actionbar.ActionBarUtil;
import org.atalk.ohos.gui.call.telephony.TelephonySlice;
import org.atalk.ohos.gui.chat.conference.ConferenceCallInviteDialog;
import org.atalk.ohos.gui.chatroomslist.ChatRoomBookmarksDialog;
import org.atalk.ohos.gui.chatroomslist.ChatRoomCreateDialog;
import org.atalk.ohos.gui.contactlist.AddContactAbility;
import org.atalk.ohos.gui.contactlist.ContactBlockListAbility;
import org.atalk.ohos.gui.contactlist.ContactListSlice;
import org.atalk.ohos.gui.contactlist.model.MetaContactListProvider;
import org.atalk.ohos.gui.settings.SettingsAbility;
import org.atalk.ohos.plugin.geolocation.GeoLocationAbility;
import org.atalk.ohos.plugin.textspeech.TTSAbility;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * The main options menu. Every <code>Ability</code> that desires to have the general options menu
 * shown have to extend this class.
 * <p>
 * The <code>MainMenuActivity</code> is an <code>OSGiAbility</code>.
 *
 * @author Eng Chong Meng
 */
public class MainMenuAbility extends ExitMenuAbility implements ServiceListener, ContactPresenceStatusListener {
    /**
     * Common options menu items.
     */
    protected MenuItem mShowHideOffline;
    protected MenuItem mOnOffLine;
    protected TelephonySlice mTelephony = null;

    /**
     * Video bridge conference call menu. In the case of more than one account.
     */
    private final MenuItem videoBridgeMenuItem = null;
    private VideoBridgeProviderMenuItem menuVbItem = null;

    private static boolean done = false;
    public Context mContext;

    public static boolean disableMediaServiceOnFault = false;

    /*
     * The {@link CallConference} instance depicted by this <code>CallPanel</code>.
     */
    // private final CallConference callConference = null;
    // private ProtocolProviderService preselectedProvider = null;
    // private List<ProtocolProviderService> videoBridgeProviders = null;

    /**
     * Called when the activity is starting. Initializes the corresponding call interface.
     *
     * @param intent If the activity is being re-initialized after previously being shut down then this
     * Bundle contains the data it most recently supplied in onSaveAbilityState(PacMap).
     * Note: Otherwise it is null.
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        mContext = this;
    }

    @Override
    protected void onActive() {
        super.onActive();
        if (AppGUIActivator.bundleContext != null) {
            AppGUIActivator.bundleContext.addServiceListener(this);
            if (menuVbItem == null) {
                initVideoBridge();
            }
        }
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        // FFR v3.0.5: NullPointerException; may have stop() in AndroidGUIActivator
        if (AppGUIActivator.bundleContext != null)
            AppGUIActivator.bundleContext.removeServiceListener(this);
    }

    /**
     * Invoked when the options menu is created. Creates our own options menu from the corresponding xml.
     *
     * @param menu the options menu
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(mContext);
        inflater.parse(ResourceTable.Layout_menu_main, menu);

        // Get the SearchBar and set the search theme
        // MenuItem searchItem = menu.findItem(ResourceTable.Id_search);
        SearchBar searchBar = menu.findComponentById(ResourceTable.Id_search);
        searchBar.setSearchHint(getString(ResourceTable.String_enter_name_or_number));
        searchBar.setSearchTextColor(Color.LTGRAY);

        // cmeng: 20191220 <= disable videoBridge until implementation
        // this.videoBridgeMenuItem = menu.findItem(ResourceTable.Id_create_videobridge);
        /* Need this on first start up */
        // initVideoBridge();
        // videoBridgeMenuItem.setEnabled(true);

        mShowHideOffline = menu.findComponentById(ResourceTable.Id_show_hide_offline);
        int itemId = ConfigurationUtils.isShowOffline()
                ? ResourceTable.String_contact_offline_hide
                : ResourceTable.String_contact_offline_show;
        mShowHideOffline.setTitle(itemId);

        mOnOffLine = menu.findComponentById(ResourceTable.Id_sign_in_off);
        itemId = GlobalStatusEnum.OFFLINE_STATUS.equals(ActionBarUtil.getStatus(this))
                ? ResourceTable.String_sign_in
                : ResourceTable.String_sign_out;
        mOnOffLine.setTitle(itemId);

        // Adds exit option from super class
        super.onCreateOptionsMenu(menu);
        return true;
    }

    /**
     * Put initVideoBridge as separate task as it takes time to filtered server advertised
     * features/info (long list)
     * TODO: cmeng: Need more works for multiple accounts where not all servers support videoBridge
     */
    private void initVideoBridge_task() {
        final boolean enableMenu;
        if (menuVbItem == null)
            this.menuVbItem = new VideoBridgeProviderMenuItem();

        List<ProtocolProviderService> videoBridgeProviders = getVideoBridgeProviders();
        int videoBridgeProviderCount = (videoBridgeProviders == null)
                ? 0 : videoBridgeProviders.size();

        if (videoBridgeProviderCount >= 1) {
            enableMenu = true;
            if (videoBridgeProviderCount == 1) {
                menuVbItem.setPreselectedProvider(videoBridgeProviders.get(0));
            }
            else {
                menuVbItem.setPreselectedProvider(null);
                menuVbItem.setVideoBridgeProviders(videoBridgeProviders);
            }
        }
        else
            enableMenu = false;

        if (videoBridgeMenuItem != null) {
            runOnUiThread(() -> {
                // videoBridgeMenuItem is always enabled - allow user to re-trigger if earlier init failed
                videoBridgeMenuItem.setEnabled(true);

                if (enableMenu) {
                    videoBridgeMenuItem.setAlpha(255);
                }
                else {
                    videoBridgeMenuItem.setAlpha(80);
                    menuVbItem = null;
                }
            });
        }
    }

    /**
     * Progressing dialog to inform user while fetching xmpp server advertised features.
     * May takes time as some servers have many features & slow response.
     * Auto cancel after menu is displayed - end of fetching cycle
     */
    private void initVideoBridge() {
        if (disableMediaServiceOnFault || (videoBridgeMenuItem == null))
            return;

        final ProgressBar progressBar = new ProgressBar(getContext());
        if (!done) {
            progressBar.setProgressHintText(getString(ResourceTable.String_please_wait) + "\n"
                    + getString(ResourceTable.String_server_info_fetching));
        }
        else {
            progressBar.release();
        }

        new Thread(() -> {
            try {
                initVideoBridge_task();
                Thread.sleep(100);
            } catch (Exception ex) {
                Timber.e("Init VideoBridge: %s ", ex.getMessage());
            }
            if (progressBar != null) {
                progressBar.release();
                done = true;
            }
        }).start();
    }

    public MenuItem getMenuItemOnOffLine() {
        return mOnOffLine;
    }

    /**
     * Invoked when an options item has been selected.
     *
     * @param item the item that has been selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent();
        Intent.OperationBuilder operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(getBundleName());

        // Handle item selection
        switch (item.getItemId()) {
            case ResourceTable.Id_search:
                break;
            case ResourceTable.Id_add_chat_room:
                ChatRoomCreateDialog chatRoomCreateDialog = new ChatRoomCreateDialog(this);
                chatRoomCreateDialog.create().show();
                break;

            case ResourceTable.Id_create_videobridge:
                if (menuVbItem == null) {
                    initVideoBridge();
                }
                else
                    menuVbItem.actionPerformed();
                break;

            case ResourceTable.Id_show_location:
                operation.withAbilityName(GeoLocationAbility.class);
                intent.setOperation(operation.build());
                intent.setParam(GeoLocationAbility.SHARE_ALLOW, false);
                startAbility(intent);
                break;

            case ResourceTable.Id_telephony:
                mTelephony = new TelephonySlice();
                getSupportFragmentManager().beginTransaction()
                        .replace(ResourceTable.Id_content, mTelephony, TelephonySlice.TELEPHONY_TAG).commit();
                break;

            case ResourceTable.Id_muc_bookmarks:
                ChatRoomBookmarksDialog chatRoomBookmarksDialog = new ChatRoomBookmarksDialog(this);
                chatRoomBookmarksDialog.create().show();
                break;

            case ResourceTable.Id_add_contact:
                operation.withAbilityName(AddContactAbility.class);
                intent.setOperation(operation.build());
                startAbility(intent);
                break;

            case ResourceTable.Id_block_list:
                operation.withAbilityName(ContactBlockListAbility.class);
                intent.setOperation(operation.build());
                startAbility(intent);
                break;

            case ResourceTable.Id_main_settings:
                operation.withAbilityName(SettingsAbility.class);
                intent.setOperation(operation.build());
                startAbility(intent);
                break;

            case ResourceTable.Id_account_settings:
                operation.withAbilityName(AccountsListAbility.class);
                intent.setOperation(operation.build());
                startAbility(intent);
                break;

            case ResourceTable.Id_tts_settings:
                operation.withAbilityName(TTSAbility.class);
                intent.setOperation(operation.build());
                startAbility(intent);
                break;

            case ResourceTable.Id_show_hide_offline:
                boolean isShowOffline = !ConfigurationUtils.isShowOffline(); // toggle
                MetaContactListProvider.presenceFilter.setShowOffline(isShowOffline);
                AbilitySlice clf = aTalk.getFragment(aTalk.CL_FRAGMENT);
                if (clf instanceof ContactListSlice) {
                    MetaContactListProvider contactListAdapter = ((ContactListSlice) clf).getContactListAdapter();
                    contactListAdapter.filterData("");
                }
                int itemId = isShowOffline
                        ? ResourceTable.String_contact_offline_hide
                        : ResourceTable.String_contact_offline_show;
                mShowHideOffline.setTitle(itemId);

                break;
            case ResourceTable.Id_notification_setting:
                openNotificationSettings();
                break;

            case ResourceTable.Id_sign_in_off:
                // Toggle current account presence status
                boolean isOffline = GlobalStatusEnum.OFFLINE_STATUS.equals(ActionBarUtil.getStatus(this));
                GlobalStatusService globalStatusService = AppGUIActivator.getGlobalStatusService();
                if (isOffline)
                    globalStatusService.publishStatus(GlobalStatusEnum.ONLINE);
                else
                    globalStatusService.publishStatus(GlobalStatusEnum.OFFLINE);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    //========================================================

    /**
     * The <code>VideoBridgeProviderMenuItem</code> for each protocol provider.
     */
    private class VideoBridgeProviderMenuItem {
        private ProtocolProviderService preselectedProvider;
        private List<ProtocolProviderService> videoBridgeProviders;

        /**
         * Creates an instance of <code>VideoBridgeProviderMenuItem</code>
         * <p>
         * // @param preselectedProvider the <code>ProtocolProviderService</code> that provides the video bridge
         */
        public VideoBridgeProviderMenuItem() {
            preselectedProvider = null;
            videoBridgeProviders = null;
        }

        /**
         * Opens a conference invite dialog when this menu is selected.
         */
        public void actionPerformed() {
            ConferenceCallInviteDialog inviteDialog = null;
            if (preselectedProvider != null)
                inviteDialog = new ConferenceCallInviteDialog(mContext, preselectedProvider, true);
            else if (videoBridgeProviders != null)
                inviteDialog = new ConferenceCallInviteDialog(mContext, videoBridgeProviders, true);

            if (inviteDialog != null)
                inviteDialog.create().show();
        }

        public void setPreselectedProvider(ProtocolProviderService protocolProvider) {
            this.preselectedProvider = protocolProvider;
        }

        public void setVideoBridgeProviders(List<ProtocolProviderService> videoBridgeProviders) {
            this.videoBridgeProviders = videoBridgeProviders;
        }
    }

    /**
     * Returns a list of all available video bridge providers.
     *
     * @return a list of all available video bridge providers
     */
    private List<ProtocolProviderService> getVideoBridgeProviders() {
        List<ProtocolProviderService> activeBridgeProviders = new ArrayList<>();

        for (ProtocolProviderService videoBridgeProvider
                : AccountUtils.getRegisteredProviders(OperationSetVideoBridge.class)) {
            OperationSetVideoBridge videoBridgeOpSet
                    = videoBridgeProvider.getOperationSet(OperationSetVideoBridge.class);

            // Check if the video bridge is actually active before adding it to the list of active providers.
            if (videoBridgeOpSet.isActive())
                activeBridgeProviders.add(videoBridgeProvider);
        }
        return activeBridgeProviders;
    }

    /**
     * Implements the <code>ServiceListener</code> method. Verifies whether the passed event concerns
     * a <code>ProtocolProviderService</code> and adds the corresponding UI controls in the menu.
     *
     * @param event The <code>ServiceEvent</code> object.
     */
    public void serviceChanged(ServiceEvent event) {
        ServiceReference<?> serviceRef = event.getServiceReference();

        // Timber.d("Bundle State: %s: ", serviceRef.getBundle().getState());
        // if the event is caused by a bundle being stopped, we don't want to know
        if (serviceRef.getBundle().getState() == BundleImpl.STOPPING) {
            return;
        }

        // we don't care if the source service is not a protocol provider
        Object service = AppGUIActivator.bundleContext.getService(serviceRef);
        if (!(service instanceof ProtocolProviderService)) {
            return;
        }

        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
            case ServiceEvent.UNREGISTERING:
                if (videoBridgeMenuItem != null) {
                    runOnUiThread(this::initVideoBridge);
                }
                break;
        }
    }

    @Override
    public void contactPresenceStatusChanged(final ContactPresenceStatusChangeEvent evt) {
        // cmeng - how to add the listener onActive - multiple protocol providers???
        runOnUiThread(() -> {
            Contact sourceContact = evt.getSourceContact();
            initVideoBridge();
        });
    }
}
