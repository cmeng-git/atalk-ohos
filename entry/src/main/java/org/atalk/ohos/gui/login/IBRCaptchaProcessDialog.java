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
package org.atalk.ohos.gui.login;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Button;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.components.TextField;
import ohos.agp.window.dialog.BaseDialog;
import ohos.app.Context;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.media.image.common.Size;

import net.java.sip.communicator.impl.protocol.jabber.JabberActivator;
import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusEnum;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.util.AppImageUtil;
import org.atalk.ohos.util.ComponentUtil;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StanzaError.Condition;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.bob.element.BoBDataExtension;
import org.jivesoftware.smackx.captcha.packet.CaptchaExtension;
import org.jivesoftware.smackx.iqregisterx.AccountManager;
import org.jivesoftware.smackx.iqregisterx.packet.Registration;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.FormField.Type;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.util.XmppStringUtils;

import timber.log.Timber;

/**
 * The dialog pops up when the user account login return with "not-authorized" i.e. not
 * registered on server, and user has select the InBand Registration option.
 * The IBRegistration supports Form submission with optional captcha challenge,
 * and the bare attributes format method
 *
 * @author Eng Chong Meng
 */
public class IBRCaptchaProcessDialog extends BaseDialog {
    /**
     * Listens for connection closes or errors.
     */
    private JabberConnectionListener connectionListener;

    private final ProtocolProviderServiceJabberImpl mPPS;
    private XMPPConnection mConnection;

    // Map contains extra form field label and variable not in static layout
    private final Map<String, String> varMap = new HashMap<>();

    // The layout container to add the extra form fields
    private DirectionalLayout entryFields;

    private TextField mCaptchaText;
    private TextField mPasswordField;
    private Checkbox mServerOverrideCheckBox;
    private TextField mServerIpField;
    private TextField mServerPortField;
    private Text mReason;

    private Image mImageView;
    private Checkbox mShowPasswordCheckBox;

    private Button mSubmitButton;
    private Button mCancelButton;
    private Button mOKButton;

    private final AccountID mAccountId;
    private PixelMap mCaptcha;
    private final Context mContext;
    private DataForm mDataForm;
    private DataForm.Builder formBuilder;
    private final String mPassword;
    private String mReasonText;

