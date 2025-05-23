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
package org.atalk.ohos;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.ability.AbilityLifecycleCallbacks;
import ohos.aafwk.ability.AbilityPackage;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.utils.Point;
import ohos.agp.window.service.Display;
import ohos.agp.window.service.DisplayManager;
import ohos.app.Context;
import ohos.event.intentagent.IntentAgent;
import ohos.event.intentagent.IntentAgentConstant;
import ohos.event.intentagent.IntentAgentHelper;
import ohos.event.intentagent.IntentAgentInfo;
import ohos.global.configuration.Configuration;
import ohos.global.resource.ResourceManager;
import ohos.media.audio.AudioManager;
import ohos.media.camera.CameraKit;
import ohos.miscservices.screenlock.ScreenLockController;
import ohos.utils.PacMap;

import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.ServiceUtils;

import org.atalk.impl.appnotification.NotificationInit;
import org.atalk.impl.appstray.NotificationPopupHandler;
import org.atalk.impl.timberlog.TimberLogImpl;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.LauncherAbility;
import org.atalk.ohos.gui.Splash;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.account.AccountLoginAbility;
import org.atalk.ohos.gui.chat.ChatSessionManager;
import org.atalk.ohos.gui.dialogs.Toast;
import org.atalk.ohos.gui.util.LocaleHelper;
import org.atalk.ohos.gui.util.PixelMapCache;
import org.atalk.ohos.plugin.permissions.PermissionsAbility;
import org.atalk.persistance.DatabaseBackend;
import org.atalk.service.configuration.ConfigurationService;
import org.atalk.service.log.LogUploadService;
import org.osgi.framework.BundleContext;

import timber.log.Timber;

import static org.atalk.ohos.gui.settings.SettingsSlice.P_KEY_LOCALE;

/**
 * <code>aTalkApp</code> is used, as a global context and utility class for global actions (like EXIT broadcast).
 *
 * @author Eng Chong Meng
 */
public class aTalkApp extends AbilityPackage implements AbilityLifecycleCallbacks {
    /**
     * Name of config property that indicates whether foreground icon should be displayed.
     */
    public static final String SHOW_ICON_PROPERTY_NAME = "org.atalk.ohos.show_icon";

    /**
     * Indicate if aTalk is in the foreground (true) or background (false)
     */
    public static boolean isForeground = false;

    public static boolean permissionFirstRequest = true;

    /**
     * Static instance holder.
     */
    private static Context mInstance;

    /**
     * The currently shown activity.
     */
    private static Ability currentAbility = null;

    /**
     * Bitmap cache instance.
     */
    private final static PixelMapCache pixelMapCache = new PixelMapCache();

    /**
     * Used to track current <code>Ability</code>. This monitor is notified each time current <code>Ability</code> changes.
     */
    private static final Object currentAbilityMonitor = new Object();

    public static boolean isPortrait = true;
    public static Dimension mDisplaySize;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInitialize() {
        registerCallbacks(this, this);
        TimberLogImpl.init();

        // Must initialize Notification channels before any notification is being issued.
        new NotificationInit(this);

        // force delete in case system locked during testing
        // ServerPersistentStoresRefreshDialog.deleteDB();  // purge sql database

        // Trigger the aTalk database upgrade or creation if none exist
        DatabaseBackend.getInstance(this);
        // MigrationTo6.updateChatSessionTable(DatabaseBackend.getInstance(this).getWritableDatabase());
        // DatabaseBackend.getRdbStore().executeSql(DatabaseBackend.CREATE_ENTITY_CAPS_STATEMENT);

        // Do this after WebView(this).destroy(); Set up contextWrapper to use aTalk user selected Language
        super.onInitialize();
        // AndroidThreeTen.init(this);

        getDisplaySize();
    }

    /**
     * setLocale for Application class to work properly with PBContext class.
     */
    @Override
    public void attachBaseContext(Context base) {
        // mInstance must be initialize before getProperty() for SQLiteConfigurationStore() init.
        mInstance = base;
        String language = ConfigurationUtils.getProperty(P_KEY_LOCALE, "");
        // showToastMessage("aTalkApp reinit locale: " + language);
        mInstance = LocaleHelper.setLocale(base, language);
        super.attachBaseContext(mInstance);
    }


    /**
     * This method is for use in emulated process environments.  It will never be called on a production Android
     * device, where processes are removed by simply killing them; no user code (including this callback)
     * is executed when doing so.
     */
    @Override
    public void onEnd() {
        unregisterCallbacks(this, this);
        super.onEnd();
        mInstance = null;
    }

