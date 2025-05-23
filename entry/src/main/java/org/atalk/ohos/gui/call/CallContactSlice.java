/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.call;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.security.SystemPermission;

import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.account.AccountUtils;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.account.Account;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.gui.dialogs.PopupMenu;
import org.atalk.ohos.util.ComponentUtil;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.roster.Roster;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.Collection;
import timber.log.Timber;

/**
 * Tha <code>CallContactSlice</code> encapsulated GUI used to make a call.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class CallContactSlice extends BaseSlice {
    /**
     * The bundle context.
     */
    private BundleContext bundleContext;

    /**
     * Optional phone number argument.
     */
    public static String ARG_PHONE_NUMBER = "arg.phone_number";

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start(BundleContext bundleContext)
            throws Exception {
        super.start(bundleContext);
        /*
         * If there are unit tests to be run, do not run anything else and just perform
         * the unit tests.
         */
        if (System.getProperty("net.java.sip.communicator.slick.runner.TEST_LIST") != null)
            return;

        this.bundleContext = bundleContext;
        initAndroidAccounts();
    }

    @Override
    public void onStart(Intent intent) {
        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        final Component content = inflater.parse(ResourceTable.Layout_call_contact, container, false);

        final Image callButton = content.findComponentById(ResourceTable.Id_callButtonFull);
        callButton.setClickedListener(v -> {
            String contact = ComponentUtil.toString(content.findComponentById(ResourceTable.Id_callField));
            if (contact == null) {
                System.err.println("Contact is empty");
            }
            else {
                showCallViaMenu(callButton, contact);
            }
        });

        // Call intent handling
        String phoneNumber = intent.getStringParam(ARG_PHONE_NUMBER);
        if (!TextUtils.isEmpty(phoneNumber)) {
            ComponentUtil.setTextViewValue(content, ResourceTable.Id_callField, phoneNumber);
        }
    }

    /**
     * Shows "call via" menu allowing user to selected from multiple providers if available.
     *
     * @param v the Component that will contain the popup menu.
     * @param calleeAddress target callee name.
     */
    private void showCallViaMenu(Component v, final String calleeAddress) {
        PopupMenu popup = new PopupMenu(getAbility(), v);
        DirectionalLayout menu = popup.getMenu();
        ProtocolProviderService mProvider = null;

        Collection<ProtocolProviderService> onlineProviders = AccountUtils.getOnlineProviders();

        for (final ProtocolProviderService provider : onlineProviders) {
            XMPPConnection connection = provider.getConnection();
            try {
                if (Roster.getInstanceFor(connection).contains(JidCreate.bareFrom(calleeAddress))) {

                    String accountAddress = provider.getAccountID().getAccountJid();
                    Text menuItem = popup.addMenuItem(accountAddress);
                    menuItem.setClickedListener(item -> {
                        createCall(provider, calleeAddress);
                    });
                    mProvider = provider;
                }
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
        }
        if (menu.getChildCount() > 1)
            popup.show();
        else
            createCall(mProvider, calleeAddress);
    }

    /**
     * Creates new call to given <code>destination</code> using selected <code>provider</code>.
     *
     * @param destination target callee name.
     * @param provider the provider that will be used to make a call.
     */
    private void createCall(final ProtocolProviderService provider, final String destination) {
        new Thread() {
            public void run() {
                try {
                    CallManager.createCall(provider, destination, false);
                } catch (Throwable t) {
                    Timber.e(t, "Error creating the call: %s", t.getMessage());
                    DialogH.getInstance(getContext()).showDialog(getContext(), getString(ResourceTable.String_error), t.getMessage());
                }
            }
        }.start();
    }

    /**
     * Loads Android accounts.
     */
    public void initAndroidAccounts() {
        if (aTalk.hasPermission(getAbility(), true,
                aTalk.PRC_GET_CONTACTS, SystemPermission.GET_ALL_APP_ACCOUNTS)) {
            android.accounts.AccountManager androidAccManager = android.accounts.AccountManager.get(getAbility());
            Account[] androidAccounts = androidAccManager.getAccountsByType(getString(ResourceTable.String_aTalk_account_type));
            for (Account account : androidAccounts) {
                System.err.println("ACCOUNT======" + account);
            }
        }
    }

    /**
     * Creates new parametrized instance of <code>CallContactSlice</code>.
     *
     * @param phoneNumber optional phone number that will be filled.
     *
     * @return new parameterized instance of <code>CallContactSlice</code>.
     */
    public static CallContactSlice newInstance(String phoneNumber) {
        CallContactSlice ccFragment = new CallContactSlice();
        Bundle args = new Bundle();
        args.putString(ARG_PHONE_NUMBER, phoneNumber);

        ccFragment.setArguments(args);
        return ccFragment;
    }
}