    /**
     * Constructor for the <code>Captcha Request Dialog</code> for passing the dialog parameters
     *
     * @param context the context to which the dialog belongs
     * @param pps the protocol provider service that offers the service
     * @param accountId the AccountID of the login user request for IBRegistration
     */
    public IBRCaptchaProcessDialog(Context context, ProtocolProviderServiceJabberImpl pps, AccountID accountId, String pwd) {
        super(context);
        mContext = context;
        mPPS = pps;
        mConnection = pps.getConnection();
        mAccountId = accountId;
        mPassword = pwd;
        mReasonText = aTalkApp.getResString(ResourceTable.String_captcha_ibr_reason);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(Intent intent) {
        this.setUIContent(ResourceTable.Layout_ibr_captcha);
        setTitle(mContext.getString(ResourceTable.String_captcha_ibr_request));

        TextField mUserNameField = this.findComponentById(ResourceTable.Id_username);
        mUserNameField.setText(mAccountId.getUserID());
        mUserNameField.setEnabled(false);

        mPasswordField = this.findComponentById(ResourceTable.Id_password);
        mShowPasswordCheckBox = this.findComponentById(ResourceTable.Id_show_password);
        mServerOverrideCheckBox = this.findComponentById(ResourceTable.Id_serverOverridden);
        mServerIpField = this.findComponentById(ResourceTable.Id_serverIpField);
        mServerPortField = this.findComponentById(ResourceTable.Id_serverPortField);
        mImageView = this.findComponentById(ResourceTable.Id_captcha);
        mCaptchaText = this.findComponentById(ResourceTable.Id_input);
        mReason = this.findComponentById(ResourceTable.Id_reason_field);

        mSubmitButton = this.findComponentById(ResourceTable.Id_button_Submit);
        mSubmitButton.setVisibility(Component.VISIBLE);
        mOKButton = this.findComponentById(ResourceTable.Id_button_OK);
        mOKButton.setVisibility(Component.HIDE);
        mCancelButton = this.findComponentById(ResourceTable.Id_button_Cancel);

        if (connectionListener == null) {
            connectionListener = new JabberConnectionListener();
            mConnection.addConnectionListener(connectionListener);
        }

        // Prevents from closing the dialog on outside touch or Back Key
        setCanceledOnTouchOutside(false);
        setCancelable(false);

        UpdateDialogContent();
        if (initIBRRegistration()) {
            mReason.setText(mReasonText);
            updateEntryFields();
            showCaptchaContent();
            initializeViewListeners();
        }
        // unable to start IBR registration on server
        else {
            onIBRServerFailure();
        }
    }

    /*
     * Update IBRegistration dialog content with the initial user supplied information.
     */
    private void UpdateDialogContent() {
        mPasswordField.setText(mPassword);
        boolean isServerOverridden = mAccountId.isServerOverridden();
        mServerOverrideCheckBox.setChecked(isServerOverridden);

        mServerIpField.setText(mAccountId.getServerAddress());
        mServerPortField.setText(mAccountId.getServerPort());
        updateViewVisibility(isServerOverridden);
    }

    /**
     * Start the InBand Registration for the accountId on the defined XMPP connection by pps.
     * Registration can either be:
     * - simple username and password or
     * - Form With captcha protection with embedded captcha image if available, else the
     * image is retrieved from the given url in the form.
     *
     * @return <code>true</code> if IBRegistration is supported and info is available
     */
    private boolean initIBRRegistration() {
        // NetworkOnMainThreadException if attempt to reconnect in UI thread; so return if no connection, else deadlock.
        if (!mConnection.isConnected())
            return false;

        try {
            // Check and proceed only if IBRegistration is supported by the server
            AccountManager accountManager = AccountManager.getInstance(mConnection);
            if (accountManager.isSupported()) {
                Registration info = accountManager.getRegistrationInfo();
                if (info != null) {
                    // do not proceed if dataForm is null
                    DataForm dataForm = info.getDataForm();
                    if (dataForm == null)
                        return false;

                    mDataForm = dataForm;
                    BoBDataExtension bob = info.getBoB();
                    if (bob != null) {
                        byte[] bytData = bob.getBobData().getContent();
                        mCaptcha = AppImageUtil.pixelMapFromBytes(bytData);
                    }
                    // Get the captcha image from the url link if bob is not available
                    else {
                        FormField urlField = dataForm.getField("url");
                        if (urlField != null) {
                            String urlString = urlField.getFirstValue();
                            getCaptcha(urlString);
                        }
                    }
                }
                // Not user Form, so setup to use plain attributes login method instead.
                else {
                    mDataForm = null;
                    mCaptcha = null;
                }
                return true;
            }
        } catch (InterruptedException | XMPPException | SmackException e) {
            String errMsg = e.getMessage();
            StanzaError xmppError = StanzaError.from(Condition.not_authorized, errMsg).build();
            mPPS.accountIBRegistered.reportFailure(new XMPPException.XMPPErrorException(null, xmppError));
            showResult();
        }
        return false;
    }

    /**
     * Add extra Form fields if there are not in the static layout
     */
    private void updateEntryFields() {
        entryFields = findComponentById(ResourceTable.Id_entry_fields);
        LayoutScatter layoutScatter = LayoutScatter.getInstance(mContext);

        if (mDataForm != null) {
            List<FormField> formFields = mDataForm.getFields();
            for (FormField formField : formFields) {
                Type type = formField.getType();
                String var = formField.getFieldName();

                if ((type == Type.hidden) || (type == Type.fixed))
                    continue;

                String label = formField.getLabel();
                String value = formField.getFirstValue();

                if (var.equals("url")) {
                    ((Text) findComponentById(ResourceTable.Id_url_label)).setText(label);
                    Text url_link = findComponentById(ResourceTable.Id_url_link);
                    url_link.setText(value);
                    url_link.setClickedListener(v -> {
                        getCaptcha(value);
                    });
                }
                else {
                    if (var.equals(CaptchaExtension.USER_NAME) || var.equals(CaptchaExtension.PASSWORD) || var.equals(CaptchaExtension.OCR))
                        continue;

                    DirectionalLayout fieldEntry = (DirectionalLayout) layoutScatter.parse(ResourceTable.Layout_ibr_field_entry_row, null, false);
                    Text viewLabel = fieldEntry.findComponentById(ResourceTable.Id_field_label);
                    Image viewRequired = fieldEntry.findComponentById(ResourceTable.Id_star);

                    Timber.w("New entry field: %s = %s", label, var);
                    // Keep copy of the variable field for later extracting the user entered value
                    varMap.put(label, var);

                    viewLabel.setText(label);
                    viewRequired.setVisibility(formField.isRequired() ? Component.VISIBLE : Component.INVISIBLE);
                    entryFields.addComponent(fieldEntry);
                }
            }
        }
    }

    /*
     * Update dialog content with the received captcha information for form presentation.
     */
    private void showCaptchaContent() {
        BaseAbility.runOnUiThread(() -> {
            if (mCaptcha != null) {
                findComponentById(ResourceTable.Id_captcha_container).setVisibility(Component.VISIBLE);
                // Scale the captcha to the display resolution
                DisplayMetrics metrics = mContext.getResourceManager().getDisplayMetrics();
                Size size = mCaptcha.getImageInfo().size;
                PixelMap captcha = AppImageUtil.scaledPixelMap(mCaptcha,
                        (int) (size.width * metrics.scaledDensity),
                        (int) (size.height * metrics.scaledDensity));
                mImageView.setPixelMap(captcha);
                mCaptchaText.setHint(ResourceTable.String_captcha_ibr_hint);
                mCaptchaText.requestFocus();
            }
            else {
                findComponentById(ResourceTable.Id_captcha_container).setVisibility(Component.HIDE);
            }
        });
    }

    /**
     * Fetch the captcha bitmap from the given url link on new thread
     *
     * @param urlString Url link to fetch the captcha
     */
    private void getCaptcha(String urlString) {
        new Thread(() -> {
            try {
                if (!TextUtils.isEmpty(urlString)) {
                    URL url = new URL(urlString);
                    InputStream is = url.openConnection().getInputStream();
                    ImageSource.SourceOptions srcOptions = new ImageSource.SourceOptions();
                    ImageSource imageSrc = ImageSource.create(is, srcOptions);
                    mCaptcha = imageSrc.createPixelmap(null);
                    showCaptchaContent();
                }
            } catch (IOException e) {
                Timber.e(e, "%s", e.getMessage());
            }
        }).start();
    }

    /**
     * Setup all the dialog buttons' listeners for the required actions on user click
     */
    private void initializeViewListeners() {
        mShowPasswordCheckBox.setCheckedStateChangedListener((buttonView, isChecked)
                -> ComponentUtil.showPassword(mPasswordField, isChecked));
        mServerOverrideCheckBox.setCheckedStateChangedListener((buttonView, isChecked)
                -> updateViewVisibility(isChecked));
        mImageView.setClickedListener(v -> mCaptchaText.requestFocus());

        mSubmitButton.setClickedListener(v -> {
            // server disconnect user if waited for too long
            if ((mConnection != null) && mConnection.isConnected()) {
                if (updateAccount()) {
                    onSubmitClicked();
                    showResult();
                }
            }
        });

        // Re-trigger IBR if user click OK - let login takes over
        mOKButton.setClickedListener(v -> {
            closeDialog();
            GlobalStatusService globalStatusService = AppGUIActivator.getGlobalStatusService();
            globalStatusService.publishStatus(GlobalStatusEnum.ONLINE);
        });

        // Set IBR to false on user cancel. Otherwise may loop in IBR if server returns error
        mCancelButton.setClickedListener(v -> {
            mAccountId.setIbRegistration(false);
            String errMsg = "InBand registration cancelled by user!";
            StanzaError xmppError = StanzaError.from(Condition.registration_required, errMsg).build();
            mPPS.accountIBRegistered.reportFailure(new XMPPException.XMPPErrorException(null, xmppError));
            closeDialog();
        });
    }

    /**
     * Updated AccountID with the parameters entered by user
     */
    private boolean updateAccount() {
        String password;
        String pwd = mPasswordField.getText();
        if ((pwd != null) && StringUtils.isNotEmpty(password = pwd)) {
            mAccountId.setPassword(password);
            if (mAccountId.isPasswordPersistent())
                JabberActivator.getProtocolProviderFactory().storePassword(mAccountId, password);
        }
        else {
            mReason.setText(ResourceTable.String_captcha_ibr_pwd_empty);
            return false;
        }

        // Update server override options
        String serverAddress = ComponentUtil.toString(mServerIpField);
        String serverPort = ComponentUtil.toString(mServerPortField);

        boolean isServerOverride = mServerOverrideCheckBox.isChecked();
        mAccountId.setServerOverridden(isServerOverride);
        if ((isServerOverride) && (serverAddress != null) && (serverPort != null)) {
            mAccountId.setServerAddress(serverAddress);
            mAccountId.setServerPort(serverPort);
        }
        return true;
    }

    /**
     * Handles the <code>ActionEvent</code> triggered when one user clicks on the Submit button.
     */
    private void onSubmitClicked() {
        // Server will end connection on wait timeout due to user no response
        if ((mConnection != null) && mConnection.isConnected()) {
            AccountManager accountManager = AccountManager.getInstance(mConnection);

            // Only localPart is required
            String userName = XmppStringUtils.parseLocalpart(mAccountId.getUserID());
            String pwd = mPasswordField.getText();

            try {
                if (mDataForm != null) {
                    formBuilder = DataForm.builder(DataForm.Type.submit);

                    addFormField(CaptchaExtension.USER_NAME, userName);
                    if (pwd != null) {
                        addFormField(CaptchaExtension.PASSWORD, pwd);
                    }

                    // Add an extra field if any and its value is not empty
                    int varCount = entryFields.getChildCount();
                    for (int i = 0; i < varCount; i++) {
                        final Component row = entryFields.getChildAt(i);
                        String label = ComponentUtil.toString(row.findComponentById(ResourceTable.Id_field_label));
                        if (varMap.containsKey(label)) {
                            String data = ComponentUtil.toString(row.findComponentById(ResourceTable.Id_field_value));
                            if (data != null)
                                addFormField(varMap.get(label), data);
                        }
                    }

                    // set captcha challenge required info
                    if (mCaptcha != null) {
                        addFormField(FormField.FORM_TYPE, CaptchaExtension.NAMESPACE);

                        formBuilder.addField(mDataForm.getField(CaptchaExtension.CHALLENGE));
                        formBuilder.addField(mDataForm.getField(CaptchaExtension.SID));

                        addFormField(CaptchaExtension.ANSWER, "3");
                        String rc = mCaptchaText.getText();
                        if (rc != null) {
                            addFormField(CaptchaExtension.OCR, rc);
                        }
                    }
                    accountManager.createAccount(formBuilder.build());
                }
                else {
                    Localpart username = Localpart.formUnescapedOrNull(userName);
                    accountManager.sensitiveOperationOverInsecureConnection(false);
                    if (pwd != null) {
                        accountManager.createAccount(username, pwd);
                    }
                }

                // if not exception being thrown, then registration is successful. Clear IBR flag on success
                mAccountId.setIbRegistration(false);
                mPPS.accountIBRegistered.reportSuccess();
            } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                     | SmackException.NotConnectedException | InterruptedException ex) {
                StanzaError xmppError;
                String errMsg = ex.getMessage();
                String errDetails = "";
                if (ex instanceof XMPPException.XMPPErrorException) {
                    xmppError = ((XMPPException.XMPPErrorException) ex).getStanzaError();
                    errDetails = xmppError.getDescriptiveText();
                }
                else {
                    xmppError = StanzaError.from(Condition.not_acceptable, errMsg).build();
                }
                Timber.e("Exception: %s; %s", errMsg, errDetails);
                if ((errMsg != null) && errMsg.contains("conflict") && (errDetails != null) && errDetails.contains("exists"))
                    mAccountId.setIbRegistration(false);
                mPPS.accountIBRegistered.reportFailure(new XMPPException.XMPPErrorException(null, xmppError));
            }
        }
    }