    /**
     * Returns the size of the main application window.
     * Must support different android API else system crashes on some devices
     * e.g. UnsupportedOperationException: in Xiaomi Mi 11 Android 11 (SDK 30)
     *
     * @return the size of the main application display window.
     */
    public static Dimension getDisplaySize() {
        // Get huawei device screen display size
        DisplayManager displayManager = DisplayManager.getInstance();
        Optional<Display> optDisplay = displayManager.getDefaultDisplay(mInstance);

        Point size = new Point(0, 0);
        if (optDisplay.isPresent()) {
            Display display = optDisplay.get();
            display.getRealSize(size);
        }
        mDisplaySize = new Dimension(size.getPointXToInt(), size.getPointYToInt());
        return mDisplaySize;
    }

    @Override
    public void onConfigurationUpdated(Configuration newConfig) {
        super.onConfigurationUpdated(newConfig);
        isPortrait = (newConfig.direction == Configuration.DIRECTION_VERTICAL);
    }

    // ========= AbilityLifecycleCallbacks implementation ======= //
    @Override
    public void onAbilityStart(Ability ability) {
    }

    @Override
    public void onAbilityActive(Ability ability) {
    }

    @Override
    public void onAbilityInactive(Ability ability) {
    }

    @Override
    public void onAbilityForeground(Ability ability) {
        isForeground = true;
        Timber.d("APP FOREGROUNDED");
    }

    @Override
    public void onAbilityBackground(Ability ability) {
        isForeground = false;
        Timber.d("APP BACKGROUNDED");
    }

    @Override
    public void onAbilityStop(Ability ability) {

    }

    @Override
    public void onAbilitySaveState(PacMap pacMap) {

    }

    // ========= AbilityLifecycleCallbacks implementation End ======= //

    /**
     * Returns true if the device is locked or screen turned off (in case password not set)
     */
    public static boolean isDeviceLocked() {
        boolean isLocked = ScreenLockController.getInstance().isScreenLocked();
        Timber.d("Android device is %s.", isLocked ? "locked" : "unlocked");
        return isLocked;
    }

    /**
     * Returns global bitmap cache of the application.
     *
     * @return global bitmap cache of the application.
     */
    public static PixelMapCache getImageCache() {
        return pixelMapCache;
    }

    /**
     * Retrieves <code>AudioManager</code> instance using application context.
     *
     * @return <code>AudioManager</code> service instance.
     */
    public static AudioManager getAudioManager() {
        return new AudioManager(mInstance);
    }

    /**
     * Retrieves <code>CameraManager</code> instance using application context.
     *
     * @return <code>CameraManager</code> service instance.
     */
    public static CameraKit getCameraManager() {
        return CameraKit.getInstance(mInstance);
    }

    /**
     * Get aTalkApp application instance
     *
     * @return aTalkApp mInstance
     */
    public static Context getInstance() {
        return mInstance;
    }

    /**
     * Returns application <code>Resources</code> object.
     *
     * @return application <code>Resources</code> object.
     */
    public static ResourceManager getAppResources() {
        return mInstance.getResourceManager();
    }

    /**
     * Returns Android string resource of the user selected language for given <code>id</code>
     * and format arguments that will be used for substitution.
     *
     * @param id the string identifier.
     * @param arg the format arguments that will be used for substitution.
     *
     * @return Android string resource for given <code>id</code> and format arguments.
     */
    public static String getResString(int id, Object... arg) {
        return mInstance.getString(id, arg);
    }

    /**
     * Toast show message in UI thread;
     * Cancel current toast view to allow immediate display of new toast message.
     *
     * @param message the string message to display.
     */
    public static void showToastMessage(final String message) {
        BaseAbility.runOnUiThread(() -> {
            Toast.showToast(mInstance, message, Toast.LENGTH_LONG);
        });
    }

    public static void showToastMessage(int id, Object... arg) {
        showToastMessage(mInstance.getString(id, arg));
    }

    public static void showGenericError(final int id, final Object... arg) {
        String msg = mInstance.getString(id, arg);
        BaseAbility.runOnUiThread(() -> {
            Toast.showToast(mInstance, msg, Toast.LENGTH_LONG);
            // String err = mInstance.getString(ResourceTable.String_error);
            // DialogH.showDialog(mInstance, mInstance.getString(ResourceTable.String_error), msg);
        });
    }

