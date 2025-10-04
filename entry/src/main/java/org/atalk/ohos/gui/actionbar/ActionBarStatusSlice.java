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
package org.atalk.ohos.gui.actionbar;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.media.image.PixelMap;

import net.java.sip.communicator.service.globaldisplaydetails.GlobalDisplayDetailsService;
import net.java.sip.communicator.service.globaldisplaydetails.event.GlobalAvatarChangeEvent;
import net.java.sip.communicator.service.globaldisplaydetails.event.GlobalDisplayDetailsListener;
import net.java.sip.communicator.service.globaldisplaydetails.event.GlobalDisplayNameChangeEvent;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusEnum;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService;
import net.java.sip.communicator.util.StatusUtil;
import net.java.sip.communicator.util.account.AccountUtils;

import org.apache.commons.lang3.StringUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.account.AppLoginRenderer;
import org.atalk.ohos.gui.menu.GlobalStatusMenu;
import org.atalk.ohos.gui.util.event.EventListener;
import org.atalk.ohos.gui.widgets.ActionMenuItem;
import org.atalk.ohos.util.AppImageUtil;

import java.util.Collection;

/**
 * AbilitySlice when added to Ability will display global display details like avatar, display name
 * and status. External events will also trigger a change to the contents.
 * When status is clicked a popup menu is displayed allowing user to set global presence status.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class ActionBarStatusSlice extends BaseSlice
        implements EventListener<PresenceStatus>, GlobalDisplayDetailsListener {
    /**
     * The online status.
     */
    private static final int ONLINE = 1;

    /**
     * The offline status.
     */
    private static final int OFFLINE = 2;

    /**
     * The free for chat status.
     */
    private static final int FFC = 3;

    /**
     * The away status.
     */
    private static final int AWAY = 4;

    /**
     * The away status.
     */
    private static final int EXTENDED_AWAY = 5;

    /**
     * The do not disturb status.
     */
    private static final int DND = 6;

    private static int ACTION_ID = DND + 1;

    /**
     * The global status menu.
     */
    private GlobalStatusMenu globalStatusMenu;
    private Ability mContext;

    private static GlobalDisplayDetailsService displayDetailsService;
    private static AppLoginRenderer loginRenderer;
    private Component actionBarView;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        mContext = getAbility();

        displayDetailsService = AppGUIActivator.getGlobalDisplayDetailsService();
        globalStatusMenu = createGlobalStatusMenu();

        actionBarView = mContext.findComponentById(ResourceTable.Id_actionBarView);
        if (actionBarView != null) {
            actionBarView.setClickedListener(v -> {
                globalStatusMenu.show(actionBarView);
            });
        }
    }

    @Override
    public void onActive() {
        super.onActive();
        loginRenderer = AppGUIActivator.getLoginRenderer();
        loginRenderer.addGlobalStatusListener(this);
        onChangeEvent(loginRenderer.getGlobalStatus());

        displayDetailsService.addGlobalDisplayDetailsListener(this);
        setGlobalAvatar(displayDetailsService.getDisplayAvatar(null));
        setGlobalDisplayName(displayDetailsService.getDisplayName(null));
    }

    @Override
    public void onInactive() {
        super.onInactive();
        loginRenderer.removeGlobalStatusListener(this);
        displayDetailsService.removeGlobalDisplayDetailsListener(this);
    }

    /**
     * Creates the <code>GlobalStatusMenu</code>.
     *
     * @return the newly created <code>GlobalStatusMenu</code>
     */
    private GlobalStatusMenu createGlobalStatusMenu() {
        ActionMenuItem ffcItem = new ActionMenuItem(FFC,
                mContext.getString(ResourceTable.String_free_for_chat),
                AppImageUtil.getPixelMap(mContext, ResourceTable.Media_global_ffc));
        ActionMenuItem onlineItem = new ActionMenuItem(ONLINE,
                mContext.getString(ResourceTable.String_online),
                AppImageUtil.getPixelMap(mContext, ResourceTable.Media_global_online));
        ActionMenuItem offlineItem = new ActionMenuItem(OFFLINE,
                mContext.getString(ResourceTable.String_offline),
                AppImageUtil.getPixelMap(mContext, ResourceTable.Media_global_offline));
        ActionMenuItem awayItem = new ActionMenuItem(AWAY,
                mContext.getString(ResourceTable.String_away),
                AppImageUtil.getPixelMap(mContext, ResourceTable.Media_global_away));
        ActionMenuItem extendedAwayItem = new ActionMenuItem(EXTENDED_AWAY,
                mContext.getString(ResourceTable.String_extended_away),
                AppImageUtil.getPixelMap(mContext, ResourceTable.Media_global_extended_away));
        ActionMenuItem dndItem = new ActionMenuItem(DND,
                mContext.getString(ResourceTable.String_do_not_disturb),
                AppImageUtil.getPixelMap(mContext, ResourceTable.Media_global_dnd));

        final GlobalStatusMenu globalStatusMenu = new GlobalStatusMenu(mContext, actionBarView);
        globalStatusMenu.addActionItem(ffcItem);
        globalStatusMenu.addActionItem(onlineItem);
        globalStatusMenu.addActionItem(offlineItem);
        globalStatusMenu.addActionItem(awayItem);
        globalStatusMenu.addActionItem(extendedAwayItem);
        globalStatusMenu.addActionItem(dndItem);

        // Add all registered PPS users to the presence status menu
        Collection<ProtocolProviderService> registeredProviders = AccountUtils.getRegisteredProviders();
        for (ProtocolProviderService pps : registeredProviders) {
            AccountID accountId = pps.getAccountID();
            String userJid = accountId.getAccountJid();
            PixelMap icon = AppImageUtil.getPixelMap(mContext, ResourceTable.Media_jabber_status_online);

            ActionMenuItem actionItem = new ActionMenuItem(ACTION_ID++, userJid, icon);
            globalStatusMenu.addActionItem(actionItem, pps);
        }

        globalStatusMenu.setOnActionItemClickListener((source, pos, actionId) -> {
            if (actionId <= DND)
                publishGlobalStatus(actionId);
        });

        globalStatusMenu.setOnDismissListener(() -> {
            // TODO: Add a dismiss action.
        });
        return globalStatusMenu;
    }

    /**
     * Publishes global status on separate thread to prevent <code>NetworkOnMainThreadException</code>.
     *
     * @param newStatus new global status to set.
     */
    private void publishGlobalStatus(final int newStatus) {
        /*
         * Runs publish status on separate thread to prevent NetworkOnMainThreadException
         */
        new Thread(() -> {
            GlobalStatusService globalStatusService = AppGUIActivator.getGlobalStatusService();
            switch (newStatus) {
                case FFC:
                    globalStatusService.publishStatus(GlobalStatusEnum.FREE_FOR_CHAT);
                    break;
                case ONLINE:
                    globalStatusService.publishStatus(GlobalStatusEnum.ONLINE);
                    break;
                case OFFLINE:
                    globalStatusService.publishStatus(GlobalStatusEnum.OFFLINE);
                    break;
                case AWAY:
                    globalStatusService.publishStatus(GlobalStatusEnum.AWAY);
                    break;
                case EXTENDED_AWAY:
                    globalStatusService.publishStatus(GlobalStatusEnum.EXTENDED_AWAY);
                    break;
                case DND:
                    globalStatusService.publishStatus(GlobalStatusEnum.DO_NOT_DISTURB);
                    break;
            }
        }).start();
    }

    @Override
    public void onChangeEvent(final PresenceStatus presenceStatus) {
        if ((presenceStatus == null) || (mContext == null))
            return;

        BaseAbility.runOnUiThread(() -> {
            String mStatus = presenceStatus.getStatusName();
            ActionBarUtil.setSubtitle(mContext, mStatus);
            ActionBarUtil.setStatusIcon(mContext, StatusUtil.getStatusIcon(presenceStatus));

            MenuItem mOnOffLine = ((aTalk) mContext).getMenuItemOnOffLine();
            // Proceed only if mOnOffLine has been initialized
            if (mOnOffLine != null) {
                boolean isOffline = GlobalStatusEnum.OFFLINE_STATUS.equals(mStatus);
                int itemId = isOffline ? ResourceTable.String_sign_in : ResourceTable.String_sign_out;
                mOnOffLine.setTitle(itemId);
                mOnOffLine.setVisible(isOffline);
            }
        });
    }

    /**
     * Indicates that the global avatar has been changed.
     */
    @Override
    public void globalDisplayAvatarChanged(final GlobalAvatarChangeEvent evt) {
        BaseAbility.runOnUiThread(() -> setGlobalAvatar(evt.getNewAvatar()));
    }

    /**
     * Indicates that the global display name has been changed.
     */
    @Override
    public void globalDisplayNameChanged(final GlobalDisplayNameChangeEvent evt) {
        BaseAbility.runOnUiThread(() -> setGlobalDisplayName(evt.getNewDisplayName()));
    }

    /**
     * Sets the global avatar in the action bar.
     *
     * @param avatar the byte array representing the avatar to set
     */
    private void setGlobalAvatar(final byte[] avatar) {
        if (avatar != null && avatar.length > 0) {
            ActionBarUtil.setAvatar(mContext, avatar);
        }
        else {
            ActionBarUtil.setAvatar(mContext, ResourceTable.Media_ic_icon);
        }
    }

    /**
     * Sets the global display name in the action bar as 'Me' if multiple accounts are involved, otherwise UserJid.
     *
     * @param name the display name to set
     */
    private void setGlobalDisplayName(final String name) {
        String displayName = name;
        Collection<ProtocolProviderService> pProviders = AccountUtils.getRegisteredProviders();

        if (StringUtils.isEmpty(displayName) && (pProviders.size() == 1)) {
            displayName = pProviders.iterator().next().getAccountID().getUserID();
        }
        if (pProviders.size() > 1)
            displayName = getString(ResourceTable.String_account_me);
        ActionBarUtil.setTitle(mContext, displayName);
    }
}
