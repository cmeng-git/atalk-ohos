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

import ohos.data.preferences.Preferences;

import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.plugin.jabberaccregwizz.JabberAccountRegistrationActivator;
import net.java.sip.communicator.service.certificate.CertificateConfigEntry;
import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.jabber.JabberAccountRegistration;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.settings.BasePreferenceSlice;
import org.atalk.ohos.gui.settings.util.SummaryMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import timber.log.Timber;

/**
 * The preferences fragment implements for xmpp connection settings.
 *
 * @author Eng Chong Meng
 */
public class XmppConnectionSlice extends BasePreferenceSlice implements Preferences.PreferencesObserver {
    // Account General
    private static final String P_KEY_GMAIL_NOTIFICATIONS = "pref_key_gmail_notifications";
    private static final String P_KEY_GOOGLE_CONTACTS_ENABLED = "pref_key_google_contact_enabled";
    private static final String P_KEY_DTMF_METHOD = "pref_key_dtmf_method";

    // Client TLS certificate
    private static final String P_KEY_TLS_CERT_ID = "pref_key_client_tls_cert";

    // Server Options
    private static final String P_KEY_IS_KEEP_ALIVE_ENABLE = "pref_key_is_keep_alive_enable";
    public static final String P_KEY_PING_INTERVAL = "pref_key_ping_interval";
    private static final String P_KEY_IS_PING_AUTO_TUNE_ENABLE = "pref_key_ping_auto_tune_enable";
    private static final String P_KEY_IS_SERVER_OVERRIDDEN = "pref_key_is_server_overridden";
    public static final String P_KEY_SERVER_ADDRESS = "pref_key_server_address";
    public static final String P_KEY_SERVER_PORT = "pref_key_server_port";
    private static final String P_KEY_MINIMUM_TLS_VERSION = "pref_key_minimum_TLS_version";
    private static final String P_KEY_ALLOW_NON_SECURE_CONN = "pref_key_allow_non_secure_conn";

    // Jabber Resource
    private static final String P_KEY_AUTO_GEN_RESOURCE = "pref_key_auto_gen_resource";
    private static final String P_KEY_RESOURCE_NAME = "pref_key_resource_name";
    private static final String P_KEY_RESOURCE_PRIORITY = "pref_key_resource_priority";

    /*
     * A new instance of AccountID and is not the same as accountID.
     * Defined as static, otherwise it may get clear onActivityResult - on some android devices
     */
    private static JabberAccountRegistration jbrReg;
    protected Preferences mPrefs;

    /**
     * We load values only once into shared preferences to not reset values on screen rotated event.
     */
    private final boolean isInitialized = false;

    /**
     * Summary mapper used to display preferences values as summaries.
     */
    private final SummaryMapper summaryMapper = new SummaryMapper();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        String accountID = getArguments().getString(AccountPreferenceSlice.EXTRA_ACCOUNT_ID);
        AccountID account = AccountUtils.getAccountIDForUID(accountID);

        ProtocolProviderService pps = AccountUtils.getRegisteredProviderForAccount(account);
        if (pps == null) {
            Timber.w("No protocol provider registered for %s", account);
            return;
        }

        // must initPreferences before setPreferencesFromResource; else updated value will not be reflected
        initPreferences();
        setPreferencesFromResource(R.xml.xmpp_connection_preferences, rootKey);
        setPrefTitle(ResourceTable.String_jbr_connection);

        mPrefs.registerObserver(this);
        mPrefs.registerObserver(summaryMapper);