    /**
     * Returns home <code>Ability</code> class.
     *
     * @return Returns home <code>Ability</code> class.
     */
    public static Class<?> getHomeScreenAbilityClass() {
        BundleContext osgiContext = AppGUIActivator.bundleContext;
        if (osgiContext == null) {
            // If OSGI has not started show splash screen as home
            return LauncherAbility.class;
        }

        // If account manager is null means that OSGI has not started yet
        AccountManager accountManager = ServiceUtils.getService(osgiContext, AccountManager.class);
        if (accountManager == null) {
            return LauncherAbility.class;
        }

        final int accountCount = accountManager.getStoredAccounts().size();
        // Start new account Ability if none is found
        if (accountCount == 0) {
            return AccountLoginAbility.class;
        }
        else {
            // Start main view
            return aTalk.class;
        }
    }

    /**
     * Creates the home <code>Ability</code> <code>Intent</code>.
     *
     * @return the home <code>Ability</code> <code>Intent</code>.
     */
    public static Intent getHomeIntent() {
        // Home is singleTask anyway, but this way it can be started from non Ability context.
        Intent homeIntent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withBundleName(mInstance.getBundleName())
                .withAbilityName(getHomeScreenAbilityClass())
                .build();
        homeIntent.setOperation(operation);
        return homeIntent;
    }

    /**
     * Creates pending <code>Intent</code> to be started, when aTalk icon is clicked.
     *
     * @return new pending <code>Intent</code> to be started, when aTalk icon is clicked.
     */
    public static IntentAgent getaTalkIconIntent() {
        Intent intent = ChatSessionManager.getLastChatIntent();
        if (intent == null) {
            intent = getHomeIntent();
        }

        List<Intent> intentList = Collections.singletonList(intent);
        IntentAgentInfo intentInfo = new IntentAgentInfo(0, IntentAgentConstant.OperationType.START_ABILITY,
                NotificationPopupHandler.getIntentFlag(false, true), intentList, null);
        return IntentAgentHelper.getIntentAgent(mInstance, intentInfo);
    }

    /**
     * Returns <code>ConfigurationService</code> instance.
     *
     * @return <code>ConfigurationService</code> instance.
     */
    public static ConfigurationService getConfig() {
        return ServiceUtils.getService(AppGUIActivator.bundleContext, ConfigurationService.class);
    }

    /**
     * Returns <code>true</code> if aTalk notification icon should be displayed.
     *
     * @return <code>true</code> if aTalk notification icon should be displayed.
     */
    public static boolean isIconEnabled() {
        return (getConfig() == null) || getConfig().getBoolean(SHOW_ICON_PROPERTY_NAME, false);
    }

    /**
     * Sets the current activity.
     *
     * @param a the current activity to set
     */
    public static void setCurrentAbility(Ability a) {
        synchronized (currentAbilityMonitor) {
            // Timber.i("Current activity set to %s", a);
            currentAbility = a;
            // Notify listening threads
            currentAbilityMonitor.notifyAll();
        }
    }

    /**
     * Returns the current activity.
     *
     * @return the current activity
     */
    public static Ability getCurrentAbility() {
        return currentAbility;
    }

    /**
     * Displays the send logs dialog.
     */
    public static void showSendLogsDialog() {
        LogUploadService logUpload = ServiceUtils.getService(AppGUIActivator.bundleContext, LogUploadService.class);
        String defaultEmail = getConfig().getString("org.atalk.ohos.LOG_REPORT_EMAIL");

        if (logUpload != null) {
            logUpload.sendLogs(new String[]{defaultEmail},
                    getResString(ResourceTable.String_send_log_subject),
                    getResString(ResourceTable.String_send_log_title));
        }
    }

    /**
     * If OSGi has not started, then wait for the <code>LauncherAbility</code> etc to complete before
     * showing any dialog. Dialog should only be shown while <code>NOT in LaunchAbility</code> etc
     * Otherwise the dialog will be obscured by these activities; max wait = 5 waits of 1000ms each
     */
    public static Ability waitForFocus() {
        // if (AndroidGUIActivator.bundleContext == null) { #false on first application installation
        synchronized (currentAbilityMonitor) {
            int wait = 6; // 5 waits each lasting max of 1000ms
            while (wait-- > 0) {
                try {
                    currentAbilityMonitor.wait(1000);
                } catch (InterruptedException e) {
                    Timber.e("%s", e.getMessage());
                }

                if (currentAbility != null) {
                    if (!(currentAbility instanceof LauncherAbility
                            || currentAbility instanceof Splash
                            || currentAbility instanceof PermissionsAbility)) {
                        return currentAbility;
                    }
                    else {
                        Timber.d("Wait %s sec for aTalk focus on activity: %s", wait, currentAbility);
                    }
                }
            }
            return null;
        }
    }
}
