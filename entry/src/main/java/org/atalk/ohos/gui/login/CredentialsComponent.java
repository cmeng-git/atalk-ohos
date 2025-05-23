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

import java.util.Arrays;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.agp.components.TextField;
import ohos.app.Context;
import ohos.utils.PacMap;

import net.java.sip.communicator.service.certificate.CertificateConfigEntry;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.util.ComponentUtil;

/**
 * The credentials fragment can be used to retrieve username, password, the "store password" option status, login
 * server overridden option and the server ip:port. Use the arguments to fill the fragment with default values.
 * Supported arguments are:
 * - {@link #ARG_LOGIN} login default text value; editable only if new user creation
 * - {@link #ARG_LOGIN_EDITABLE} <code>boolean</code> flag indicating if the login field is editable
 * - {@link #ARG_PASSWORD} password default text value
 * - {@link #ARG_IB_REGISTRATION} "store password" default <code>boolean</code> value
 * - {@link #ARG_IB_REGISTRATION} "ibr_registration" default <code>boolean</code> value
 * - {@link #ARG_STORE_PASSWORD} "store password" default <code>boolean</code> value
 * - {@link #ARG_IS_SERVER_OVERRIDDEN} "Server Overridden" default <code>boolean</code> value
 * - {@link #ARG_SERVER_ADDRESS} Server address default text value
 * - {@link #ARG_SERVER_PORT} Server port default text value
 * - {@link #ARG_LOGIN_REASON} login in reason, present last server return exception if any
 *
 * @author Eng Chong Meng
 */
public class CredentialsComponent extends Component {
    /**
     * Pre-entered login argument.
     */
    public static final String ARG_LOGIN = "login";

    /**
     * Pre-entered password argument.
     */
    public static final String ARG_PASSWORD = "password";

    /**
     * Pre-entered dnssecMode argument.
     */
    public static final String ARG_DNSSEC_MODE = "dnssec_mode";

    /**
     * Argument indicating whether the login can be edited.
     */
    public static final String ARG_LOGIN_EDITABLE = "login_editable";

    /**
     * Pre-entered "store password" <code>boolean</code> value.
     */
    public static final String ARG_STORE_PASSWORD = "store_pass";

    /**
     * Pre-entered "store password" <code>boolean</code> value.
     */
    public static final String ARG_IB_REGISTRATION = "ib_registration";

    /**
     * Show server option for user entry if true " <code>boolean</code> value.
     */
    public static final String ARG_IS_SHOWN_SERVER_OPTION = "is_shown_server_option";

    /**
     * Pre-entered "is server overridden" <code>boolean</code> value.
     */
    public static final String ARG_IS_SERVER_OVERRIDDEN = "is_server_overridden";

    /**
     * Pre-entered "store server address".
     */
    public static final String ARG_SERVER_ADDRESS = "server_address";

    /**
     * Pre-entered "store server port".
     */
    public static final String ARG_SERVER_PORT = "server_port";

    /**
     * Reason for the login / reLogin.
     */
    public static final String ARG_LOGIN_REASON = "login_reason";

    public static final String ARG_CERT_ID = "cert_id";

    private final Checkbox mServerOverrideCheckBox;
    private final TextField mServerIpField;
    private final TextField mServerPortField;

    private final TextField mPasswordField;
    private final Checkbox mShowPasswordCheckBox;

    public CredentialsComponent(Context ctx, PacMap pacMap) {
        super(ctx);

        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        Component content = inflater.parse(ResourceTable.Layout_account_credentials, null, false);

        ListContainer spinnerDM = content.findComponentById(ResourceTable.Id_dnssecModeSpinner);
        BaseItemProvider adapterDM = ArrayAdapter.createFromResource(getContext(),
                ResourceTable.Strarray_dnssec_Mode_name, ResourceTable.Layout_simple_spinner_item);
        adapterDM.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);
        spinnerDM.setItemProvider(adapterDM);

        String dnssecMode = pacMap.getString(ARG_DNSSEC_MODE);
        String[] dnssecModeValues = ctx.getStringArray(ResourceTable.Strarray_dnssec_Mode_value);
        int sPos = Arrays.asList(dnssecModeValues).indexOf(dnssecMode);
        spinnerDM.setSelectedItemIndex(sPos);

        TextField mUserNameEdit = content.findComponentById(ResourceTable.Id_username);
        mUserNameEdit.setText(pacMap.getString(ARG_LOGIN));
        mUserNameEdit.setEnabled(pacMap.getBooleanValue(ARG_LOGIN_EDITABLE, true));

        mShowPasswordCheckBox = content.findComponentById(ResourceTable.Id_show_password);

        mPasswordField = content.findComponentById(ResourceTable.Id_password);
        mPasswordField.setText(pacMap.getString(ARG_PASSWORD));
        // ComponentUtil.setTextViewValue(content, ResourceTable.Id_password, args.getString(ARG_PASSWORD));

        ComponentUtil.setCompoundChecked(content, ResourceTable.Id_store_password, pacMap.getBooleanValue(ARG_STORE_PASSWORD, true));
        ComponentUtil.setCompoundChecked(content, ResourceTable.Id_ib_registration, pacMap.getBooleanValue(ARG_IB_REGISTRATION, false));

        Image showCert = content.findComponentById(ResourceTable.Id_showCert);
        String clientCertId = pacMap.getString(ARG_CERT_ID);
        if ((clientCertId == null) || clientCertId.equals(CertificateConfigEntry.CERT_NONE.toString())) {
            showCert.setVisibility(Component.HIDE);
        }

        mServerOverrideCheckBox = content.findComponentById(ResourceTable.Id_serverOverridden);
        mServerIpField = content.findComponentById(ResourceTable.Id_serverIpField);
        mServerPortField = content.findComponentById(ResourceTable.Id_serverPortField);

        boolean isShownServerOption = pacMap.getBooleanValue(ARG_IS_SHOWN_SERVER_OPTION, false);
        if (isShownServerOption) {
            boolean isServerOverridden = pacMap.getBooleanValue(ARG_IS_SERVER_OVERRIDDEN, false);
            ComponentUtil.setCompoundChecked(content, ResourceTable.Id_serverOverridden, isServerOverridden);
            mServerIpField.setText(pacMap.getString(ARG_SERVER_ADDRESS));
            mServerPortField.setText(pacMap.getString(ARG_SERVER_PORT));
            updateViewVisibility(isServerOverridden);
        }
        else {
            mServerIpField.setVisibility(Component.HIDE);
            mServerPortField.setVisibility(Component.HIDE);
        }

        // make xml text more human readable and link clickable
        Text reasonField = content.findComponentById(ResourceTable.Id_reason_field);
        String xmlText = pacMap.getString(ARG_LOGIN_REASON);
        if (!TextUtils.isEmpty(xmlText)) {
            String loginReason = Html.fromHtml(xmlText.replace("\n", "<br/>"));
            reasonField.setText(loginReason);
        }

        initializeViewListeners();
    }

    private void initializeViewListeners() {
        mShowPasswordCheckBox.setCheckedStateChangedListener((buttonView, isChecked)
                -> ComponentUtil.showPassword(mPasswordField, isChecked));

        mServerOverrideCheckBox.setCheckedStateChangedListener(
                (buttonView, isChecked) -> updateViewVisibility(isChecked));
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
}
