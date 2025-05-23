/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.call;

import ohos.aafwk.ability.Ability;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Text;
import ohos.app.Context;

import net.java.sip.communicator.impl.protocol.jabber.OperationSetBasicTelephonyJabberImpl;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.gui.dialogs.PopupMenu;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smackx.jinglemessage.element.JingleMessage;
import org.jxmpp.jid.Jid;

import timber.log.Timber;

/**
 * @author Eng Chong Meng
 */
public class AppCallUtil {
    /**
     * Field used to track the thread used to create outgoing calls.
     */
    private static Thread createCallThread;

    /**
     * Creates an app call.
     *
     * @param context the android context
     * @param contact the contact address to call
     * @param callButton the button view that generated the call
     * @param isVideoCall true to setup video call
     */
    public static void createAndroidCall(Context context, Jid contact, Component callButton, boolean isVideoCall) {
        showCallViaMenu(context, contact, callButton, isVideoCall);
    }

    /**
     * Shows "call via" menu allowing user to select from multiple providers.
     *
     * @param context the android context
     * @param calleeJid the target callee name that will be used.
     * @param v the Component that will contain the popup menu.
     * @param isVideoCall true for video call setup
     */
    private static void showCallViaMenu(final Context context, final Jid calleeJid, Component v, final boolean isVideoCall) {
        PopupMenu popup = new PopupMenu(context, v);
        ComponentContainer menu = popup.getMenu();
        ProtocolProviderService mProvider = null;

        // loop through all registered providers to find the callee own provider
        for (final ProtocolProviderService provider : AccountUtils.getOnlineProviders()) {
            XMPPConnection connection = provider.getConnection();
            if (Roster.getInstanceFor(connection).contains(calleeJid.asBareJid())) {
                String accountAddress = provider.getAccountID().getAccountJid();
                Text menuItem = popup.addMenuItem(accountAddress);
                menuItem.setClickedListener(item -> {
                    createCall(context, provider, calleeJid, isVideoCall);
                });
                mProvider = provider;
            }
            // Pre-assigned current provider in case the calleeAddress is not listed in roaster;
            // e.g call contact from phone book - user the first available
            if (mProvider == null)
                mProvider = provider;
        }

        // Show contact selection menu if more than one choice
        if (menu.getChildCount() > 1) {
            popup.show();
        }
        else if (mProvider != null)
            createCall(context, mProvider, calleeJid, isVideoCall);
    }

    /**
     * Creates new call to given <code>destination</code> using selected <code>provider</code>.
     *
     * @param context the android context
     * @param metaContact target callee metaContact.
     * @param isVideoCall true to setup video call
     * @param callButtonView not null if call via contact list fragment.
     */
    public static void createCall(Context context, MetaContact metaContact, boolean isVideoCall, Component callButtonView) {
        // Check for resource permission before continue
        if (!aTalk.isMediaCallAllowed(isVideoCall)) {
            Timber.w("createCall permission denied #1: %s", isVideoCall);
            return;
        }

        Contact contact = metaContact.getDefaultContact();
        Jid callee = contact.getJid();
        ProtocolProviderService pps = contact.getProtocolProvider();
        if (!pps.isRegistered()) {
            aTalkApp.showToastMessage(ResourceTable.String_create_call_failed);
            return;
        }

        boolean isJmSupported = metaContact.isFeatureSupported(JingleMessage.NAMESPACE);
        if (isJmSupported) {
            JingleMessageSessionImpl.sendJingleMessagePropose(pps.getConnection(), callee, isVideoCall);
        }
        else {
            // Must init the Sid if call not via JingleMessage
            OperationSetBasicTelephonyJabberImpl basicTelephony = (OperationSetBasicTelephonyJabberImpl)
                    pps.getOperationSet(OperationSetBasicTelephony.class);
            basicTelephony.initSid();
            if (callButtonView != null) {
                showCallViaMenu(context, callee, callButtonView, isVideoCall);
            }
            else {
                createCall(context, pps, callee, isVideoCall);
            }
        }
    }

    /**
     * Creates new call to given <code>destination</code> using selected <code>provider</code>.
     *
     * @param context the android context
     * @param provider the provider that will be used to make a call.
     * @param callee target callee Jid.
     * @param isVideoCall true for video call setup
     */
    public static void createCall(final Context context, final ProtocolProviderService provider,
            final Jid callee, final boolean isVideoCall) {
        if (!aTalk.isMediaCallAllowed(isVideoCall)) {
            Timber.w("createCall permission denied #2: %s", isVideoCall);
            return;
        }

        // Force to null assuming user is making a call seeing no call in progress, otherwise cannot make call at all
        if (createCallThread != null) {
            Timber.w("Another call is being created; restarting call thread!");
            createCallThread = null;
        }
        // Allow max of 2 outgoing calls for attended call transfer support
        else if (CallManager.getActiveCallsCount() > 1) {
            aTalkApp.showToastMessage(ResourceTable.String_call_max_transfer);
            return;
        }
        // cmeng (20210319: Seems to have no chance to show; and it causes waitForDialogOpened() (10s) error often, so remove it
//        final long dialogId = ProgressDialog.showProgressDialog(
//                aTalkApp.getResString(ResourceTable.String_service_gui_CALL_OUTGOING),
//                aTalkApp.getResString(ResourceTable.String_service_gui_CALL_OUTGOING_MSG, callee));

        createCallThread = new Thread("Create call thread") {
            public void run() {
                try {
                    CallManager.createCall(provider, callee.toString(), isVideoCall);
                } catch (Throwable t) {
                    Timber.e(t, "Error creating the call: %s", t.getMessage());
                    DialogH.getInstance(context).showDialog(context, context.getString(ResourceTable.String_error), t.getMessage());
                } finally {
                    createCallThread = null;
                }
            }
        };
        createCallThread.start();
    }

    /**
     * Checks if there is a call in progress. If true then shows a warning toast and finishes the activity.
     *
     * @param activity activity doing a check.
     *
     * @return <code>true</code> if there is call in progress and <code>Ability</code> was finished.
     */
    public static boolean checkCallInProgress(Ability activity) {
        if (CallManager.getActiveCallsCount() > 0) {
            Timber.w("Call is in progress");
            aTalkApp.showToastMessage(ResourceTable.String_call_in_progress_warning);
            activity.terminateAbility();
            return true;
        }
        else {
            return false;
        }
    }
}
