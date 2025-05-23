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
package org.atalk.impl.appcertdialog;

import ohos.aafwk.content.Intent;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import net.java.sip.communicator.impl.certificate.CertificateServiceImpl;
import net.java.sip.communicator.plugin.jabberaccregwizz.JabberAccountRegistrationActivator;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSetConnectionInfo;
import net.java.sip.communicator.service.protocol.OperationSetTLS;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.TransportProtocol;
import net.java.sip.communicator.util.account.AccountUtils;

import org.apache.commons.lang3.StringUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.dialogs.DialogA;

/**
 * Setting screen which displays protocolProvider connection info and servers SSL Certificates.
 * Unregistered accounts without any approved certificates are not shown.
 * a. Short click to display the SSL certificate for registered account.
 * b. Long Click to delete any manually approved self signed SSL certificates if any.
 *
 * @author Eng Chong Meng
 */
public class ConnectionInfo extends BaseAbility {
    /**
     * List of AccountId to its array of manual approved self signed certificates
     */
    private final Map<AccountID, List<String>> certificateEntry = new Hashtable<>();

    /*
     * Adapter used to display connection info and SSL certificates for all protocolProviders.
     */
    private ConnectionInfoProvider mCIProvider;

    private CertificateServiceImpl cvs;

    /*
     * X509 SSL Certificate view on dialog window
     */
    private X509CertificateView viewCertDialog;