    /**
     * Add field / value to formBuilder for registration
     *
     * @param name the FormField variable
     * @param value the FormField value
     */
    private void addFormField(String name, String value) {
        TextSingleFormField.Builder field = FormField.builder(name);
        field.setValue(value);
        formBuilder.addField(field.build());
    }

    private void closeDialog() {
        if (connectionListener != null) {
            mConnection.removeConnectionListener(connectionListener);
            connectionListener = null;
        }
        mConnection = null;
        this.cancel();
    }

    /**
     * Show or hide server address & port
     *
     * @param IsServerOverridden <code>true</code> show server address and port field for user entry
     */
    private void updateViewVisibility(boolean IsServerOverridden) {
        if (IsServerOverridden) {
            mServerIpField.setVisibility(Component.VISIBLE);
            mServerPortField.setVisibility(Component.VISIBLE);
        }
        else {
            mServerIpField.setVisibility(Component.HIDE);
            mServerPortField.setVisibility(Component.HIDE);
        }
    }

    /**
     * Shows IBR registration result.
     */
    private void showResult() {
        String errMsg = null;
        mReasonText = mContext.getString(ResourceTable.String_captcha_ibr_success);
        try {
            XMPPException ex = mPPS.accountIBRegistered.checkIfSuccessOrWait();
            if (ex != null) {
                errMsg = ex.getMessage();
                if (ex instanceof XMPPException.XMPPErrorException) {
                    String errDetails = ((XMPPException.XMPPErrorException) ex).getStanzaError().getDescriptiveText();
                    if (!StringUtils.isEmpty(errDetails))
                        errMsg += "\n" + errDetails;
                }
            }
        } catch (SmackException.NoResponseException | InterruptedException e) {
            errMsg = e.getMessage();
        }
        if (StringUtils.isNotEmpty(errMsg)) {
            mReasonText = mContext.getString(ResourceTable.String_captcha_ibr_failed, errMsg);
        }
        // close connection on error, else throws connectionClosedOnError on timeout
        Async.go(() -> {
            if (mConnection.isConnected())
                ((AbstractXMPPConnection) mConnection).disconnect();
        });

        mReason.setText(mReasonText);
        mSubmitButton.setVisibility(Component.HIDE);
        mOKButton.setVisibility(Component.VISIBLE);
        mCaptchaText.setHint(ResourceTable.String_captcha_ibr_retry);
        mCaptchaText.setEnabled(false);
    }

