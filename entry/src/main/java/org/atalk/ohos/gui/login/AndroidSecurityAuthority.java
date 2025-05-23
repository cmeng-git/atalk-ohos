/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.login;

import ohos.agp.components.Component;
import ohos.agp.components.Picker;
import ohos.app.Context;
import ohos.eventhandler.EventRunner;
import ohos.utils.PacMap;

import net.java.sip.communicator.impl.msghistory.MessageHistoryActivator;
import net.java.sip.communicator.impl.msghistory.MessageHistoryServiceImpl;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.UserCredentials;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.util.ComponentUtil;

import timber.log.Timber;

/**
 * Android <code>SecurityAuthority</code> implementation.
 * The method checks for valid reason based on given reasonCode. Pending on the given reason, it
 * either launches a user login dialog or displays an error message.
 * When launching a login dialog, it will waits until the right activity is in view before
 * displaying the dialog to the user. Otherwise the dialog may be obscured by other activity display
 * windows. The login dialog menu allows user to change certain account settings before signing in.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AndroidSecurityAuthority implements SecurityAuthority {
    /**
     * If user name should be editable when asked for credentials.
     */
    private boolean isUserNameEditable = true;

    /**
     * user last entered userName to check for anymore new changes in userName
     */
    private String userNameLastEdited;

    /**
     * Returns a UserCredentials object associated with the specified realm (accountID), by
     * specifying the reason of this operation. Or display an error message.
     *
     * @param accountID The realm (accountID) that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @param reasonCode indicates the reason for which we're obtaining the credentials.
     *
     * @return The credentials associated with the specified realm or null if none could be obtained.
     */
    public UserCredentials obtainCredentials(AccountID accountID, UserCredentials defaultValues,
            int reasonCode, Boolean isShowServerOption) {
        if (reasonCode != SecurityAuthority.REASON_UNKNOWN) {
            return obtainCredentials(accountID, defaultValues, isShowServerOption);
        }

        Context ctx = aTalkApp.getInstance();
        String errorMessage = ctx.getString(ResourceTable.String_connection_failed_message,
                defaultValues.getUserName(), defaultValues.getServerAddress());

        DialogH.getInstance(ctx).showDialog(ctx, ctx.getString(ResourceTable.String_login_failed), errorMessage);
        return defaultValues;
    }

    /**
     * Returns a UserCredentials object associated with the specified realm (AccountID), by
     * specifying the reason of this operation.
     *
     * @param accountID The accountId / realm that the credentials are needed for.
     * @param credentials the values to propose the user by default
     *
     * @return The credentials associated with the specified realm or null if none could be obtained.
     */
    public UserCredentials obtainCredentials(final AccountID accountID,
            final UserCredentials credentials, final Boolean isShowServerOption) {
        if (EventRunner.current() == EventRunner.getMainEventRunner()) {
            Timber.e("Cannot obtain credentials from the main thread!");
            return credentials;
        }

        // Insert DialogH arguments
        PacMap args = new PacMap();
        // Login userName and editable state
        String userName = credentials.getUserName();
        userNameLastEdited = userName;
        args.putString(CredentialsComponent.ARG_LOGIN, userName);
        args.putBooleanValue(CredentialsComponent.ARG_LOGIN_EDITABLE, isUserNameEditable);

        // Password argument
        char[] password = credentials.getPassword();
        if (password != null) {
            args.putString(CredentialsComponent.ARG_PASSWORD, credentials.getPasswordAsString());
        }

        String dnssecMode = accountID.getDnssMode();
        args.putString(CredentialsComponent.ARG_DNSSEC_MODE, dnssecMode);

        // Persistent password argument
        args.putBooleanValue(CredentialsComponent.ARG_STORE_PASSWORD, credentials.isPasswordPersistent());

        // InBand Registration argument
        args.putBooleanValue(CredentialsComponent.ARG_IB_REGISTRATION, accountID.isIbRegistration());
        args.putString(CredentialsComponent.ARG_CERT_ID, accountID.getTlsClientCertificate());

        args.putBooleanValue(CredentialsComponent.ARG_IS_SHOWN_SERVER_OPTION, isShowServerOption);
        if (isShowServerOption) {
            // Server overridden argument
            args.putBooleanValue(CredentialsComponent.ARG_IS_SERVER_OVERRIDDEN, accountID.isServerOverridden());
            args.putString(CredentialsComponent.ARG_SERVER_ADDRESS, accountID.getServerAddress());
            args.putString(CredentialsComponent.ARG_SERVER_PORT, accountID.getServerPort());
        }
        args.putString(CredentialsComponent.ARG_LOGIN_REASON, credentials.getLoginReason());

        Context ctx = aTalkApp.getInstance();
        CredentialsComponent component = new CredentialsComponent(ctx, args);
        // Obtain credentials lock
        final Object credentialsLock = new Object();

        aTalkApp.waitForFocus();

        // Displays the credentials dialog and waits for it to complete
        DialogH.getInstance(ctx).showCustomDialog(ctx, ctx.getString(ResourceTable.String_login_credential), component,
                ctx.getString(ResourceTable.String_sign_in), new DialogH.DialogListener() {
                    public boolean onConfirmClicked(DialogH dialog) {
                        Component dialogContent = component.findComponentById(ResourceTable.Id_alertContent);
                        String userNameEntered = ComponentUtil.getTextViewValue(dialogContent, ResourceTable.Id_username);
                        String password = ComponentUtil.getTextViewValue(dialogContent, ResourceTable.Id_password);

                        boolean storePassword = ComponentUtil.isCompoundChecked(dialogContent, ResourceTable.Id_store_password);
                        boolean ibRegistration = ComponentUtil.isCompoundChecked(dialogContent, ResourceTable.Id_ib_registration);

                        if (!userNameLastEdited.equals(userNameEntered)) {
                            int msgCount = checkPurgedMsgCount(accountID.getAccountUid(), userNameEntered);
                            if (msgCount < 0) {
                                userNameLastEdited = userName;
                                ComponentUtil.setTextViewValue(dialogContent, ResourceTable.Id_reason_field,
                                        ctx.getString(ResourceTable.String_username_password_null));
                                return false;
                            }
                            else if (msgCount > 0) {
                                String msgReason = ctx.getString(ResourceTable.String_username_change_warning,
                                        userNameEntered, msgCount, userName);
                                ComponentUtil.setTextViewValue(dialogContent, ResourceTable.Id_reason_field, msgReason);
                                ComponentUtil.setTextViewColor(dialogContent, ResourceTable.Id_reason_field, ResourceTable.Color_red);
                                userNameLastEdited = userNameEntered;
                                return false;
                            }
                        }

                        credentials.setUserName(userNameEntered);
                        credentials.setPassword((password != null) ? password.toCharArray() : null);
                        credentials.setPasswordPersistent(storePassword);
                        credentials.setIbRegistration(ibRegistration);

                        // Translate dnssecMode label to dnssecMode value
                        Picker spinnerDM = dialogContent.findComponentById(ResourceTable.Id_dnssecModeSpinner);
                        String[] dnssecModeValues = ctx.getStringArray(ResourceTable.Strarray_dnssec_Mode_value);
                        String selectedDnssecMode = dnssecModeValues[spinnerDM.getSelectorItemNum()];
                        credentials.setDnssecMode(selectedDnssecMode);

                        if (isShowServerOption) {
                            boolean isServerOverridden = ComponentUtil.isCompoundChecked(dialogContent, ResourceTable.Id_serverOverridden);
                            String serverAddress = ComponentUtil.getTextViewValue(dialogContent, ResourceTable.Id_serverIpField);
                            String serverPort = ComponentUtil.getTextViewValue(dialogContent, ResourceTable.Id_serverPortField);
                            if (serverAddress == null || serverPort == null) {
                                aTalkApp.showToastMessage(ResourceTable.String_certconfig_incomplete);
                                return false;
                            }

                            credentials.setIsServerOverridden(isServerOverridden);
                            credentials.setServerAddress(serverAddress);
                            credentials.setServerPort(serverPort);
                            credentials.setUserCancel(false);
                        }
                        synchronized (credentialsLock) {
                            credentialsLock.notify();
                        }
                        return true;
                    }

                    public void onDialogCancelled(DialogH dialog) {
                        credentials.setUserCancel(true);
                        synchronized (credentialsLock) {
                            credentialsLock.notify();
                        }
                        dialog.destroy();
                    }
                }, null);
        try {
            synchronized (credentialsLock) {
                // Wait for the credentials
                credentialsLock.wait();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return credentials;
    }

    /**
     * Get the number of messages belong to editedAccUid
     *
     * @param editedAccUid current edited account Uid
     * @param userName new userName
     *
     * @return count of old messages belong to editedAccUid which are to be purged or -1 if userName entered is invalid
     */
    private int checkPurgedMsgCount(String editedAccUid, String userName) {
        if (!TextUtils.isEmpty(userName) && userName.contains("@")) {
            if (!editedAccUid.split(":")[1].equals(userName)) {
                MessageHistoryServiceImpl mhs = MessageHistoryActivator.getMessageHistoryService();
                return mhs.getMessageCountForAccountUuid(editedAccUid);
            }
            else
                return 0;
        }
        else {
            return -1;
        }
    }

    /**
     * Sets the userNameEditable property, which should indicate to the implementations of
     * this interface if the user name could be changed by user or not.
     *
     * @param isUserNameEditable indicates if the user name could be changed by user in the
     * implementation of this interface.
     */
    public void setUserNameEditable(boolean isUserNameEditable) {
        this.isUserNameEditable = isUserNameEditable;
    }

    /**
     * Indicates if the user name is currently editable, i.e. could be changed by user or not.
     *
     * @return {@code true</code> if the user name could be changed, <code>false} - otherwise.
     */
    public boolean isUserNameEditable() {
        return isUserNameEditable;
    }
}
