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
package org.atalk.ohos.gui.call.telephony;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.app.Context;

import net.java.sip.communicator.impl.protocol.jabber.OperationSetBasicTelephonyJabberImpl;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.jabber.JabberAccountID;
import net.java.sip.communicator.util.account.AccountUtils;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.account.Account;
import org.atalk.ohos.gui.account.AccountsListProvider;
import org.atalk.ohos.gui.call.AppCallUtil;
import org.atalk.ohos.util.ComponentUtil;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.java.sip.communicator.impl.protocol.jabber.OperationSetBasicTelephonyJabberImpl.GOOGLE_VOICE_DOMAIN;

/**
 * This activity allows user to make pbx phone call via the selected service gateway.
 * The server must support pbx phone call via gateway i.e.
 * <feature var='urn:xmpp:jingle:apps:rtp:audio'/>
 * <feature var='urn:xmpp:jingle:apps:rtp:video'/>
 *
 * @author Eng Chong Meng
 */
public class TelephonySlice extends BaseSlice {
    public static final String TELEPHONY_TAG = "telephonyFragment";
    private static String mLastJid = null;
    private static String mDomainJid;

    private Context mContext;
    AbilitySlice fragmentAbility;
    private ListContainer accountsSpinner;
    private RecipientSelectView vRecipient;
    private Text vTelephonyDomain;

    private ProtocolProviderService mPPS;

    public TelephonySlice() {
        mDomainJid = null;
    }

    public static TelephonySlice newInstance(String domainJid) {
        TelephonySlice telephonySlice = new TelephonySlice();
        mDomainJid = domainJid;
        return telephonySlice;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        fragmentAbility = getAbility();
    }

    @Override
    public void onStart(Intent intent) {
        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        Component content = inflater.parse(ResourceTable.Layout_telephony, null, false);

        vRecipient = content.findComponentById(ResourceTable.Id_address);
        vRecipient.addTextChangedListener(new Text.TextObserver() {
            public void afterTextChanged(Editable s) {
                if (!vRecipient.isEmpty()) {
                    mLastJid = vRecipient.getAddresses()[0].getAddress();
                }
                // to prevent device rotate from changing it to null - not working
                else {
                    mLastJid = ComponentUtil.toString(vRecipient);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        if (mLastJid != null)
            vRecipient.setText(mLastJid);

        vTelephonyDomain = content.findComponentById(ResourceTable.Id_telephonyDomain);

        accountsSpinner = content.findComponentById(ResourceTable.Id_selectAccountSpinner);
        accountsSpinner.setItemSelectedListener(new ListContainer.ItemSelectedListener() {
            @Override
            public void onItemSelected(ListContainer parentView, Component selectedItemView, int position, long id) {
                Account selectedAcc = (Account) accountsSpinner.getSelectedItem();
                mPPS = selectedAcc.getProtocolProvider();
                JabberAccountID accountJID = (JabberAccountID) mPPS.getAccountID();
                String telephonyDomain = accountJID.getOverridePhoneSuffix();
                if (TextUtils.isEmpty(telephonyDomain)) {
                    // StrictMode$AndroidBlockGuardPolicy.onNetwork(StrictMode.java:1448); so a simple check only instead of SRV
                    // boolean isGoogle =  mPPS.isGmailOrGoogleAppsAccount();
                    boolean isGoogle = accountJID.toString().contains("google.com");
                    if (isGoogle) {
                        String bypassDomain = accountJID.getTelephonyDomainBypassCaps();
                        if (!TextUtils.isEmpty(bypassDomain))
                            telephonyDomain = bypassDomain;
                        else
                            telephonyDomain = GOOGLE_VOICE_DOMAIN;
                    }
                    else
                        telephonyDomain = accountJID.getService();
                }
                mDomainJid = telephonyDomain;
                vTelephonyDomain.setText(telephonyDomain);
            }
        });

        initAccountSpinner();
        initButton(content);
        return content;
    }

    /**
     * Initializes accountIDs spinner selector with existing registered accounts.
     */
    private void initAccountSpinner() {
        int idx = 0;
        int selectedIdx = -1;
        List<AccountID> accounts = new ArrayList<>();

        Collection<ProtocolProviderService> providers = AccountUtils.getRegisteredProviders();
        for (ProtocolProviderService provider : providers) {
            OperationSet opSet = provider.getOperationSet(OperationSetPresence.class);
            if (opSet != null) {
                AccountID accountID = provider.getAccountID();
                accounts.add(accountID);
                if ((selectedIdx == -1) && (mDomainJid != null)) {
                    if (mDomainJid.contains(accountID.getService()))
                        selectedIdx = idx;
                }
                idx++;
            }
        }
        AccountsListProvider accountsAdapter = new AccountsListProvider(getContext(),
                ResourceTable.Layout_select_account_row, ResourceTable.Layout_select_account_dropdown, accounts, true);
        accountsSpinner.setItemProvider(accountsAdapter);

        // if we have only select account option and only one account select the available account
        if (accounts.size() == 1)
            accountsSpinner.setSelectedItemIndex(0);
        else
            accountsSpinner.setSelectedItemIndex(selectedIdx);
    }

    /**
     * Initializes the button click actions.
     */
    private void initButton(final Component content) {
        final Button buttonAudio = content.findComponentById(ResourceTable.Id_button_audio);
        buttonAudio.setClickedListener(v -> onCallClicked(false));

        final Button buttonVideo = content.findComponentById(ResourceTable.Id_button_video);
        buttonVideo.setClickedListener(v -> onCallClicked(true));

        final Button buttonCancel = content.findComponentById(ResourceTable.Id_button_cancel);
        buttonCancel.setClickedListener(v -> closeFragment());
    }

    /**
     * Method fired when one of the call buttons is clicked.
     *
     * @param videoCall vide call is true else audio call
     */
    private void onCallClicked(boolean videoCall) {
        String recipient;
        if (!vRecipient.isEmpty()) {
            recipient = vRecipient.getAddresses()[0].getAddress();
        }
        else {
            recipient = ComponentUtil.toString(vRecipient);
        }
        if (recipient == null) {
            // aTalkApp.showToastMessage(ResourceTable.String_service_gui_NO_ONLINE_TELEPHONY_ACCOUNT);
            aTalkApp.showToastMessage(ResourceTable.String_contact_phone_empty);
            return;
        }

        recipient = recipient.replace(" ", "");
        mLastJid = recipient;

        if (!recipient.contains("@")) {
            String telephonyDomain = ComponentUtil.toString(vTelephonyDomain);
            recipient += "@" + telephonyDomain;
        }

        Jid phoneJid;
        try {
            phoneJid = JidCreate.from(recipient);
        } catch (XmppStringprepException | IllegalArgumentException e) {
            aTalkApp.showToastMessage(ResourceTable.String_unknown_recipient);
            return;
        }

        // Must init the Sid if call not via JingleMessage
        OperationSetBasicTelephonyJabberImpl basicTelephony = (OperationSetBasicTelephonyJabberImpl)
                mPPS.getOperationSet(OperationSetBasicTelephony.class);
        basicTelephony.initSid();

        AppCallUtil.createCall(mContext, mPPS, phoneJid, videoCall);
        closeFragment();
    }

    public boolean closeFragment() {
        AbilitySlice phoneFragment = fragmentAbility.getSupportFragmentManager().findFragmentByTag(TELEPHONY_TAG);
        if (phoneFragment != null) {
            fragmentAbility.getSupportFragmentManager().beginTransaction().remove(phoneFragment).commit();
            return true;
        }
        return false;
    }
}