        initTLSCert(pps.getAccountID());
        mapSummaries(summaryMapper);
    }

    protected void initPreferences() {
        jbrReg = JabberPreferenceSlice.jbrReg;
        mPrefs = BaseAbility.getPreferenceStore();

        // Connection
        mPrefs.putBoolean(P_KEY_GMAIL_NOTIFICATIONS, jbrReg.isGmailNotificationEnabled());
        mPrefs.putBoolean(P_KEY_GOOGLE_CONTACTS_ENABLED, jbrReg.isGoogleContactsEnabled());
        mPrefs.putString(P_KEY_MINIMUM_TLS_VERSION, jbrReg.getMinimumTLSversion());
        mPrefs.putBoolean(P_KEY_ALLOW_NON_SECURE_CONN, jbrReg.isAllowNonSecure());
        mPrefs.putString(P_KEY_DTMF_METHOD, jbrReg.getDTMFMethod());

        // Keep alive options
        mPrefs.putBoolean(P_KEY_IS_KEEP_ALIVE_ENABLE, jbrReg.isKeepAliveEnable());
        mPrefs.putString(P_KEY_PING_INTERVAL, jbrReg.getPingInterval());
        mPrefs.putBoolean(P_KEY_IS_PING_AUTO_TUNE_ENABLE, jbrReg.isPingAutoTuneEnable());
        mPrefs.putString(P_KEY_TLS_CERT_ID, jbrReg.getTlsClientCertificate());

        // Server options
        mPrefs.putBoolean(P_KEY_IS_SERVER_OVERRIDDEN, jbrReg.isServerOverridden());
        mPrefs.putString(P_KEY_SERVER_ADDRESS, jbrReg.getServerAddress());
        mPrefs.putString(P_KEY_SERVER_PORT, jbrReg.getServerPort());

        // Resource
        mPrefs.putBoolean(P_KEY_AUTO_GEN_RESOURCE, jbrReg.isResourceAutoGenerated());
        mPrefs.putString(P_KEY_RESOURCE_NAME, jbrReg.getResource());
        mPrefs.putString(P_KEY_RESOURCE_PRIORITY, String.valueOf(jbrReg.getPriority()));

        mPrefs.flush();
    }

    /**
     * Initialize the client TLS certificate selection list
     */
    private void initTLSCert(AccountID accountID) {
        List<String> certList = new ArrayList<>();

        CertificateService cvs = JabberAccountRegistrationActivator.getCertificateService();
        List<CertificateConfigEntry> certEntries = cvs.getClientAuthCertificateConfigs();
        certEntries.add(0, CertificateConfigEntry.CERT_NONE);

        for (CertificateConfigEntry e : certEntries) {
            certList.add(e.toString());
        }

        String currentCert = accountID.getTlsClientCertificate();
        if (!certList.contains(currentCert) && !isInitialized) {
            // Use the empty one i.e. None cert
            currentCert = certList.get(0);
            mPrefs.putString(P_KEY_TLS_CERT_ID, currentCert).flush();
        }

        String[] entries = new String[certList.size()];
        entries = certList.toArray(entries);
        ListPreference certPreference = findPreference(P_KEY_TLS_CERT_ID);
        certPreference.setEntries(entries);
        certPreference.setEntryValues(entries);

        if (!isInitialized)
            certPreference.setValue(currentCert);
    }

    /**
     * {@inheritDoc}
     */
    protected void mapSummaries(SummaryMapper summaryMapper) {
        String emptyStr = getString(ResourceTable.String_settings_not_set);

        // DTMF Option
        summaryMapper.includePreference(findPreference(P_KEY_DTMF_METHOD), emptyStr, input -> {
            ListPreference lp = findPreference(P_KEY_DTMF_METHOD);
            return lp.getEntry().toString();
        });

        // Ping interval
        summaryMapper.includePreference(findPreference(P_KEY_PING_INTERVAL), emptyStr);
        summaryMapper.includePreference(findPreference(P_KEY_TLS_CERT_ID), emptyStr);

        // Server options
        summaryMapper.includePreference(findPreference(P_KEY_SERVER_ADDRESS), emptyStr);
        summaryMapper.includePreference(findPreference(P_KEY_SERVER_PORT), emptyStr);

        // Resource
        summaryMapper.includePreference(findPreference(P_KEY_RESOURCE_NAME), emptyStr);
        summaryMapper.includePreference(findPreference(P_KEY_RESOURCE_PRIORITY), emptyStr);
    }

    /**
     * {@inheritDoc}
     */
    public void onChange(Preferences shPreferences, String key) {
        // Check to ensure a valid key before proceed
        if (!mPrefs.hasKey(key))
            return;

        JabberPreferenceSlice.setUncommittedChanges();
        switch (key) {
            case P_KEY_GMAIL_NOTIFICATIONS:
                jbrReg.setGmailNotificationEnabled(mPrefs.getBoolean(P_KEY_GMAIL_NOTIFICATIONS, false));
                break;
            case P_KEY_GOOGLE_CONTACTS_ENABLED:
                jbrReg.setGoogleContactsEnabled(mPrefs.getBoolean(P_KEY_GOOGLE_CONTACTS_ENABLED, false));
                break;
            case P_KEY_MINIMUM_TLS_VERSION:
                String newMinimumTLSVersion = mPrefs.getString(P_KEY_MINIMUM_TLS_VERSION,
                        ProtocolProviderServiceJabberImpl.defaultMinimumTLSversion);
                boolean isSupported = false;
                try {
                    String[] supportedProtocols
                            = ((SSLSocket) SSLSocketFactory.getDefault().createSocket()).getSupportedProtocols();
                    for (String suppProto : supportedProtocols) {
                        if (suppProto.equals(newMinimumTLSVersion)) {
                            isSupported = true;
                            break;
                        }
                    }
                } catch (IOException ignore) {
                }
                if (!isSupported) {
                    newMinimumTLSVersion = ProtocolProviderServiceJabberImpl.defaultMinimumTLSversion;
                }
                jbrReg.setMinimumTLSversion(newMinimumTLSVersion);
                break;
            case P_KEY_ALLOW_NON_SECURE_CONN:
                jbrReg.setAllowNonSecure(mPrefs.getBoolean(P_KEY_ALLOW_NON_SECURE_CONN, false));
                break;
            case P_KEY_DTMF_METHOD:
                jbrReg.setDTMFMethod(mPrefs.getString(P_KEY_DTMF_METHOD, null));
                break;
            case P_KEY_IS_KEEP_ALIVE_ENABLE:
                jbrReg.setKeepAliveOption(mPrefs.getBoolean(P_KEY_IS_KEEP_ALIVE_ENABLE, true));
                break;
            case P_KEY_PING_INTERVAL:
                jbrReg.setPingInterval(mPrefs.getString(P_KEY_PING_INTERVAL,
                        Integer.toString(ProtocolProviderServiceJabberImpl.defaultPingInterval)));
                break;
            case P_KEY_IS_PING_AUTO_TUNE_ENABLE:
                jbrReg.setPingAutoTuneOption(mPrefs.getBoolean(P_KEY_IS_PING_AUTO_TUNE_ENABLE, true));
                break;
            case P_KEY_TLS_CERT_ID:
                jbrReg.setTlsClientCertificate(mPrefs.getString(P_KEY_TLS_CERT_ID, null));
                break;
            case P_KEY_IS_SERVER_OVERRIDDEN:
                jbrReg.setServerOverridden(mPrefs.getBoolean(P_KEY_IS_SERVER_OVERRIDDEN, false));
                break;
            case P_KEY_SERVER_ADDRESS:
                jbrReg.setServerAddress(mPrefs.getString(P_KEY_SERVER_ADDRESS, null));
                break;
            case P_KEY_SERVER_PORT:
                jbrReg.setServerPort(mPrefs.getString(P_KEY_SERVER_PORT,
                        Integer.toString(ProtocolProviderServiceJabberImpl.DEFAULT_PORT)));
                break;
            case P_KEY_AUTO_GEN_RESOURCE:
                jbrReg.setResourceAutoGenerated(mPrefs.getBoolean(P_KEY_AUTO_GEN_RESOURCE, true));
                break;
            case P_KEY_RESOURCE_NAME:
                jbrReg.setResource(mPrefs.getString(P_KEY_RESOURCE_NAME, null));
                break;
            case P_KEY_RESOURCE_PRIORITY:
                try {
                    jbrReg.setPriority(mPrefs.getInt(P_KEY_RESOURCE_PRIORITY, 30));
                } catch (Exception ex) {
                    Timber.w("Invalid resource priority: %s", ex.getMessage());
                }
                break;
        }
    }
}

