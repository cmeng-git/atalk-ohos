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
package org.atalk.ohos.gui.account.settings;

import ohos.agp.components.*;
import ohos.agp.window.dialog.BaseDialog;
import ohos.agp.window.dialog.CommonDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.app.Context;

import net.java.sip.communicator.plugin.jabberaccregwizz.JabberAccountRegistrationActivator;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.ProtocolProviderActivator;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.jabber.JabberAccountRegistration;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.service.configuration.ConfigurationService;

/**
 * The Bosh-Proxy dialog is the one shown when the user clicks to set Bosh-Proxy preference in Account Settings...
 *
 * @author Eng Chong Meng
 */
public class BoshProxyDialog extends CommonDialog implements BaseDialog.RemoveCallback,
        ListContainer.ItemSelectedListener, Text.TextObserver {
    public final static String NONE = "NONE";
    public final static String BOSH = "BOSH";
    public final static String HTTP = "HTTP";

    private final static String SOCKS4 = "SOCKS4";
    private final static String SOCKS5 = "SOCKS5";

    private final Context mContext;
    private final JabberAccountRegistration jbrReg;
    private final String mAccountUuid;

    /**
     * The bosh proxy list view.
     */
    private ListContainer spinnerType;
    private Component boshUrlSetting;

    private Checkbox cbHttpProxy;
    private Text boshURL;

    private Text proxyHost;
    private Text proxyPort;

    private Text proxyUserName;
    private Text proxyPassword;

    private Button mApplyButton;

    /**
     * Flag indicating if there are uncommitted changes - need static to avoid clear by android OS
     */
    private boolean hasChanges;

    // Init to default selected so avoid false trigger on entry.
    private int mIndex = 0;

    /**
     * Constructs the <code>Bosh-Proxy Dialog</code>.
     *
     * @param context the Context
     * @param jbrReg the JabberAccountRegistration
     */
    public BoshProxyDialog(Context context, JabberAccountRegistration jbrReg) {
        super(context);
        mContext = context;

        this.jbrReg = jbrReg;
        String editedAccUID = jbrReg.getAccountUid();
        AccountManager accManager = ProtocolProviderActivator.getAccountManager();
        ProtocolProviderFactory factory = JabberAccountRegistrationActivator.getJabberProtocolProviderFactory();
        mAccountUuid = accManager.getStoredAccountUUID(factory, editedAccUID);

        initBoshProxy();
    }

    private void initBoshProxy() {
        setTitleText(mContext.getString(ResourceTable.String_settings_bosh_proxy));

        LayoutScatter layoutScatter = LayoutScatter.getInstance(mContext);
        Component boshProxy = layoutScatter.parse(ResourceTable.Layout_bosh_proxy_dialog, null, false);

        spinnerType = boshProxy.findComponentById(ResourceTable.Id_boshProxyType);

        boshUrlSetting = boshProxy.findComponentById(ResourceTable.Id_boshURL_setting);
        boshURL = boshProxy.findComponentById(ResourceTable.Id_boshURL);
        boshURL.addTextObserver(this);

        cbHttpProxy = boshProxy.findComponentById(ResourceTable.Id_cbHttpProxy);
        cbHttpProxy.setCheckedStateChangedListener((buttonView, isChecked) -> hasChanges = true);

        proxyHost = boshProxy.findComponentById(ResourceTable.Id_proxyHost);
        proxyHost.addTextObserver(this);
        proxyPort = boshProxy.findComponentById(ResourceTable.Id_proxyPort);
        proxyPort.addTextObserver(this);

        proxyUserName = boshProxy.findComponentById(ResourceTable.Id_proxyUsername);
        proxyUserName.addTextObserver(this);
        proxyPassword = boshProxy.findComponentById(ResourceTable.Id_proxyPassword);
        proxyPassword.addTextObserver(this);

        initBoshProxyContent();

        Checkbox showPassword = boshProxy.findComponentById(ResourceTable.Id_show_password);
        showPassword.setCheckedStateChangedListener((buttonView, isChecked)
                -> ComponentUtil.showPassword(proxyPassword, isChecked));

        mApplyButton = boshProxy.findComponentById(ResourceTable.Id_button_Apply);
        mApplyButton.setClickedListener(v -> {
            if (hasChanges) {
                if (saveBoshProxySettings())
                    destroy();
            }
        });

        Button cancelButton = boshProxy.findComponentById(ResourceTable.Id_button_Cancel);
        cancelButton.setClickedListener(v -> checkUnsavedChanges());

        setSwipeToDismiss(true);
        setAutoClosable(false);
        siteRemovable(true);
        hasChanges = false;
    }

    /**
     * initialize the Bosh-proxy dialog with the db stored values
     */
    private void initBoshProxyContent() {
        BaseItemProvider<CharSequence> adapterType = ArrayAdapter.createFromResource(mContext,
                ResourceTable.Strarray_bosh_proxy_type, ResourceTable.Layout_simple_spinner_item);
        adapterType.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);
        spinnerType.setItemProvider(adapterType);
        spinnerType.setItemSelectedListener(this);

        String type = jbrReg.getProxyType();
        if (!TextUtils.isEmpty(type)) {
            for (mIndex = 0; mIndex < spinnerType.getChildCount(); mIndex++) {
                if (spinnerType.getComponentAt(mIndex).equals(type)) {
                    spinnerType.setSelectedItemIndex(mIndex);
                    onItemSelected(spinnerType, spinnerType.getComponentAt(0), mIndex, spinnerType.getSelectedItemIndex());
                    break;
                }
            }
        }
        boshURL.setText(jbrReg.getBoshUrl());
        cbHttpProxy.setChecked(jbrReg.isBoshHttpProxyEnabled());

        proxyHost.setText(jbrReg.getProxyAddress());
        proxyPort.setText(jbrReg.getProxyPort());

        proxyUserName.setText(jbrReg.getProxyUserName());
        proxyPassword.setText(jbrReg.getProxyPassword());
    }

    @Override
    public void onItemSelected(ListContainer listContainer, Component component, int pos, long id) {

        Text type = (Text) listContainer.getComponentAt(pos);
        if (BOSH.equals(type.toString())) {
            boshUrlSetting.setVisibility(Component.VISIBLE);
        }
        else {
            boshUrlSetting.setVisibility(Component.HIDE);
        }

        if (mIndex != pos) {
            mIndex = pos;
            hasChanges = true;
        }
    }

    @Override
    public void onTextUpdated(String text, int start, int before, int count) {
        hasChanges = true;
    }

    /**
     * Save user entered Bosh-Proxy settings.
     */
    private boolean saveBoshProxySettings() {
        Object sType = spinnerType.getSelectedItem();
        String type = (sType == null) ? NONE : sType.toString();

        String boshUrl = ComponentUtil.toString(boshURL);
        String host = ComponentUtil.toString(proxyHost);
        String port = ComponentUtil.toString(proxyPort);
        String userName = ComponentUtil.toString(proxyUserName);
        String password = ComponentUtil.toString(proxyPassword);

        String accPrefix = mAccountUuid + ".";
        ConfigurationService configSrvc = ProtocolProviderActivator.getConfigurationService();
        configSrvc.setProperty(accPrefix + ProtocolProviderFactory.PROXY_TYPE, type);
        jbrReg.setProxyType(type);

        switch (type) {
            case BOSH:
                if (boshUrl == null) {
                    aTalkApp.showToastMessage(ResourceTable.String_bosh_proxy_url_null);
                    return false;
                }
                configSrvc.setProperty(accPrefix + ProtocolProviderFactory.BOSH_URL, boshUrl);
                jbrReg.setBoshUrl(boshUrl);

                boolean isHttpProxy = cbHttpProxy.isChecked();
                configSrvc.setProperty(accPrefix + ProtocolProviderFactory.BOSH_PROXY_HTTP_ENABLED, isHttpProxy);
                jbrReg.setBoshHttpProxyEnabled(isHttpProxy);

                // Continue with proxy settings checking if BOSH HTTP Proxy is enabled
                if (!isHttpProxy)
                    break;
            case HTTP:
            case SOCKS4:
            case SOCKS5:
                if ((host == null) || (port == null)) {
                    aTalkApp.showToastMessage(ResourceTable.String_bosh_proxy_host_port_null);
                    return false;
                }
                break;
            case NONE:
            default:
                break;
        }

        // value if null will remove the parameter from DB
        configSrvc.setProperty(accPrefix + ProtocolProviderFactory.PROXY_ADDRESS, host);
        jbrReg.setProxyAddress(host);

        configSrvc.setProperty(accPrefix + ProtocolProviderFactory.PROXY_PORT, port);
        jbrReg.setProxyPort(port);

        configSrvc.setProperty(accPrefix + ProtocolProviderFactory.PROXY_USERNAME, userName);
        jbrReg.setProxyUserName(userName);

        configSrvc.setProperty(accPrefix + ProtocolProviderFactory.PROXY_PASSWORD, password);
        jbrReg.setProxyPassword(password);

        // remove obsolete setting from DB - to be remove on later version (>2.0.4)
        configSrvc.setProperty(accPrefix + ProtocolProviderFactory.IS_USE_PROXY, null);

        AccountPreferenceSlice.setUncommittedChanges();
        return true;
    }

    @Override
    public void onRemove(IDialog iDialog) {
        if (hasChanges) {
            checkUnsavedChanges();
        }
    }

    /**
     * check for any unsaved changes and alert user
     */
    private void checkUnsavedChanges() {
        if (hasChanges) {
            DialogH.getInstance(mContext).showConfirmDialog(mContext,
                    ResourceTable.String_unsaved_changes_title,
                    ResourceTable.String_unsaved_changes,
                    ResourceTable.String_save, new DialogH.DialogListener() {
                        /**
                         * Fired when user clicks the dialog's confirm button.
                         *
                         * @param dialog source <code>DialogH</code>.
                         */
                        @Override
                        public boolean onConfirmClicked(DialogH dialog) {
                            return mApplyButton.simulateClick();
                        }

                        /**
                         * Fired when user dismisses the dialog.
                         *
                         * @param dialog source <code>DialogH</code>
                         */
                        @Override
                        public void onDialogCancelled(DialogH dialog) {
                            dialog.destroy();
                        }
                    });
        }
        else {
            destroy();
        }
    }
}