    // Server failure with start of IBR registration
    private void onIBRServerFailure() {
        mReasonText = "InBand registration - Server Error!";
        mImageView.setVisibility(Component.HIDE);
        mReason.setText(mReasonText);
        mPasswordField.setEnabled(false);
        mCaptchaText.setVisibility(Component.HIDE);
        mSubmitButton.setEnabled(false);
        mSubmitButton.setAlpha(0.5f);
        mOKButton.setEnabled(false);
        mOKButton.setAlpha(0.5f);
        initializeViewListeners();
    }

    /**
     * Listener for jabber connection events
     */
    private class JabberConnectionListener implements ConnectionListener {
        /**
         * Notification that the connection was closed normally.
         */
        public void connectionClosed() {
        }

        /**
         * Notification that the connection was closed due to an exception. When abruptly disconnected.
         * Note: ReconnectionManager was not enabled otherwise it will try to reconnecting to the server.
         * Any update of the view must be on UiThread
         *
         * @param exception contains information of the error.
         */
        public void connectionClosedOnError(Exception exception) {
            String errMsg = exception.getMessage();
            Timber.e("Captcha-Exception: %s", errMsg);

            StanzaError xmppError = StanzaError.from(Condition.remote_server_timeout, errMsg).build();
            mPPS.accountIBRegistered.reportFailure(new XMPPException.XMPPErrorException(null, xmppError));
            BaseAbility.runOnUiThread(IBRCaptchaProcessDialog.this::showResult);
        }

        @Override
        public void connected(XMPPConnection connection) {
        }

        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
        }
    }
}
