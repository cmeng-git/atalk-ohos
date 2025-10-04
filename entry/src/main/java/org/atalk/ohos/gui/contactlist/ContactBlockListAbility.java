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
package org.atalk.ohos.gui.contactlist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohos.aafwk.content.Intent;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.agp.components.TextField;
import ohos.agp.components.element.ShapeElement;
import ohos.media.image.PixelMap;

import net.java.sip.communicator.impl.protocol.jabber.OperationSetPersistentPresenceJabberImpl;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ContactBlockingStatusListener;
import net.java.sip.communicator.util.StatusUtil;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.util.EntityListHelper;
import org.atalk.ohos.util.AppImageUtil;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.avatar.AvatarManager;
import org.jivesoftware.smackx.blocking.BlockingCommandManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;

import timber.log.Timber;

/**
 * An activity to show all the blocked contacts; user may unblock any listed contact from this view.
 */
public class ContactBlockListAbility extends BaseAbility
        implements ContactBlockingStatusListener, Component.LongClickedListener {
    private final List<OperationSetPresence> presenceOpSets = new ArrayList<>();

    // A reference map between contact and its viewHolder
    private final Map<Contact, ContactViewHolder> mContactViews = new HashMap<>();

    private final List<Contact> volatileContacts = new ArrayList<>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setMainTitle(ResourceTable.String_block_list_title);

        setUIContent(ResourceTable.Layout_list_layout);
        ListContainer contactBlockList = findComponentById(ResourceTable.Id_list);
        contactBlockList.setBackground(new ShapeElement(getContext(), ResourceTable.Color_background_light));

        BlockListProvider blockListProvider = new BlockListProvider();
        contactBlockList.setItemProvider (blockListProvider);
    }

    /**
     * Get a list of all known contacts being blocked
     *
     * @return the list of all known contacts being blocked.
     */
    private List<Contact> getContactBlockList() {
        List<Contact> blockContacts = new ArrayList<>();
        volatileContacts.clear();

        // Get all the registered protocolProviders
        Collection<ProtocolProviderService> providers = AccountUtils.getRegisteredProviders();
        for (ProtocolProviderService pps : providers) {
            XMPPConnection connection = pps.getConnection();
            if (connection == null)
                continue;

            OperationSetPresence presenceOpSet = pps.getOperationSet(OperationSetPresence.class);
            if (presenceOpSet != null) {
                presenceOpSet.addContactBlockStatusListener(this);
                presenceOpSets.add(presenceOpSet);

                BlockingCommandManager bcManager = BlockingCommandManager.getInstanceFor(connection);
                try {
                    List<Jid> blockList = bcManager.getBlockList();
                    for (Jid jid : blockList) {
                        Contact contact = presenceOpSet.findContactByJid(jid);
                        // create a volatile contact if not found
                        if (contact == null) {
                            contact = ((OperationSetPersistentPresenceJabberImpl) presenceOpSet).createVolatileContact(jid);
                            volatileContacts.add(contact);
                        }
                        blockContacts.add(contact);
                        contact.setContactBlock(true);
                    }
                } catch (Exception e) {
                    Timber.w("initContactBlockStatus: %s", e.getMessage());
                }
            }
        }
        return blockContacts;
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (OperationSetPresence ops : presenceOpSets) {
            ops.removeContactBlockStatusListener(this);
        }

        // Remove all volatile contacts on exit, else show up in contact list
        new Thread(() -> {
            MetaContactListService metaContactListService = AppGUIActivator.getContactListService();
            for (Contact contact : volatileContacts) {
                try {
                    metaContactListService.removeContact(contact);
                } catch (Exception ex) {
                    Timber.w("Remove contact %s error: %s", contact, ex.getMessage());
                }
            }
        }).start();
    }

    @Override
    public void contactBlockingStatusChanged(Contact contact, boolean blockState) {
        ContactViewHolder contactView = mContactViews.get(contact);
        if (contactView != null) {
            Image blockView = contactView.contactBlockState;
            if (blockView == null) {
                Timber.w("No contact blocking status view found for %s", contact);
                return;
            }

            runOnUiThread(() -> {
                if (contact.isContactBlock())
                    blockView.setPixelMap(ResourceTable.Media_contact_block);
                else
                    blockView.setPixelMap(null);
            });
        }
    }

    public PixelMap getStatusIcon(Contact contact) {
        PresenceStatus presenceStatus = contact.getPresenceStatus();
        if (presenceStatus != null) {
            byte[] statusBlob = StatusUtil.getContactStatusIcon(presenceStatus);
            if (statusBlob != null)
                return AppImageUtil.pixelMapFromBytes(statusBlob);
        }
        return null;
    }

    /**
     * Retrieve the contact from viewHolder to take action on.
     */
    @Override
    public void onLongClicked(Component view) {
        Contact contact = ((ContactViewHolder) view.getTag()).contact;
        EntityListHelper.setEntityBlockState(getContext(), contact, !contact.isContactBlock());
    }

    /**
     * Adapter which displays block state for each contact in the list.
     */
    private class BlockListProvider extends BaseItemProvider {
        /**
         * The list of currently blocked contacts.
         */
        private final List<Contact> contactBlockList;

        /**
         * Creates new instance of <code>BlockListProvider</code>.
         */
        BlockListProvider() {
            contactBlockList = getContactBlockList();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount() {
            return contactBlockList.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Contact getItem(int position) {
            return contactBlockList.get(position);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getComponent(int position, Component convertView, ComponentContainer parent) {
            // Keeps reference to avoid future findViewById()
            ContactViewHolder contactViewHolder;
            if (convertView == null) {
                convertView = LayoutScatter.getInstance(getContext()).parse(ResourceTable.Layout_contact_block_list_row, parent, false);

                contactViewHolder = new ContactViewHolder();
                contactViewHolder.contactAvatar = convertView.findComponentById(ResourceTable.Id_avatarIcon);
                contactViewHolder.contactBlockState = convertView.findComponentById(ResourceTable.Id_contactBlockIcon);
                contactViewHolder.contactStatus = convertView.findComponentById(ResourceTable.Id_contactStatusIcon);

                contactViewHolder.displayName = convertView.findComponentById(ResourceTable.Id_displayName);
                contactViewHolder.statusMessage = convertView.findComponentById(ResourceTable.Id_statusMessage);

                convertView.setTag(contactViewHolder);
            }
            else {
                contactViewHolder = (ContactViewHolder) convertView.getTag();
            }

            // update contact display info
            Contact contact = contactBlockList.get(position);
            contactViewHolder.contact = contact;

            contactViewHolder.displayName.setText(contact.getDisplayName());
            contactViewHolder.statusMessage.setText(contact.getStatusMessage());
            contactViewHolder.statusMessage.setSelected(true); // to start scroll the text.

            // Set avatar.
            byte[] byteAvatar = AvatarManager.getAvatarImageByJid(contact.getJid().asBareJid());
            PixelMap avatar = AppImageUtil.getRoundedCornerPixelMapFromBytes(byteAvatar);
            if (avatar == null) {
                avatar =  AppImageUtil.getPixelMap(getContext(),
                        contact.getJid() instanceof DomainBareJid ? ResourceTable.Media_domain_icon : ResourceTable.Media_contact_avatar);
            }

            contactViewHolder.contactAvatar.setPixelMap(avatar);
            contactViewHolder.contactStatus.setPixelMap(getStatusIcon(contact));

            if (contact.isContactBlock()) {
                contactViewHolder.contactBlockState.setPixelMap(ResourceTable.Media_contact_block);
            }
            else {
                contactViewHolder.contactBlockState.setPixelMap(null);
            }

            convertView.setLongClickedListener(ContactBlockListAbility.this);
            mContactViews.put(contact, contactViewHolder);
            return convertView;
        }
    }

    private static class ContactViewHolder {
        TextField displayName;
        TextField statusMessage;
        Image contactAvatar;
        Image contactBlockState;
        Image contactStatus;
        Contact contact;
    }
}
