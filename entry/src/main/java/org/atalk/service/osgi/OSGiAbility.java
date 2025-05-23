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
package org.atalk.service.osgi;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.ability.IAbilityConnection;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.Text;
import ohos.bundle.ElementName;
import ohos.eventhandler.EventRunner;
import ohos.rpc.IRemoteObject;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.LauncherAbility;
import org.atalk.ohos.gui.actionbar.ActionBarUtil;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.plugin.errorhandler.ExceptionHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Implements a base <code>AbilitySlice</code> which employs OSGi.
 *
 * @author Eng Chong Meng
 */
public class OSGiAbility extends BaseAbility {
    private BundleActivator bundleActivator;

    private BundleContext bundleContext;

    private BundleContextHolder mService;

    private AbilityConnection mServiceConnection;

    /**
     * List of attached {@link OSGiUiPart}.
     */
    private final List<OSGiUiPart> OSGiSlices = new ArrayList<>();

    /**
     * Called when the activity is starting. Initializes the corresponding call interface.
     * Both setLanguage and setTheme must happen before super.onStart(intent) is called
     *
     * @param intent If the activity is being re-initialized after previously being shut down
     * then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     * Note: Otherwise it is null.
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);

        // Hooks the exception handler to the UI thread
        ExceptionHandler.checkAndAttachExceptionHandler();

        mServiceConnection = new AbilityConnection();
        boolean bindService = false;
        try {
            bindService = bindService(new Intent(this, OSGiService.class), mServiceConnection, BIND_AUTO_CREATE);
        } finally {
            if (!bindService)
                mServiceConnection = null;
        }
    }

    public class AbilityConnection implements IAbilityConnection {
        @Override
        public void onAbilityConnectDone(ElementName elementName, IRemoteObject iRemoteObject, int i) {
            if (this == mServiceConnection)
                setService((BundleContextHolder) iRemoteObject);
        }

        @Override
        public void onAbilityDisconnectDone(ElementName elementName, int i) {
            if (this == mServiceConnection)
                setService(null);
        }
    }

    @Override
    protected void onActive() {
        super.onActive();
        // If OSGi service is running check for send logs
        if (bundleContext != null) {
            checkForSendLogsDialog();
        }
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        // Clear the references to this activity.
        clearReferences();
    }

    /**
     * Called when an activity is destroyed.
     */
    @Override
    protected void onStop() {
        super.onStop();
        ServiceConnection serviceConnection = this.mServiceConnection;
        this.mServiceConnection = null;
        try {
            setService(null);
        } finally {
            if (serviceConnection != null)
                unbindService(serviceConnection);
        }
    }


    /**
     * Checks if the crash has occurred since the aTalk was last started. If it's true asks the
     * user about eventual logs report.
     */
    private void checkForSendLogsDialog() {
        // Checks if aTalk has previously crashed and asks the user user about log reporting
        if (!ExceptionHandler.hasCrashed()) {
            return;
        }
        // Clears the crash status and ask user to send debug log
        ExceptionHandler.resetCrashedStatus();
        DialogA.Builder question = new DialogA.Builder(this);
        question.setTitle(ResourceTable.String_warning)
                .setContent("$string:send_log_prompt")
                .setNegativeButton(ResourceTable.String_no, DialogA::remove)
                .setPositiveButton(ResourceTable.String_yes, dialog -> {
                    dialog.remove();
                    aTalkApp.showSendLogsDialog();
                })
                .create().show();
    }

    private void setService(BundleContextHolder service) {
        if (mService != service) {
            if ((mService != null) && (bundleActivator != null)) {
                try {
                    mService.removeBundleActivator(bundleActivator);
                    bundleActivator = null;
                } finally {
                    try {
                        internalStop(null);
                    } catch (Throwable t) {
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                    }
                }
            }

            mService = service;
            if (mService != null) {
                if (bundleActivator == null) {
                    bundleActivator = new BundleActivator() {
                        public void start(BundleContext bundleContext)
                                throws Exception {
                            internalStart(bundleContext);
                        }

                        public void stop(BundleContext bundleContext)
                                throws Exception {
                            internalStop(bundleContext);
                        }
                    };
                }
                mService.addBundleActivator(bundleActivator);
            }
        }
    }

    /**
     * Starts this osgi activity.
     *
     * @param bundleContext the osgi <code>BundleContext</code>
     *
     * @throws Exception
     */
    private void internalStart(BundleContext bundleContext)
            throws Exception {
        this.bundleContext = bundleContext;
        boolean start = false;
        try {
            start(bundleContext);
            start = true;
        } finally {
            if (!start && (this.bundleContext == bundleContext))
                this.bundleContext = null;
        }
    }

