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
package org.atalk.ohos.gui.settings;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.app.Context;
import ohos.data.preferences.Preferences;

import net.java.sip.communicator.impl.msghistory.MessageHistoryActivator;
import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.systray.PopupMessageHandler;
import net.java.sip.communicator.service.systray.SystrayService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.ServiceUtils;
import net.java.sip.communicator.util.UtilActivator;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.impl.neomedia.MediaServiceImpl;
import org.atalk.impl.neomedia.NeomediaActivator;
import org.atalk.impl.neomedia.device.OhosCameraSystem;
import org.atalk.impl.neomedia.device.DeviceConfiguration;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.settings.util.SummaryMapper;
import org.atalk.ohos.gui.util.LocaleHelper;
import org.atalk.ohos.gui.util.PreferenceUtil;
import org.atalk.ohos.gui.util.ThemeHelper;
import org.atalk.ohos.gui.util.ThemeHelper.Theme;
import org.atalk.service.configuration.ConfigurationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.media.MediaLocator;
import timber.log.Timber;

/**
 * The preferences fragment implements aTalk settings.
 *
 * @author Eng Chong Meng
 */
public class SettingsSlice extends BasePreferenceSlice
        implements Preferences.PreferencesObserver {
    // PreferenceScreen and PreferenceCategories
    private static final String P_KEY_MEDIA_CALL = "pref.cat.settings.media_call";
    private static final String P_KEY_CALL = "pref.cat.settings.call";

    // Advance video/audio & Provisioning preference settings
    private static final String P_KEY_ADVANCED = "pref.cat.settings.advanced";

    // Interface Display settings
    public static final String P_KEY_LOCALE = "pref.key.locale";
    public static final String P_KEY_THEME = "pref.key.theme";

    private static final String P_KEY_WEB_PAGE = "gui.WEB_PAGE_ACCESS";

    // Message section
    private static final String P_KEY_AUTO_START = "org.atalk.ohos.auto_start";
    private static final String P_KEY_LOG_CHAT_HISTORY = "pref.key.msg.history_logging";
    private static final String P_KEY_SHOW_HISTORY = "pref.key.msg.show_history";
    private static final String P_KEY_HISTORY_SIZE = "pref.key.msg.chat_history_size";
    private static final String P_KEY_MESSAGE_DELIVERY_RECEIPT = "pref.key.message_delivery_receipt";
    private static final String P_KEY_CHAT_STATE_NOTIFICATIONS = "pref.key.msg.chat_state_notifications";
    private static final String P_KEY_XFER_THUMBNAIL_PREVIEW = "pref.key.send_thumbnail";
    private static final String P_KEY_AUTO_ACCEPT_FILE = "pref.key.auto_accept_file";
    private static final String P_KEY_PRESENCE_SUBSCRIBE_MODE = "pref.key.presence_subscribe_mode";
    // User option property names
    public static final String P_KEY_AUTO_UPDATE_CHECK_ENABLE = "pref.key.auto_update_check_enable";

    // Notifications
    private static final String P_KEY_POPUP_HANDLER = "pref.key.notification.popup_handler";
    public static final String P_KEY_HEADS_UP_ENABLE = "pref.key.notification.heads_up_enable";

    // Call section
    private static final String P_KEY_NORMALIZE_PNUMBER = "pref.key.call.remove.special";
    private static final String P_KEY_ACCEPT_ALPHA_PNUMBERS = "pref.key.call.convert.letters";

    // Video settings
    private static final String P_KEY_VIDEO_CAMERA = "pref.key.video.camera";
    // Video resolutions
    private static final String P_KEY_VIDEO_RES = "pref.key.video.resolution";

    /**
     * The device configuration
     */
    private DeviceConfiguration mDeviceConfig;

    private static ConfigurationService mConfigService;
    private PreferenceScreen mPreferenceScreen;
    private Preferences mPrefs;

    private ListPreference resList;
    private Context mContext;

    /**
     * Summary mapper used to display preferences values as summaries.
     */
    private final SummaryMapper summaryMapper = new SummaryMapper();

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        mContext = getContext();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActive() {
        super.onActive();
        setPrefTitle(ResourceTable.String_system_settings);

        // FFR: v2.1.5 NPE; use UtilActivator instead of AndroidGUIActivator which was initialized much later
        mConfigService = UtilActivator.getConfigurationService();
        mPreferenceScreen = getPreferenceScreen();
        mPrefs = BaseAbility.getPreferenceStore();
        mPrefs.registerObserver(this);
        mPrefs.registerObserver(summaryMapper);

        // init display locale and theme (not implemented)
        initLocale();
        initTheme();
        initWebPagePreference();

        // Messages section
        initMessagesPreferences();

        // Notifications section
        initNotificationPreferences();
        initAutoStart();

        if (!aTalk.disableMediaServiceOnFault) {
            MediaServiceImpl mediaServiceImpl = NeomediaActivator.getMediaServiceImpl();
            if (mediaServiceImpl != null) {
                mDeviceConfig = mediaServiceImpl.getDeviceConfiguration();
            }
            else {
                // Do not proceed if mediaServiceImpl == null; else system crashes on NPE
                disableMediaOptions();
                return;
            }

            // Call section
            initCallPreferences();

            // Video section
            initVideoPreferences();
        }
        else {
            disableMediaOptions();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        mPrefs.unregisterObserver(this);
        mPrefs.unregisterObserver(summaryMapper);
        super.onStop();
    }

    private void initAutoStart() {
        ConfigurationUtils.setAutoStart(false);
        findPreference(P_KEY_AUTO_START).setEnabled(false);

        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_AUTO_START, ConfigurationUtils.isAutoStartEnable());
    }

    /**
     * Initialize web default access page
     */
    private void initWebPagePreference() {
        // Updates displayed history size summary.
        EditTextPreference webPagePref = findPreference(P_KEY_WEB_PAGE);
        webPagePref.setText(ConfigurationUtils.getWebPage());
        updateWebPageSummary();
    }

    private void updateWebPageSummary() {
        EditTextPreference webPagePref = findPreference(P_KEY_WEB_PAGE);
        webPagePref.setSummary(ConfigurationUtils.getWebPage());
    }

    /**
     * Initialize interface Locale
     */
    protected void initLocale() {
        // Immutable empty {@link CharSequence} array
        CharSequence[] EMPTY_CHAR_SEQUENCE_ARRAY = new CharSequence[0];
        final ListPreference pLocale = findPreference(P_KEY_LOCALE);

        List<CharSequence> entryVector = new ArrayList<CharSequence>(Arrays.asList(pLocale.getEntries()));
        List<CharSequence> entryValueVector = new ArrayList<CharSequence>(Arrays.asList(pLocale.getEntryValues()));
        String[] supportedLanguages =  getStringArray(ResourceTable.Strarray_supported_languages);
        Set<String> supportedLanguageSet = new HashSet<>(Arrays.asList(supportedLanguages));
        for (int i = entryVector.size() - 1; i > -1; --i) {
            if (!supportedLanguageSet.contains(entryValueVector.get(i).toString())) {
                entryVector.remove(i);
                entryValueVector.remove(i);
            }
        }

        CharSequence[] entries = entryVector.toArray(EMPTY_CHAR_SEQUENCE_ARRAY);
        CharSequence[] entryValues = entryValueVector.toArray(EMPTY_CHAR_SEQUENCE_ARRAY);
        String language = LocaleHelper.getLanguage();

        pLocale.setEntries(entries);
        pLocale.setEntryValues(entryValues);
        pLocale.setValue(language);
        pLocale.setSummary(pLocale.getEntry());

        // summaryMapper not working for Locale, so use this instead
        pLocale.setOnPreferenceChangeListener((preference, value) -> {
            String language1 = value.toString();
            pLocale.setValue(language1);
            pLocale.setSummary(pLocale.getEntry());

            // Save selected language in DB
            mConfigService.setProperty(P_KEY_LOCALE, language1);

            // Need to destroy and restart to set new language if there is a change
            if (!language.equals(value) && (mContext != null)) {
                // All language setting changes must call via aTalkApp so its contextWrapper is updated
                LocaleHelper.setLocale(getContext(), language1);

                // must get aTalk to restart onActive to show correct UI for preference menu
                aTalk.setPrefChange(aTalk.Locale_Change);

                // do destroy activity last
                Intent intent = new Intent();
                Operation operation = new Intent.OperationBuilder()
                        .withDeviceId("")
                        .withBundleName(aTalkApp.getInstance().getBundleName())
                        .withAbilityName(SettingsAbility.class)
                        .build();
                intent.setOperation(operation);
                mContext.startAbility(intent, 0);
                terminate();
            }
            return true;
        });
    }

    /**
     * Initialize interface Theme
     */
    protected void initTheme() {
        final ListPreference pTheme = findPreference(P_KEY_THEME);
        String nTheme = ThemeHelper.isAppTheme(Theme.LIGHT) ? "light" : "dark";
        pTheme.setValue(nTheme);
        pTheme.setSummary(pTheme.getEntry());

        // summaryMapper not working for Theme. so use this instead
        pTheme.setOnPreferenceChangeListener((preference, value) -> {
            pTheme.setValue((String) value);
            pTheme.setSummary(pTheme.getEntry());

            // Save Display Theme to DB
            Theme vTheme = value.equals("light") ? Theme.LIGHT : Theme.DARK;
            mConfigService.setProperty(P_KEY_THEME, vTheme.ordinal());

            // Need to destroy and restart to set new Theme if there is a change
            if (!nTheme.equals(value) && (mContext != null)) {
                ThemeHelper.setTheme(mContext, vTheme);
                // must get aTalk to restart onActive to show new Theme
                aTalk.setPrefChange(aTalk.Theme_Change);

                Intent intent = new Intent();
                Operation operation = new Intent.OperationBuilder()
                        .withDeviceId("")
                        .withBundleName(aTalkApp.getInstance().getBundleName())
                        .withAbilityName(SettingsAbility.class)
                        .build();
                intent.setOperation(operation);
                mContext.startAbility(intent, 0);
                terminate();
            }
            return true;
        });
    }

    /**
     * Initializes messages section
     */
    private void initMessagesPreferences() {
        // mhs may be null if user access settings before the mhs service is properly setup
        MessageHistoryService mhs = MessageHistoryActivator.getMessageHistoryService();
        boolean isHistoryLoggingEnabled = (mhs != null) && mhs.isHistoryLoggingEnabled();
        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_LOG_CHAT_HISTORY, isHistoryLoggingEnabled);

        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_SHOW_HISTORY, ConfigurationUtils.isHistoryShown());

        // Updates displayed history size summary.
        EditTextPreference historySizePref = findPreference(P_KEY_HISTORY_SIZE);
        historySizePref.setText(Integer.toString(ConfigurationUtils.getChatHistorySize()));
        updateHistorySizeSummary();

        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_MESSAGE_DELIVERY_RECEIPT,
                ConfigurationUtils.isSendMessageDeliveryReceipt());

        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_CHAT_STATE_NOTIFICATIONS,
                ConfigurationUtils.isSendChatStateNotifications());

        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_XFER_THUMBNAIL_PREVIEW,
                ConfigurationUtils.isSendThumbnail());

        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_PRESENCE_SUBSCRIBE_MODE,
                ConfigurationUtils.isPresenceSubscribeAuto());

        initAutoAcceptFileSize();
        // PreferenceUtil.setCheckboxVal(this, P_KEY_CHAT_ALERTS, ConfigurationUtils.isAlerterEnabled());
    }

    /**
     * Updates displayed history size summary.
     */
    private void updateHistorySizeSummary() {
        EditTextPreference historySizePref = findPreference(P_KEY_HISTORY_SIZE);
        historySizePref.setSummary(getString(ResourceTable.String_settings_history_summary,
                ConfigurationUtils.getChatHistorySize()));
    }

    /**
     * Initialize auto accept file size
     */
    protected void initAutoAcceptFileSize() {
        final ListPreference fileSizeList = findPreference(P_KEY_AUTO_ACCEPT_FILE);
        fileSizeList.setEntries(R.array.filesizes);
        fileSizeList.setEntryValues(R.array.filesizes_values);
        long filesSize = ConfigurationUtils.getAutoAcceptFileSize();
        fileSizeList.setValue(String.valueOf(filesSize));
        fileSizeList.setSummary(fileSizeList.getEntry());

        // summaryMapper not working for auto accept fileSize so use this instead
        fileSizeList.setOnPreferenceChangeListener((preference, value) -> {
            String fileSize = value.toString();
            fileSizeList.setValue(fileSize);
            fileSizeList.setSummary(fileSizeList.getEntry());

            ConfigurationUtils.setAutoAcceptFileSizeSize(Integer.parseInt(fileSize));
            return true;
        });
    }

    /**
     * Initializes notifications section
     */
    private void initNotificationPreferences() {
        BundleContext bc = AppGUIActivator.bundleContext;
        ServiceReference[] handlerRefs = ServiceUtils.getServiceReferences(bc, PopupMessageHandler.class);

        String[] names = new String[handlerRefs.length + 1]; // +1 Auto
        String[] values = new String[handlerRefs.length + 1];
        names[0] = getString(ResourceTable.String_popup_auto);
        values[0] = "Auto";
        int selectedIdx = 0; // Auto by default

        // mCongService may be null feedback NPE from the field report, so just assume null i.e.
        // "Auto" selected. Delete the user's preference and select the best available handler.
        String configuredHandler = (mConfigService == null) ?
                null : mConfigService.getString("systray.POPUP_HANDLER");
        int idx = 1;
        for (ServiceReference<PopupMessageHandler> ref : handlerRefs) {
            PopupMessageHandler handler = bc.getService(ref);

            names[idx] = handler.toString();
            values[idx] = handler.getClass().getName();

            if ((configuredHandler != null) && configuredHandler.equals(handler.getClass().getName())) {
                selectedIdx = idx;
            }
        }

        // Configures ListPreference
        ListPreference handlerList = findPreference(P_KEY_POPUP_HANDLER);
        handlerList.setEntries(names);
        handlerList.setEntryValues(values);
        handlerList.setValueIndex(selectedIdx);
        // Summaries mapping
        summaryMapper.includePreference(handlerList, "Auto");

        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_HEADS_UP_ENABLE,
                ConfigurationUtils.isHeadsUpEnable());
    }

    // Disable all media options when MediaServiceImpl is not initialized due to text-relocation in ffmpeg
    private void disableMediaOptions() {
        PreferenceCategory myPrefCat = findPreference(P_KEY_MEDIA_CALL);
        if (myPrefCat != null)
            mPreferenceScreen.removePreference(myPrefCat);

        myPrefCat = findPreference(P_KEY_CALL);
        if (myPrefCat != null)
            mPreferenceScreen.removePreference(myPrefCat);

        // android OS cannot support removal of nested PreferenceCategory, so just disable all advance settings
        myPrefCat = findPreference(P_KEY_ADVANCED);
        if (myPrefCat != null) {
            mPreferenceScreen.removePreference(myPrefCat);
        }
    }

    /**
     * Initializes call section
     */
    private void initCallPreferences() {
        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_NORMALIZE_PNUMBER,
                ConfigurationUtils.isNormalizePhoneNumber());
        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_ACCEPT_ALPHA_PNUMBERS,
                ConfigurationUtils.acceptPhoneNumberWithAlphaChars());
    }

    /**
     * Initializes video preferences part.
     */
    private void initVideoPreferences() {
        AndroidCamera[] cameras = AndroidCamera.getCameras();
        String[] names = new String[cameras.length];
        String[] values = new String[cameras.length];
        for (int i = 0; i < cameras.length; i++) {
            names[i] = cameras[i].getName();
            values[i] = cameras[i].getLocator().toString();
        }

        ListPreference cameraList = findPreference(P_KEY_VIDEO_CAMERA);
        cameraList.setEntries(names);
        cameraList.setEntryValues(values);

        // Get camera from configuration
        AndroidCamera currentCamera = AndroidCamera.getSelectedCameraDevInfo();
        if (currentCamera != null)
            cameraList.setValue(currentCamera.getLocator().toString());

        // Resolutions
        int resolutionSize = CameraUtils.PREFERRED_SIZES.length;
        String[] resolutionValues = new String[resolutionSize];
        for (int i = 0; i < resolutionSize; i++) {
            resolutionValues[i] = resToStr(CameraUtils.PREFERRED_SIZES[i]);
        }

        resList = findPreference(P_KEY_VIDEO_RES);
        resList.setEntries(resolutionValues);
        resList.setEntryValues(resolutionValues);

        // Init current resolution
        resList.setValue(resToStr(mDeviceConfig.getVideoSize()));

        // Summaries mapping
        summaryMapper.includePreference(cameraList, getString(ResourceTable.String_settings_no_camera));
        summaryMapper.includePreference(resList, "720x480");
    }

    /**
     * Converts resolution to string.
     *
     * @param d resolution as <code>Dimension</code>
     *
     * @return resolution string.
     */
    private static String resToStr(Dimension d) {
        return ((int) d.getWidth()) + "x" + ((int) d.getHeight());
    }

    /**
     * Selects resolution from supported resolutions list for given string.
     *
     * @param resStr resolution string created with method {@link #resToStr(Dimension)}.
     *
     * @return resolution <code>Dimension</code> for given string representation created with method
     * {@link #resToStr(Dimension)}
     */
    private static Dimension getResForStr(String resStr) {
        Dimension[] supportedResolutions = OhosCameraSystem.SUPPORTED_SIZES;
        for (Dimension resolution : supportedResolutions) {
            if (resToStr(resolution).equals(resStr))
                return resolution;
        }

        // If none matched, then get the closer size.
        double minDiff = Double.MAX_VALUE;
        int w = Integer.parseInt(resStr.split("x")[0]);
        int h = Integer.parseInt(resStr.split("x")[1]);

        Dimension optSize = new Dimension(w, h);
        for (Dimension size : supportedResolutions) {
            if (Math.abs(size.getHeight() - h) < minDiff) {
                optSize = size;
                minDiff = Math.abs(size.getHeight() - h);
            }
        }
        return optSize;
    }

    /**
     * Retrieves currently registered <code>PopupMessageHandler</code> for given <code>clazz</code> name.
     *
     * @param clazz the class name of <code>PopupMessageHandler</code> implementation.
     *
     * @return implementation of <code>PopupMessageHandler</code> for given class name registered in OSGI context.
     */
    private PopupMessageHandler getHandlerForClassName(String clazz) {
        BundleContext bc = AppGUIActivator.bundleContext;
        ServiceReference[] handlerRefs = ServiceUtils.getServiceReferences(bc, PopupMessageHandler.class);

        for (ServiceReference<PopupMessageHandler> sRef : handlerRefs) {
            PopupMessageHandler handler = bc.getService(sRef);
            if (handler.getClass().getName().equals(clazz))
                return handler;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void onChange(Preferences shPreferences, String key) {
        // FR - mConfigService may be null???
        if (ConfigurationUtils.mConfigService == null)
            return;

        switch (key) {
            case P_KEY_LOG_CHAT_HISTORY:
                MessageHistoryService mhs = MessageHistoryActivator.getMessageHistoryService();
                boolean enable = false;
                if (mhs != null) {
                    enable = shPreferences.getBoolean(P_KEY_LOG_CHAT_HISTORY, mhs.isHistoryLoggingEnabled());
                    mhs.setHistoryLoggingEnabled(enable);
                }
                enableMam(enable);
                break;
            case P_KEY_SHOW_HISTORY:
                ConfigurationUtils.setHistoryShown(shPreferences.getBoolean(P_KEY_SHOW_HISTORY, ConfigurationUtils.isHistoryShown()));
                break;
            case P_KEY_HISTORY_SIZE:
                String intStr = shPreferences.getString(P_KEY_HISTORY_SIZE, Integer.toString(ConfigurationUtils.getChatHistorySize()));
                ConfigurationUtils.setChatHistorySize(Integer.parseInt(intStr));
                updateHistorySizeSummary();
                break;
            case P_KEY_WEB_PAGE:
                String wpStr = shPreferences.getString(P_KEY_WEB_PAGE, ConfigurationUtils.getWebPage());
                ConfigurationUtils.setWebPage(wpStr);
                updateWebPageSummary();
                break;
            case P_KEY_AUTO_START:
                ConfigurationUtils.setAutoStart(shPreferences.getBoolean(
                        P_KEY_AUTO_START, ConfigurationUtils.isAutoStartEnable()));
                break;
            case P_KEY_MESSAGE_DELIVERY_RECEIPT:
                ConfigurationUtils.setSendMessageDeliveryReceipt(shPreferences.getBoolean(
                        P_KEY_MESSAGE_DELIVERY_RECEIPT, ConfigurationUtils.isSendMessageDeliveryReceipt()));
                break;
            case P_KEY_CHAT_STATE_NOTIFICATIONS:
                ConfigurationUtils.setSendChatStateNotifications(shPreferences.getBoolean(
                        P_KEY_CHAT_STATE_NOTIFICATIONS, ConfigurationUtils.isSendChatStateNotifications()));
                break;
            case P_KEY_XFER_THUMBNAIL_PREVIEW:
                ConfigurationUtils.setSendThumbnail(shPreferences.getBoolean(
                        P_KEY_XFER_THUMBNAIL_PREVIEW, ConfigurationUtils.isSendThumbnail()));
                break;
            case P_KEY_PRESENCE_SUBSCRIBE_MODE:
                ConfigurationUtils.setPresenceSubscribeAuto(shPreferences.getBoolean(
                        P_KEY_PRESENCE_SUBSCRIBE_MODE, ConfigurationUtils.isPresenceSubscribeAuto()));
                break;

//            case P_KEY_AUTO_UPDATE_CHECK_ENABLE:
//                Boolean isEnable = shPreferences.getBoolean(P_KEY_AUTO_UPDATE_CHECK_ENABLE, true);
//                mConfigService.setProperty(AUTO_UPDATE_CHECK_ENABLE, isEnable);
//
//                Operation operation = new Intent.OperationBuilder()
//                        .withDeviceId("")
//                        .withBundleName(getBundleName())
//                        .withAbilityName(OnlineUpdateService.class)
//                        .build();
//
//                Intent updateIntent = new Intent();
//                updateIntent.setOperation(operation);
//
//                // Perform software version update check on first launch
//                updateIntent.setParam(OnlineUpdateService.ACTION, isEnable ?
//                        OnlineUpdateService.ACTION_AUTO_UPDATE_START : OnlineUpdateService.ACTION_AUTO_UPDATE_STOP);
//                startAbility(updateIntent);
//                break;

            /*
             * Chat alerter is not implemented on Android
             * else if(key.equals(P_KEY_CHAT_ALERTS)) {
             *  ConfigurationUtils.setAlerterEnabled( shPreferences.getBoolean( P_KEY_CHAT_ALERTS,
             *  ConfigurationUtils.isAlerterEnabled()));
             * }
             */
            case P_KEY_POPUP_HANDLER:
                String handler = shPreferences.getString(P_KEY_POPUP_HANDLER, "Auto");
                SystrayService systray = AppGUIActivator.getSystrayService();
                if ("Auto".equals(handler)) {
                    // "Auto" selected. Delete the user's preference and select the best available handler.
                    ConfigurationUtils.setPopupHandlerConfig(null);
                    systray.selectBestPopupMessageHandler();
                }
                else {
                    ConfigurationUtils.setPopupHandlerConfig(handler);
                    PopupMessageHandler handlerInstance = getHandlerForClassName(handler);
                    if (handlerInstance == null) {
                        Timber.w("No handler found for name: %s", handler);
                    }
                    else {
                        systray.setActivePopupMessageHandler(handlerInstance);
                    }
                }
                break;
            case P_KEY_HEADS_UP_ENABLE:
                ConfigurationUtils.setHeadsUp(shPreferences.getBoolean(P_KEY_HEADS_UP_ENABLE, true));
                break;
            // Normalize phone number
            case P_KEY_NORMALIZE_PNUMBER:
                ConfigurationUtils.setNormalizePhoneNumber(shPreferences.getBoolean(P_KEY_NORMALIZE_PNUMBER, true));
                break;
            // Camera
            case P_KEY_VIDEO_CAMERA:
                String cameraName = shPreferences.getString(P_KEY_VIDEO_CAMERA, null);
                AndroidCamera.setSelectedCamera(new MediaLocator(cameraName));
                break;
            // Video resolution
            case P_KEY_VIDEO_RES:
                String resStr = shPreferences.getString(P_KEY_VIDEO_RES, null);
                Dimension videoRes = getResForStr(resStr);
                mDeviceConfig.setVideoSize(videoRes);
                resList.setValue(resToStr(mDeviceConfig.getVideoSize()));
                break;
        }
    }

    /**
     * Enable or disable MAM service according per the P_KEY_LOG_CHAT_HISTORY new setting.
     *
     * @param enable mam state to be updated with.
     */
    private void enableMam(boolean enable) {
        Collection<ProtocolProviderService> providers = AccountUtils.getRegisteredProviders();
        for (ProtocolProviderService pps : providers) {
            if (pps.isRegistered()) {
                ProtocolProviderServiceJabberImpl.enableMam(pps.getConnection(), enable);
            }
            else {
                aTalkApp.showToastMessage(ResourceTable.String_settings_history_mam_warning, pps.getAccountID().getEntityBareJid());
            }
        }
    }
}