    private DialogA deleteDialog;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_list_layout);
        ListContainer providerKeysList = findComponentById(ResourceTable.Id_list);

        cvs = (CertificateServiceImpl) JabberAccountRegistrationActivator.getCertificateService();
        List<AccountID> accountIDS = initCertificateEntry();

        this.mCIProvider = new ConnectionInfoProvider(accountIDS);
        providerKeysList.setItemProvider(mCIProvider);

        providerKeysList.setItemClickedListener((parent, view, position, id)
                -> showSslCertificate(position));

        providerKeysList.setItemLongClickedListener((parent, view, position, id) -> {
            showSslCertificateDeleteAlert(position);
            return true;
        });
    }

    /*
     * Dismissed any opened dialog to avoid window leaks on rotation
     */
    @Override
    protected void onInactive() {
        super.onInactive();
        if (viewCertDialog != null && viewCertDialog.isShowing()) {
            viewCertDialog.destroy();
            viewCertDialog = null;
        }
        if (deleteDialog != null && deleteDialog.isShowing()) {
            deleteDialog.remove();
            deleteDialog = null;
        }
    }

    /**
     * Init and populate AccountIDs with all registered accounts or
     * account has manual approved self-signed certificate.
     *
     * @return a list of all accountIDs for display list
     */
    private List<AccountID> initCertificateEntry() {
        certificateEntry.clear();
        final List<String> certEntries = cvs.getAllServerAuthCertificates();

        // List of the accounts for display
        final List<AccountID> accountIDS = new ArrayList<>();

        // List of all local stored accounts
        Collection<AccountID> userAccounts = AccountUtils.getStoredAccounts();

        /*
         * Iterate all the local stored accounts; add to display list if there are associated user approved
         * certificates, or the account is registered for SSL certificate display.
         */
        for (AccountID accountId : userAccounts) {
            ProtocolProviderService pps = accountId.getProtocolProvider();
            String serviceName = accountId.getService();
            List<String> sslCerts = new ArrayList<>();
            for (String certEntry : certEntries) {
                if (certEntry.contains(serviceName)) {
                    sslCerts.add(certEntry);
                }
            }

            if ((!sslCerts.isEmpty()) || ((pps != null) && pps.isRegistered())) {
                accountIDS.add(accountId);
                certificateEntry.put(accountId, sslCerts);

                // remove any assigned certs from certEntries
                for (String cert : sslCerts) {
                    certEntries.remove(cert);
                }
            }
        }
        return accountIDS;
    }

    /**
     * Displays SSL Certificate information.
     * Invoked when user short clicks a link in the editor pane.
     *
     * @param position the position of <code>SSL Certificate</code> in adapter's list which will be displayed.
     */
    public void showSslCertificate(int position) {
        AccountID accountId = mCIProvider.getItem(position);
        ProtocolProviderService pps = accountId.getProtocolProvider();
        if ((pps != null) && pps.isRegistered()) {
            OperationSetTLS opSetTLS = pps.getOperationSet(OperationSetTLS.class);
            Certificate[] chain = opSetTLS.getServerCertificates();

            if (chain != null) {
                viewCertDialog = new X509CertificateView(this, chain);
                viewCertDialog.show();
            }
            else
                aTalkApp.showToastMessage(aTalkApp.getResString(ResourceTable.String_callinfo_tls_certificate_content) + ": null!");
        }
        else {
            aTalkApp.showToastMessage(ResourceTable.String_certconfig_show_cert_exception, accountId);
        }
    }

    /**
     * Displays alert asking user if he wants to delete the selected SSL Certificate. (Long click)
     * Delete both the serviceName certificate and the _xmpp-client.serviceName
     *
     * @param position the position of <code>SSL Certificate</code> in adapter's list which has to be used in the alert.
     */
    private void showSslCertificateDeleteAlert(int position) {
        AccountID accountId = mCIProvider.getItem(position);
        List<String> certs = certificateEntry.get(accountId);
        // Just display the SSL certificate info if none to delete
        if (certs.isEmpty()) {
            showSslCertificate(position);
            return;
        }

        final String bareJid = accountId.getAccountJid();
        DialogA.Builder builder = new DialogA.Builder(this);
        builder.setTitle(ResourceTable.String_settings_ssl_certificate_remove)
                .setContent(getString(ResourceTable.String_settings_ssl_certificate_purge, bareJid))
                .setNegativeButton(ResourceTable.String_no, DialogA::remove)
                .setPositiveButton(ResourceTable.String_yes, dialog -> {
                    for (String certEntry : certs)
                        cvs.removeCertificateEntry(certEntry);

                    // Update the adapter Account list after a deletion.
                    mCIProvider.setAccountIDs(initCertificateEntry());
                    dialog.remove();
                });
        deleteDialog = builder.create();
        deleteDialog.show();
    }

    /**
     * Constructs the connection info text.
     * Do not use ISAddress.getHostName(); this may make a network access for a reverse IP lookup
     * and cause NetworkOnMainThreadException
     */
    private String loadDetails(ProtocolProviderService pps) {
        final StringBuilder buff = new StringBuilder();
        buff.append("<html><body>");

        // Protocol name
        buff.append(getItemString(getString(ResourceTable.String_protocol), pps.getProtocolName()));

        // Server address and port
        final OperationSetConnectionInfo opSetConnInfo = pps.getOperationSet(OperationSetConnectionInfo.class);
        if (opSetConnInfo != null) {
            InetSocketAddress ISAddress = opSetConnInfo.getServerAddress();
            buff.append(getItemString(getString(ResourceTable.String_address),
                    (ISAddress == null) ? "" : ISAddress.getHostString()));
            buff.append(getItemString(getString(ResourceTable.String_port),
                    (ISAddress == null) ? "" : String.valueOf(ISAddress.getPort())));
        }

        // Transport protocol
        TransportProtocol preferredTransport = pps.getTransportProtocol();
        if (preferredTransport != TransportProtocol.UNKNOWN)
            buff.append(getItemString(getString(ResourceTable.String_callinfo_call_transport), preferredTransport.toString()));

        // TLS information
        final OperationSetTLS opSetTLS = pps.getOperationSet(OperationSetTLS.class);
        if (opSetTLS != null) {
            buff.append(getItemString(getString(ResourceTable.String_callinfo_tls_protocol), opSetTLS.getProtocol()));
            buff.append(getItemString(getString(ResourceTable.String_callinfo_tls_cipher_suite), opSetTLS.getCipherSuite()));

            buff.append("<b><u><font color=\"aqua\">")
                    .append(getString(ResourceTable.String_callinfo_view_certificate))
                    .append("</font></u></b>");
        }
        buff.append("</body></html>");
        return buff.toString();
    }

    /**
     * Returns an HTML string corresponding to the given labelText and infoText,
     * that could be easily added to the information text pane.
     *
     * @param labelText the label text that would be shown in bold
     * @param infoText the info text that would be shown in plain text
     *
     * @return the newly constructed HTML string
     */
    private String getItemString(String labelText, String infoText) {
        if (StringUtils.isNotEmpty(infoText)) {
            if (infoText.contains("TLS"))
                infoText = "<small>" + infoText + "</small>";
        }
        else
            infoText = "";

        return "&#8226; <b>" + labelText + "</b> : " + infoText + "<br/>";
    }

    /**
     * Adapter which displays Connection Info for list of <code>ProtocolProvider</code>s.
     */
    class ConnectionInfoProvider extends BaseItemProvider {
        /**
         * List of <code>AccountID</code> for which the connection info and certificates are being displayed.
         */
        private List<AccountID> accountIDs;

        /**
         * Creates a new instance of <code>SslCertificateListAdapter</code>.
         *
         * @param accountIDS the list of <code>AccountId</code>s for which connection info and
         * certificates will be displayed by this adapter.
         */
        ConnectionInfoProvider(List<AccountID> accountIDS) {
            this.accountIDs = accountIDS;
        }

        /**
         * Call to update the new List item; notify data change after update
         *
         * @param accountIDS the list of <code>AccountId</code>s for which connection info and
         * certificates will be displayed by this adapter.
         */
        public void setAccountIDs(List<AccountID> accountIDS) {
            this.accountIDs = accountIDS;
            notifyDataChanged();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount() {
            return accountIDs.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AccountID getItem(int position) {
            return accountIDs.get(position);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getComponent(int position, Component convertView, ComponentContainer parent) {
            // Keeps reference to avoid future findComponentById()
            CIViewHolder ciViewHolder;
            if (convertView == null) {
                convertView = LayoutScatter.getInstance(getContext()).parse(ResourceTable.Layout_connection_info_list_row, parent, false);
                ciViewHolder = new CIViewHolder();
                ciViewHolder.protocolService = convertView.findComponentById(ResourceTable.Id_protocolProvider);
                ciViewHolder.connectionInfo = convertView.findComponentById(ResourceTable.Id_connectionInfo);
                convertView.setTag(ciViewHolder);
            }
            else {
                ciViewHolder = (CIViewHolder) convertView.getTag();
            }

            AccountID accountId = getItem(position);
            String accountName = "<u>" + accountId + "</u>";
            ciViewHolder.protocolService.setText(Html.fromHtml(accountName, Html.FROM_HTML_MODE_LEGACY));

            String detailInfo;
            ProtocolProviderService pps = accountId.getProtocolProvider();
            if (pps != null) {
                detailInfo = loadDetails(accountId.getProtocolProvider());
            }
            else {
                detailInfo = getString(ResourceTable.String_account_unregistered, "&#8226; ");
            }

            ciViewHolder.connectionInfo.setText(Html.fromHtml(detailInfo, Html.FROM_HTML_MODE_LEGACY, null, null));
            return convertView;
        }
    }

    private static class CIViewHolder {
        Text protocolService;
        Text connectionInfo;
    }
}
