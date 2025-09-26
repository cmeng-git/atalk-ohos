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
package org.atalk.ohos.gui.call;

import ohos.aafwk.content.Intent;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.DatePicker;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.agp.components.TimePicker;
import ohos.app.Context;
import ohos.media.image.PixelMap;
import ohos.utils.PlainBooleanArray;

import net.java.sip.communicator.impl.callhistory.CallHistoryActivator;
import net.java.sip.communicator.service.callhistory.CallHistoryService;
import net.java.sip.communicator.service.callhistory.CallPeerRecord;
import net.java.sip.communicator.service.callhistory.CallRecord;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.CallPeerState;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetVideoTelephony;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusListener;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.MListContainer;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuInflater;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.call.telephony.TelephonySlice;
import org.atalk.ohos.gui.chat.ChatSlice;
import org.atalk.ohos.gui.contactlist.model.MetaContactRenderer;
import org.atalk.ohos.gui.util.EntityListHelper;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import timber.log.Timber;

/**
 * The user interface that allows user to view the call record history.
 *
 * @author Eng Chong Meng
 */
public class CallHistorySlice extends BaseSlice
        implements Component.ClickedListener, ContactPresenceStatusListener, EntityListHelper.TaskCompleteListener,
        DatePicker.ValueChangedListener, TimePicker.TimeChangedListener {
    /**
     * A map of <contact, MetaContact>
     */
    private final Map<String, MetaContact> mMetaContacts = new LinkedHashMap<>();

    /**
     * The list of call records
     */
    private final List<CallRecord> callRecords = new ArrayList<>();

    /**
     * The Call record list view adapter for user selection
     */
    private CallHistoryProvider callHistoryProvider;

    /**
     * The call history list view representing the chat.
     */
    private MListContainer callListContainer;

    private Component callDateTime;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private Button btnPurge;
    private Button btnCancel;
    private Text callHistoryWarn;
    private final Calendar calendar = Calendar.getInstance();
    private int mYear, mMonth, mDay;

    /**
     * Component for room configuration title description from the room configuration form
     */
    private Text mTitle;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(Intent intent) {
        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        Component contentView = inflater.parse(ResourceTable.Layout_call_history, null, false);
        mTitle = contentView.findComponentById(ResourceTable.Id_call_history);

        callDateTime = contentView.findComponentById(ResourceTable.Id_call_history_dateTime);
        datePicker = contentView.findComponentById(ResourceTable.Id_datePicker);
        timePicker = contentView.findComponentById(ResourceTable.Id_timePicker);

        callHistoryWarn = contentView.findComponentById(ResourceTable.Id_call_history_warn);
        btnPurge = contentView.findComponentById(ResourceTable.Id_purgeButton);
        btnCancel = contentView.findComponentById(ResourceTable.Id_cancelButton);

        callDateTime.setVisibility(Component.HIDE);

        callListContainer = contentView.findComponentById(ResourceTable.Id_callListContainer);
        callHistoryProvider = new CallHistoryProvider(inflater);
        callListContainer.setItemProvider(callHistoryProvider);

        // Using the contextual action mode with multi-selection
        callListContainer.setMultiChoiceListener(mMultiChoiceListener);
    }

    /**
     * Adapter displaying all the available call history records for user selection.
     */
    private class CallHistoryProvider extends BaseItemProvider {
        private final LayoutScatter mInflater;
        public int CALL_RECORD = 1;

        private CallHistoryProvider(LayoutScatter inflater) {
            mInflater = inflater;
            new getCallRecords(new Date()).execute();
        }

        @Override
        public int getComponentTypeCount() {
            return 1;
        }

        @Override
        public int getCount() {
            return callRecords.size();
        }

        @Override
        public Object getItem(int position) {
            return callRecords.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemComponentType(int position) {
            return CALL_RECORD;
        }

        @Override
        public Component getComponent(int position, Component convertView, ComponentContainer parent) {
            CallRecordViewHolder callRecordViewHolder;
            CallRecord callRecord = callRecords.get(position);

            if (convertView == null) {
                convertView = mInflater.parse(ResourceTable.Layout_call_history_row, parent, false);

                callRecordViewHolder = new CallRecordViewHolder();
                callRecordViewHolder.avatar = convertView.findComponentById(ResourceTable.Id_avatar);
                callRecordViewHolder.callType = convertView.findComponentById(ResourceTable.Id_callType);

                callRecordViewHolder.callButton = convertView.findComponentById(ResourceTable.Id_callButton);
                callRecordViewHolder.callButton.setClickedListener(CallHistorySlice.this);
                callRecordViewHolder.callButton.setTag(callRecordViewHolder);

                callRecordViewHolder.callVideoButton = convertView.findComponentById(ResourceTable.Id_callVideoButton);
                callRecordViewHolder.callVideoButton.setClickedListener(CallHistorySlice.this);
                callRecordViewHolder.callVideoButton.setTag(callRecordViewHolder);

                callRecordViewHolder.callType = convertView.findComponentById(ResourceTable.Id_callType);
                callRecordViewHolder.contactId = convertView.findComponentById(ResourceTable.Id_contactId);
                callRecordViewHolder.callInfo = convertView.findComponentById(ResourceTable.Id_callInfo);

                convertView.setTag(callRecordViewHolder);
            }
            else {
                callRecordViewHolder = (CallRecordViewHolder) convertView.getTag();
            }

            callRecordViewHolder.childPosition = position;

            // Must init child Tag here as reused convertView may not necessary contains the correct crWrapper
            // Component callInfoView = convertView.findComponentById(ResourceTable.Id_callInfoView);
            // callInfoView.setClickedListener(CallHistorySlice.this);
            // callInfoView.setOnLongClickListener(CallHistorySlice.this);

            CallPeerRecord peerRecord = callRecord.getPeerRecords().get(0);
            String peer = peerRecord.getPeerAddress();
            MetaContact metaContact = mMetaContacts.get(peer.split("/")[0]);
            callRecordViewHolder.metaContact = metaContact;

            if (metaContact != null) {
                PixelMap avatar = MetaContactRenderer.getAvatarPixelMap(metaContact);
                ChatSlice.setAvatar(callRecordViewHolder.avatar, avatar);
            }
            setCallState(callRecordViewHolder.callType, callRecord);

            callRecordViewHolder.callButton.setVisibility(isShowCallBtn(metaContact) ? Component.VISIBLE : Component.HIDE);
            callRecordViewHolder.callVideoButton.setVisibility(isShowVideoCallBtn(metaContact) ? Component.VISIBLE : Component.HIDE);

            callRecordViewHolder.contactId.setText(peerRecord.getPeerAddress());
            callRecordViewHolder.callInfo.setText(callRecord.toString());

            return convertView;
        }

        /**
         * Retrieve the call history records from locally stored database
         * Populate the fragment with the call record for use in getComponent()
         */
        private class getCallRecords {
            final Date mEndDate;

            public getCallRecords(Date date) {
                mEndDate = date;
                callRecords.clear();
                mMetaContacts.clear();
            }

            public void execute() {
                Executors.newSingleThreadExecutor().execute(() -> {
                    doInBackground();

                    runOnUiThread(() -> {
                        if (!callRecords.isEmpty()) {
                            callHistoryProvider.notifyDataChanged();
                        }
                        setTitle();
                    });
                });
            }

            private void doInBackground() {
                initMetaContactList();
                Collection<CallRecord> callRecordPPS;
                CallHistoryService CHS = CallHistoryActivator.getCallHistoryService();

                Collection<ProtocolProviderService> providers = AccountUtils.getRegisteredProviders();
                for (ProtocolProviderService pps : providers) {
                    if ((pps.getConnection() != null) && pps.getConnection().isAuthenticated()) {
                        addContactStatusListener(pps);
                        AccountID accountId = pps.getAccountID();
                        String userUuId = accountId.getAccountUid();

                        callRecordPPS = CHS.findByEndDate(userUuId, mEndDate);
                        if (!callRecordPPS.isEmpty())
                            callRecords.addAll(callRecordPPS);
                    }
                }
            }
        }
    }

    /**
     * Adds the given <code>addContactPresenceStatusListener</code> to listen for contact presence status change.
     *
     * @param pps the <code>ProtocolProviderService</code> for which we add the listener.
     */
    private void addContactStatusListener(ProtocolProviderService pps) {
        OperationSetPresence presenceOpSet = pps.getOperationSet(OperationSetPresence.class);
        if (presenceOpSet != null) {
            presenceOpSet.removeContactPresenceStatusListener(this);
            presenceOpSet.addContactPresenceStatusListener(this);
        }
    }

    /**
     * Sets the call state.
     *
     * @param callStateView the call state image view
     * @param callRecord the call record.
     */
    private void setCallState(Image callStateView, CallRecord callRecord) {
        CallPeerRecord peerRecord = callRecord.getPeerRecords().get(0);
        CallPeerState callState = peerRecord.getState();
        int resId;

        if (CallRecord.IN.equals(callRecord.getDirection())) {
            if (callState == CallPeerState.CONNECTED)
                resId = ResourceTable.Media_call_incoming;
            else
                resId = ResourceTable.Media_missed_call;
        }
        else {
            resId = ResourceTable.Media_call_outgoing;
        }
        callStateView.setPixelMap(resId);
    }

    private void setTitle() {
        String title = aTalkApp.getResString(ResourceTable.String_call_history_name)
                + " (" + callRecords.size() + ")";
        mTitle.setText(title);
    }

    // Handle only if contactImpl instanceof MetaContact;
    private boolean isShowCallBtn(Object contactImpl) {
        if (contactImpl instanceof MetaContact) {
            MetaContact metaContact = (MetaContact) contactImpl;

            boolean isDomainJid = false;
            if (metaContact.getDefaultContact() != null)
                isDomainJid = metaContact.getDefaultContact().getJid() instanceof DomainBareJid;

            return isDomainJid || isShowButton(metaContact, OperationSetBasicTelephony.class);
        }
        return false;
    }

    public boolean isShowVideoCallBtn(Object contactImpl) {
        return (contactImpl instanceof MetaContact)
                && isShowButton((MetaContact) contactImpl, OperationSetVideoTelephony.class);
    }

    private boolean isShowButton(MetaContact metaContact, Class<? extends OperationSet> opSetClass) {
        return ((metaContact != null) && metaContact.getOpSetSupportedContact(opSetClass) != null);
    }

    /**
     * Initializes the adapter data.
     */
    public void initMetaContactList() {
        MetaContactListService contactListService = AppGUIActivator.getContactListService();
        if (contactListService != null) {
            addContacts(contactListService.getRoot());
        }
    }

    /**
     * Adds all child contacts for the given <code>group</code>. Omit metaGroup of zero child.
     *
     * @param group the group, which child contacts to add
     */
    private void addContacts(MetaContactGroup group) {
        if (group.countChildContacts() > 0) {

            // Use Iterator to avoid ConcurrentModificationException on addContact()
            Iterator<MetaContact> childContacts = group.getChildContacts();
            while (childContacts.hasNext()) {
                MetaContact metaContact = childContacts.next();
                String contactId = metaContact.getDefaultContact().getAddress();
                mMetaContacts.put(contactId, metaContact);
            }
        }

        Iterator<MetaContactGroup> subGroups = group.getSubgroups();
        while (subGroups.hasNext()) {
            addContacts(subGroups.next());
        }
    }

    @Override
    public void contactPresenceStatusChanged(ContactPresenceStatusChangeEvent evt) {
        runOnUiThread(() -> callHistoryProvider.notifyDataChanged());
    }

    @Override
    public void onTaskComplete(int msgCount, List<String> deletedUUIDs) {
        aTalkApp.showToastMessage(ResourceTable.String_history_purge_count, msgCount);
        if (msgCount > 0) {
            callHistoryProvider.new getCallRecords(new Date()).execute();
        }
    }

    @Override
    public void onClick(Component view) {
        CallRecordViewHolder viewHolder = null;

        Object object = view.getTag();
        if (object instanceof CallRecordViewHolder) {
            viewHolder = (CallRecordViewHolder) view.getTag();
            // int childPos = viewHolder.childPosition;
            object = viewHolder.metaContact;
        }

        if (object instanceof MetaContact) {
            MetaContact metaContact = (MetaContact) object;
            Contact contact = metaContact.getDefaultContact();

            if (contact != null) {
                Jid jid = contact.getJid();

                switch (view.getId()) {
                    case ResourceTable.Id_callButton:
                        if (jid instanceof DomainBareJid) {
                            TelephonySlice extPhone = TelephonySlice.newInstance(contact.getAddress());
//                            ((AbilitySlice) mContext).getSupportFragmentManager().beginTransaction()
//                                    .replace(ResourceTable.Id_content, extPhone).commit();
                            getAbility().setMainRoute(extPhone.getClass().getCanonicalName());
                            break;
                        }

                    case ResourceTable.Id_callVideoButton:
                        if (viewHolder != null) {
                            boolean isVideoCall = viewHolder.callVideoButton.isPressed();
                            AppCallUtil.createAndroidCall(aTalkApp.getInstance(), jid,
                                    viewHolder.callVideoButton, isVideoCall);
                        }
                        break;

                    default:
                        break;
                }
            }
        }
        else {
            Timber.w("Clicked item is not a valid MetaContact");
        }
    }

    /**
     * ActionMode with multi-selection implementation for chatListContainer
     * AbsListContainer not available in ohos
     */
    private final MListContainer.MultiChoiceListener mMultiChoiceListener = new MListContainer.MultiChoiceListener() {
        int cPos;
        int headerCount;
        int checkListSize;
        PlainBooleanArray checkedList = new PlainBooleanArray();

        @Override
        public void onItemCheckStateChanged(ListContainer listContainer, Component component, int pos, long id, boolean checked) {
            // Here you can do something when items are selected/de-selected
            checkedList = callListContainer.getCheckedItemPositions();
            checkListSize = checkedList.size();
            int checkedItemCount = callListContainer.getCheckedItemCount();

            // Position must be aligned to the number of header views included
            cPos = pos - headerCount;

            callListContainer.setSelection(pos);
//            mode.invalidate();
//            mode.setTitle(String.valueOf(checkedItemCount));
        }

        // Called when the user selects a menu item. On action picked, close the CAB i.e. mode.terminateAbility();
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int cType;
            CallRecord callRecord;

            switch (item.getItemId()) {
                case ResourceTable.Id_cr_delete_older:
                    if (checkedList.size() > 0 && checkedList.valueAt(0)) {
                        cPos = checkedList.keyAt(0) - headerCount;
                        cType = callHistoryProvider.getItemViewType(cPos);
                        if (cType == callHistoryProvider.CALL_RECORD) {
                            callRecord = (CallRecord) callHistoryProvider.getItem(cPos);
                            if (callRecord != null) {
                                eraseCallHistory(callRecord);
                                mode.finish();
                            }
                        }
                    }
                    return true;

                case ResourceTable.Id_cr_select_all:
                    int size = callHistoryProvider.getCount();
                    if (size < 2)
                        return true;

                    for (int i = 0; i < size; i++) {
                        cPos = i + headerCount;
                        checkedList.put(cPos, true);
                        callListContainer.setSelection(cPos);
                    }
                    checkListSize = size;
                    mode.invalidate();
                    mode.setTitle(String.valueOf(size));
                    return true;

                case ResourceTable.Id_cr_delete:
                    if (checkedList.size() == 0) {
                        aTalkApp.showToastMessage(ResourceTable.String_call_history_remove_none);
                        return true;
                    }

                    List<String> callUuidDel = new ArrayList<>();
                    for (int i = 0; i < checkListSize; i++) {
                        if (checkedList.valueAt(i)) {
                            cPos = checkedList.keyAt(i) - headerCount;
                            cType = callHistoryProvider.getItemViewType(cPos);
                            if (cType == callHistoryProvider.CALL_RECORD) {
                                callRecord = (CallRecord) callHistoryProvider.getItem(cPos);
                                if (callRecord != null) {
                                    callUuidDel.add(callRecord.getCallUuid());
                                }
                            }
                        }
                    }
                    EntityListHelper.eraseEntityCallHistory(CallHistorySlice.this, callUuidDel);
                    mode.finish();
                    return true;

                default:
                    return false;
            }
        }

        // Called when the action ActionMode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            inflater.parse(ResourceTable.Layout_menu_call_history, menu);
            headerCount = callListContainer.getHeaderViewsCount();
            return true;
        }

        // Called each time the action ActionMode is shown. Always called after onCreateActionMode,
        // but may be called multiple times if the ActionMode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Here you can perform updates to the CAB due to an invalidate() request
            // Return false if nothing is done
            return false;
        }

        // Called when the user exits the action ActionMode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Here you can make any necessary updates to the activity when
            // the CAB is removed. By default, selected items are deselected/unchecked.
            callHistoryProvider.new getCallRecords(new Date()).execute();
        }
    };

    public void eraseCallHistory(final CallRecord callRecord) {
        callDateTime.setVisibility(Component.VISIBLE);
        Date sDate = callRecord.getStartTime();

        calendar.setTime(sDate);
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH);
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
        int mHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int mMinute = calendar.get(Calendar.MINUTE);
        callHistoryWarn.setText(getString(ResourceTable.String_call_history_remove_before_date_warning, calendar.getTime()));

        datePicker.init(mYear, mMonth, mDay, this);
        timePicker.set24Hour(true);
        timePicker.setHour(mHourOfDay);
        timePicker.setMinute(mMinute);
        timePicker.setTimeChangedListener(this);

        btnPurge.setClickedListener(v -> {
            EntityListHelper.eraseEntityCallHistory(this, calendar.getTime());
            callDateTime.setVisibility(Component.HIDE);
        });
        btnCancel.setClickedListener(v -> callDateTime.setVisibility(Component.HIDE));
    }

    @Override
    public void onValueChanged(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
        calendar.set(year, monthOfYear, dayOfMonth);
        callHistoryWarn.setText(getString(ResourceTable.String_call_history_remove_before_date_warning, calendar.getTime()));
    }

    @Override
    public void onTimeChanged(TimePicker timePicker, int hourOfDay, int minute, int second) {
        // must also set year/month/day; these values may get messed up by onTimeChanged().
        calendar.set(mYear, mMonth, mDay, hourOfDay, minute);
        callHistoryWarn.setText(getString(ResourceTable.String_call_history_remove_before_date_warning, calendar.getTime()));
    }

    private static class CallRecordViewHolder {
        Image avatar;
        Image callType;
        Image callButton;
        Image callVideoButton;

        Text contactId;
        Text callInfo;
        MetaContact metaContact;

        int childPosition;
    }
}
