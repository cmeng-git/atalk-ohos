/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.account;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Button;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Picker;
import ohos.agp.components.TextField;
import ohos.app.Context;
import ohos.utils.PacMap;

import net.java.sip.communicator.service.certificate.CertificateConfigEntry;
import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import org.apache.commons.lang3.StringUtils;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.plugin.certconfig.CertConfigActivator;
import org.atalk.ohos.util.ComponentUtil;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>AccountLoginFragment</code> is used for creating new account, but can be also used to obtain
 * user credentials. In order to do that parent <code>Ability</code> must implement {@link AccountLoginListener}.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AccountLoginSlice extends BaseSlice implements ListContainer.ItemSelectedListener {
    /**
     * The username property name.
     */
    public static final String ARG_USERNAME = "Username";

    /**
     * The password property name.
     */
    public static final String ARG_PASSWORD = "Password";

    /**
     * The password property name.
     */
    public static final String ARG_CLIENT_CERT = "ClientCert";

    /**
     * Contains all implementation specific properties that define the account.
     */
    protected Map<String, String> accountProperties = new HashMap<>();

    /**
     * The listener parent Ability that will be notified when user enters login, password,
     * server overridden option and server parameters etc
     */
    private AccountLoginListener loginListener;

    private TextField mPasswordField;
    private TextField mServerIpField;
    private TextField mServerPortField;

    private Checkbox mShowPasswordCheckBox;
    private Checkbox mSavePasswordCheckBox;
    private Checkbox mClientCertCheckBox;
    private Checkbox mServerOverrideCheckBox;
    private Checkbox mIBRegistrationCheckBox;

    private Picker spinnerNwk;
    private Picker spinnerDM;

    private Picker spinnerCert;
    private CertificateConfigEntry mCertEntry = null;

    /**
     * A map of <row, CertificateConfigEntry>
     */
    private Map<Integer, CertificateConfigEntry> mCertEntryList = new LinkedHashMap<>();

    private Context mContext;

    /**
     * Creates new <code>AccountLoginFragment</code> with optionally filled login and password fields.
     *
     * @param login optional login text that will be filled on the form.
     * @param password optional password text that will be filled on the form.
     *
     * @return new instance of parametrized <code>AccountLoginFragment</code>.
     */
    public static AccountLoginSlice createInstance(String login, String password) {
        AccountLoginSlice fragment = new AccountLoginSlice();

        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, login);
        args.putString(ARG_PASSWORD, password);

        return fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(Intent intent) {
        mContext = getContext();
        if (mContext instanceof AccountLoginListener) {
            this.loginListener = (AccountLoginListener) mContext;
        }
        else {
            throw new RuntimeException("Account login listener unspecified");
        }

        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        Component content = inflater.parse(ResourceTable.Layout_account_create_new, container, false);

        spinnerNwk = content.findComponentById(ResourceTable.Id_networkSpinner);
        ArrayAdapter<CharSequence> adapterNwk = ArrayAdapter.createFromResource(mContext,
                R.array.networks_array, ResourceTable.Layout_simple_spinner_item);
        adapterNwk.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);
        spinnerNwk.setItemProvider(adapterNwk);

        spinnerDM = content.findComponentById(ResourceTable.Id_dnssecModeSpinner);
        ArrayAdapter<CharSequence> adapterDM = ArrayAdapter.createFromResource(mContext,
                ResourceTable.Strarray_dnssec_Mode_name, ResourceTable.Layout_simple_spinner_item);
        adapterDM.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);
        spinnerDM.setItemProvider(adapterDM);

        mPasswordField = content.findComponentById(ResourceTable.Id_passwordField);
        mShowPasswordCheckBox = content.findComponentById(ResourceTable.Id_show_password);
        mSavePasswordCheckBox = content.findComponentById(ResourceTable.Id_store_password);
        mIBRegistrationCheckBox = content.findComponentById(ResourceTable.Id_ibRegistration);

        mClientCertCheckBox = content.findComponentById(ResourceTable.Id_clientCertEnable);
        spinnerCert = content.findComponentById(ResourceTable.Id_clientCertEntry);
        initCertList();

        mServerOverrideCheckBox = content.findComponentById(ResourceTable.Id_serverOverridden);
        mServerIpField = content.findComponentById(ResourceTable.Id_serverIpField);
        mServerPortField = content.findComponentById(ResourceTable.Id_serverPortField);

        // Hide ip and port fields on first create
        updateCertEntryViewVisibility(false);
        updateViewVisibility(false);
        initializeViewListeners();
        initButton(content);

        String username = intent.getStringParam(ARG_USERNAME);
        if (StringUtils.isNotEmpty(username)) {
            ComponentUtil.setTextViewValue(container, ResourceTable.Id_usernameField, username);
        }

        String password = intent.getStringParam(ARG_PASSWORD);
        if (StringUtils.isNotEmpty(password)) {
            ComponentUtil.setTextViewValue(content, ResourceTable.Id_passwordField, password);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInactive() {
        super.onInactive();
        loginListener = null;
    }

    /**
     * Certificate spinner list for selection
     */
    private void initCertList() {
        List<String> certList = new ArrayList<>();

        List<CertificateConfigEntry> certEntries = new ArrayList<>();
        CertificateService cvs = CertConfigActivator.getCertService();
        if (cvs != null) // NPE from field
            certEntries = cvs.getClientAuthCertificateConfigs();
        certEntries.add(0, CertificateConfigEntry.CERT_NONE);

        for (int idx = 0; idx < certEntries.size(); idx++) {
            CertificateConfigEntry entry = certEntries.get(idx);
            certList.add(entry.toString());
            mCertEntryList.put(idx, entry);
        }

        ArrayAdapter<String> certAdapter = new ArrayAdapter<>(mContext, ResourceTable.Layout_simple_spinner_item, certList);
        certAdapter.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);
        spinnerCert.setItemProvider(certAdapter);
        spinnerCert.setOnItemSelectedListener(this);
    }

    private void initializeViewListeners() {
        mShowPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked)
                -> ComponentUtil.showPassword(mPasswordField, isChecked));
        mClientCertCheckBox.setOnCheckedChangeListener((buttonView, isChecked)
                -> updateCertEntryViewVisibility(isChecked));
        mServerOverrideCheckBox.setOnCheckedChangeListener((buttonView, isChecked)
                -> updateViewVisibility(isChecked));
    }

    /**
     * Initializes the sign in button.
     */
    private void initButton(final Component content) {
        final Button signInButton = content.findComponentById(ResourceTable.Id_buttonSignIn);
        signInButton.setEnabled(true);

        signInButton.setClickedListener(v -> {
            // Translate network label to network value
            String[] networkValues = getStringArray(ResourceTable.Strarray_networks_array_values);
            String selectedNetwork = networkValues[spinnerNwk.getSelectorItemNum()];

            // Translate dnssecMode label to dnssecMode value
            String[] dnssecModeValues = getStringArray(ResourceTable.Strarray_dnssec_Mode_value);
            String selectedDnssecMode = dnssecModeValues[spinnerDM.getSelectorItemNum()];
            accountProperties.put(ProtocolProviderFactory.DNSSEC_MODE, selectedDnssecMode);

            // cmeng - must trim all leading and ending whitespace character entered
            // get included by android from auto correction checker
            String userName = ComponentUtil.toString(content.findComponentById(ResourceTable.Id_usernameField));
            String password = ComponentUtil.toString(mPasswordField);

            if (mClientCertCheckBox.isChecked() && (!CertificateConfigEntry.CERT_NONE.equals(mCertEntry))) {
                accountProperties.put(ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE, mCertEntry.toString());
            }
            else {
                accountProperties.put(ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE, CertificateConfigEntry.CERT_NONE.toString());
            }

            String serverAddress = ComponentUtil.toString(mServerIpField);
            String serverPort = ComponentUtil.toString(mServerPortField);

            String savePassword = Boolean.toString(mSavePasswordCheckBox.isChecked());
            accountProperties.put(ProtocolProviderFactory.PASSWORD_PERSISTENT, savePassword);

            String ibRegistration = Boolean.toString(mIBRegistrationCheckBox.isChecked());
            accountProperties.put(ProtocolProviderFactory.IBR_REGISTRATION, ibRegistration);

            // Update server override options
            if (mServerOverrideCheckBox.isChecked() && (serverAddress != null) && (serverPort != null)) {
                accountProperties.put(ProtocolProviderFactory.IS_SERVER_OVERRIDDEN, "true");
                accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS, serverAddress);
                accountProperties.put(ProtocolProviderFactory.SERVER_PORT, serverPort);
            }
            else {
                accountProperties.put(ProtocolProviderFactory.IS_SERVER_OVERRIDDEN, "false");
            }
            loginListener.onLoginPerformed(userName, password, selectedNetwork, accountProperties);
        });

        final Button cancelButton = content.findComponentById(ResourceTable.Id_buttonCancel);
        cancelButton.setClickedListener(v -> terminate());
    }

    private void updateCertEntryViewVisibility(boolean isEnabled) {
        if (isEnabled) {
            spinnerCert.setVisibility(Component.VISIBLE);
        }
        else {
            spinnerCert.setVisibility(Component.HIDE);
        }
    }

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
     * Stores the given <code>protocolProvider</code> data in the android system accounts.
     *
     * @param protocolProvider the <code>ProtocolProviderService</code>, corresponding to the account to store
     */
    private void storeAndroidAccount(ProtocolProviderService protocolProvider) {
        Map<String, String> accountProps = protocolProvider.getAccountID().getAccountProperties();

        String username = accountProps.get(ProtocolProviderFactory.USER_ID);
        Account account = new Account(username, getString(ResourceTable.String_aTalk_account_type));

        PacMap pacMap = new PacMap();
       for (String key : accountProps.keySet()) {
           pacMap.putString(key, accountProps.get(key));
        }

        AccountManager am = AccountManager.get(getContext());
        boolean accountCreated = am.addAccountExplicitly(account,
                accountProps.get(ProtocolProviderFactory.PASSWORD), pacMap);

        Bundle extras = getArguments();
        if (extras != null) {
            if (accountCreated) { // Pass the new account back to the account manager
                AccountAuthenticatorResponse response
                        = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

                Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(ResourceTable.String_aTalk_account_type));
                result.putAll(extraData);
                response.onResult(result);
            }
            // TODO: notify about account authentication
            // terminate();
        }
    }

    @Override
    public void onItemSelected(ListContainer listContainer, Component view, int pos, long id) {
        if (listContainer.getId() == ResourceTable.Id_clientCertEntry) {
            mCertEntry = mCertEntryList.get(pos);
        }
    }

    /**
     * The interface is used to notify listener when user click the sign-in button.
     */
    public interface AccountLoginListener {
        /**
         * Method is called when user click the sign in button.
         *
         * @param userName the login account entered by the user.
         * @param password the password entered by the user.
         * @param network the network name selected by the user.
         */
        void onLoginPerformed(String userName, String password, String network, Map<String, String> accountProperties);
    }
}
