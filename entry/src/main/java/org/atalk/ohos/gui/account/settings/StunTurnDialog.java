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

import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Picker;
import ohos.agp.components.TextField;
import ohos.app.Context;

import net.java.sip.communicator.service.protocol.StunServerDescriptor;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.util.ComponentUtil;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameException;
import inet.ipaddr.IPAddress;
import timber.log.Timber;

import static net.java.sip.communicator.service.protocol.jabber.JabberAccountID.DEFAULT_STUN_PORT;

/**
 * The dialog fragment that allows user to edit the STUN server descriptor.
 * Enhance TURN with TCP, TLS, DTLS transport
 *
 * @author Eng Chong Meng
 */
public class StunTurnDialog {
    private final Context mContext;

    /**
     * Parent adapter that will be notified about any changes to the descriptor
     */
    private final StunServerProvider mListener;

    /**
     * The edited descriptor
     */
    private StunServerDescriptor mDescriptor;

    /**
     * Creates new instance of {@link StunTurnDialog}
     *
     * @param listener the parent adapter
     * @param descriptor the descriptor to edit or <code>null</code> if new one shall be created
     */
    public StunTurnDialog(Context context, StunServerProvider listener, StunServerDescriptor descriptor) {
        if (listener == null)
            throw new NullPointerException();

        mContext = context;
        mDescriptor = descriptor;
        mListener = listener;
    }

    public DialogA create() {
        LayoutScatter inflater = LayoutScatter.getInstance(mContext);
        Component component = inflater.parse(ResourceTable.Layout_stun_turn_dialog, null, false);

        TextField ipAddress = component.findComponentById(ResourceTable.Id_ipAddress);
        TextField ipPort = component.findComponentById(ResourceTable.Id_serverPort);
        TextField turnUser = component.findComponentById(ResourceTable.Id_usernameField);
        TextField turnPassword = component.findComponentById(ResourceTable.Id_passwordField);

        Picker turnProtocolPicker = component.findComponentById(ResourceTable.Id_TURNProtocol);
        final String[] protocolArray = mContext.getStringArray(ResourceTable.Strarray_TURN_protocol);
        turnProtocolPicker.setDisplayedData(protocolArray);

        Component turnSetting = component.findComponentById(ResourceTable.Id_turnSetting);
        Checkbox useTurnCbox = component.findComponentById(ResourceTable.Id_useTurnCheckbox);
        useTurnCbox.setCheckedStateChangedListener((cButton, isChecked)
                -> turnSetting.setVisibility(isChecked ? Component.VISIBLE : Component.HIDE));

        Checkbox showPassword = component.findComponentById(ResourceTable.Id_show_password);
        showPassword.setCheckedStateChangedListener((buttonView, isChecked)
                -> ComponentUtil.showPassword(turnPassword, isChecked));

        if (mDescriptor != null) {
            ipAddress.setText(mDescriptor.getAddress());
            ipPort.setText(String.valueOf(mDescriptor.getPort()));

            useTurnCbox.setChecked(mDescriptor.isTurnSupported());
            turnUser.setText(new String(mDescriptor.getUsername()));
            turnPassword.setText(new String(mDescriptor.getPassword()));

            final String protocolText = convertTURNProtocolTypeToText(mDescriptor.getProtocol());
            for (int i = 0; i < protocolArray.length; i++) {
                if (protocolText.equals(protocolArray[i])) {
                    turnProtocolPicker.setValue(i);
                }
            }
        }
        else {
            ipPort.setText(DEFAULT_STUN_PORT);
        }

        turnSetting.setVisibility(useTurnCbox.isChecked() ? Component.VISIBLE : Component.HIDE);

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setComponent(component)
                .setTitle(ResourceTable.String_stun_turn_server)
                .setNegativeButton(ResourceTable.String_cancel, DialogA::remove)
                .setPositiveButton(ResourceTable.String_save, dialog -> {
                    if (saveChanges(component))
                        dialog.remove();
                });

        if (mDescriptor != null) {
            builder = builder.setNeutralButton(ResourceTable.String_remove, dialog -> {
                mListener.removeServer(mDescriptor);
                dialog.remove();
            });
        }
        return builder.create();
    }

    /**
     * Save the changes to the edited descriptor and notifies parent about the changes.
     * Returns <code>true</code> if all fields are correct.
     *
     * @return <code>true</code> if all field are correct and changes have been submitted to the parent adapter.
     */
    boolean saveChanges(Component dialog) {
        boolean useTurn = ((Checkbox) dialog.findComponentById(ResourceTable.Id_useTurnCheckbox)).isChecked();
        String ipAddress = ComponentUtil.toString(dialog.findComponentById(ResourceTable.Id_ipAddress));
        String portStr = ComponentUtil.toString(dialog.findComponentById(ResourceTable.Id_serverPort));

        String turnUser = ComponentUtil.toString(dialog.findComponentById(ResourceTable.Id_usernameField));
        String password = ComponentUtil.toString(dialog.findComponentById(ResourceTable.Id_passwordField));

        final Picker protocolPicker = dialog.findComponentById(ResourceTable.Id_TURNProtocol);
        final String protocol = convertTURNProtocolTextToType(protocolPicker.getDisplayedData()[protocolPicker.getValue()]);

        if ((ipAddress == null) || !isValidIpAddress(ipAddress) || (portStr == null)) {
            aTalkApp.showToastMessage(ResourceTable.String_invalid_address, ipAddress + ":" + portStr);
            return false;
        }
        int port = Integer.parseInt(portStr);

        // Create descriptor if new entry
        if (mDescriptor == null) {
            mDescriptor = new StunServerDescriptor(ipAddress, port, useTurn, turnUser, password, protocol);
            mListener.addServer(mDescriptor);
        }
        else {
            mDescriptor.setAddress(ipAddress);
            mDescriptor.setPort(port);
            mDescriptor.setTurnSupported(useTurn);
            mDescriptor.setUsername(turnUser);
            mDescriptor.setPassword(password);
            mDescriptor.setProtocol(protocol);
            mListener.updateServer(mDescriptor);
        }
        return true;
    }

    static boolean isValidIpAddress(String hostStr) {
        HostName host = new HostName(hostStr);
        try {
            // triggers exception for invalid
            host.validate();
            if (host.isAddress()) {
                IPAddress address = host.asAddress();
                Timber.d("%s address: %s", address.getIPVersion(), address);
            }
            else {
                Timber.d("Host name: %s", host);
            }
        } catch (HostNameException e) {
            return false;
        }
        return true;
    }

    private static String convertTURNProtocolTypeToText(final String type) {
        switch (type) {
            case StunServerDescriptor.PROTOCOL_UDP:
                return "UDP";
            case StunServerDescriptor.PROTOCOL_TCP:
                return "TCP";
            case StunServerDescriptor.PROTOCOL_DTLS:
                return "DTLS";
            case StunServerDescriptor.PROTOCOL_TLS:
                return "TLS";
            default:
                throw new IllegalArgumentException("unknown TURN protocol");
        }
    }

    private static String convertTURNProtocolTextToType(final String protocolText) {
        switch (protocolText) {
            case "UDP":
                return StunServerDescriptor.PROTOCOL_UDP;
            case "TCP":
                return StunServerDescriptor.PROTOCOL_TCP;
            case "DTLS":
                return StunServerDescriptor.PROTOCOL_DTLS;
            case "TLS":
                return StunServerDescriptor.PROTOCOL_TLS;
            default:
                throw new IllegalArgumentException("unknown TURN protocol");
        }
    }
}