    /**
     * Stops this osgi activity.
     *
     * @param bundleContext the osgi <code>BundleContext</code>
     *
     * @throws Exception
     */
    private void internalStop(BundleContext bundleContext)
            throws Exception {
        if (this.bundleContext != null) {
            if (bundleContext == null)
                bundleContext = this.bundleContext;
            if (this.bundleContext == bundleContext)
                this.bundleContext = null;
            stop(bundleContext);
        }
    }

    protected void start(BundleContext bundleContext)
            throws Exception {
        // Starts children OSGI fragments.
        for (OSGiUiPart OSGiSlice : OSGiSlices) {
            OSGiSlice.start(bundleContext);
        }
        // If OSGi has just started and we're on UI thread check for crash event. We must be on
        // UIThread to show the dialog and it makes no sense to show it from the background, so
        // it will be eventually displayed from .onActive()
        if (EventRunner.current() == EventRunner.getMainEventRunner()) {
            checkForSendLogsDialog();
        }
    }

    protected void stop(BundleContext bundleContext)
            throws Exception {
        // Stops children OSGI fragments.
        for (OSGiUiPart OSGiSlice : OSGiSlices) {
            OSGiSlice.stop(bundleContext);
        }
    }

    /**
     * Registers child <code>OSGiUiPart</code> to be notified on startup.
     *
     * @param fragment child <code>OSGiUiPart</code> contained in this <code>Ability</code>.
     */
    public void registerOSGiSlice(OSGiUiPart fragment) {
        OSGiSlices.add(fragment);

        if (bundleContext != null) {
            // If context exists it means we have started already, so start the fragment immediately
            try {
                fragment.start(bundleContext);
            } catch (Exception e) {
                Timber.e(e, "Error starting OSGiSlice");
            }
        }
    }

    /**
     * Unregisters child <code>OSGiUiPart</code>.
     *
     * @param fragment the <code>OSGiUiPart</code> that will be unregistered.
     */
    public void unregisterOSGiSlice(OSGiUiPart fragment) {
        if (bundleContext != null) {
            try {
                fragment.stop(bundleContext);
            } catch (Exception e) {
                Timber.e(e, "Error while trying to stop OSGiSlice");
            }
        }
        OSGiSlices.remove(fragment);
    }

    /**
     * Start the application notification settings page
     */
    public void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.setParam(Settings.EXTRA_APP_PACKAGE, getAbilityPackage());
        startAbility(intent);
    }

    /**
     * Handler for home navigator. Use upIntent if parentActivityName defined. Otherwise execute onBackKeyPressed.
     * Account setting must back to its previous menu (BackKey) to properly save changes
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getId() == ResourceTable.Id_home) {
            Intent upIntent = NavUtils.getParentActivityIntent(this);
            if (upIntent != null) {
                Timber.w("Process UpIntent for: %s", this.getLocalClassName());
                NavUtils.navigateUpTo(this, upIntent);
            }
            else {
                Timber.w("Replace Up with BackKeyPress for: %s", this.getLocalClassName());
                super.onBackPressed();
                // Class<?> homeActivity = aTalkApp.getHomeScreenActivityClass();
                // if (!this.getClass().equals(homeActivity)) {
                //    switchActivity(homeActivity);
                // }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Returns OSGI <code>BundleContext</code>.
     *
     * @return OSGI <code>BundleContext</code>.
     */
    protected BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Returns the content <code>Component</code>.
     *
     * @return the content <code>Component</code>.
     */
    protected Component getContentView() {
        return findComponentById(ResourceTable.Id_content);
    }

    /**
     * Checks if the OSGi is started and if not eventually triggers <code>LauncherActivity</code>
     * that will restore current activity from its <code>Intent</code>.
     *
     * @return <code>true</code> if restore <code>Intent</code> has been posted.
     */
    protected boolean postRestoreIntent() {
        // Restore after OSGi startup
        if (AppGUIActivator.bundleContext == null) {
            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withBundleName(getBundleName())
                    .withAbilityName(OSGiService.class)
                    .build();
            intent.setParam(LauncherAbility.ARG_RESTORE_INTENT, getIntent());
            startAbility(intent);
            terminateAbility();
            return true;
        }
        return false;
    }
    private void clearReferences() {
        Ability currentActivity = aTalkApp.getCurrentAbility();
        if (currentActivity != null && currentActivity.equals(this))
            aTalkApp.setCurrentAbility(null);
    }
}
