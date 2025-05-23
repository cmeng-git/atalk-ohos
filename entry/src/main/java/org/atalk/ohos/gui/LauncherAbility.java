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
package org.atalk.ohos.gui;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.animation.AnimatorProperty;
import ohos.agp.animation.AnimatorScatter;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.ProgressBar;
import ohos.agp.components.Text;

import org.atalk.impl.appnotification.NotificationInit;
import org.atalk.impl.appupdate.OnlineUpdateService;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.service.osgi.OSGiAbility;
import org.atalk.service.osgi.OSGiService;
import org.osgi.framework.BundleContext;

/**
 * The splash screen fragment displays animated aTalk logo and indeterminate progress indicators.
 * <p>
 * TODO: Eventually add exit option to the launcher Currently it's not possible to cancel OSGi
 * startup. Attempt to stop service during startup is causing immediate service restart after
 * shutdown even with synchronization of onStart and OnStop commands. Maybe there is still
 * some reference to OSGI service being held at that time ?
 * <p>
 * TODO: Prevent from recreating this Ability on startup. On startup when this Ability is
 * recreated it will also destroy OSGiService which is currently not handled properly. Options
 * specified in AndroidManifest.xml should cover most cases for now:
 * android:configChanges="keyboardHidden|orientation|screenSize"
 *
 * @author Eng Chong Meng
 */
public class LauncherAbility extends OSGiAbility {
    /**
     * Argument that holds an <code>Intent</code> that will be started once OSGi startup is finished.
     */
    public static final String ARG_RESTORE_INTENT = "ARG_RESTORE_INTENT";

    /**
     * Intent instance that will be called once OSGi startup is finished.
     */
    private Intent restoreIntent;
    private ProgressBar mProgressBar;
    private AnimatorProperty myFadeInAnimation;
    private boolean startOnReboot = false;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);

        // Do not show actionBar in splash screen - OSGIAbility#setTitle();
//        if (getSupportActionBar() != null)
//            getSupportActionBar().hide();

        if (OSGiService.isShuttingDown()) {
            switchAbility(ShutdownAbility.class);
            return;
        }

        // Must initialize Notification channels before any notification is being issued.
        new NotificationInit(this);

        // Get restore Intent and display "Restoring..." label
        if (intent != null) {
            this.restoreIntent = intent.getSequenceableParam(ARG_RESTORE_INTENT);
        }

        setUIContent(ResourceTable.Layout_splash);
        Text stateText = findComponentById(ResourceTable.Id_stateInfo);
        if (restoreIntent != null)
            stateText.setText(ResourceTable.String_restoring_);

        mProgressBar = findComponentById(ResourceTable.Id_actionbar_progress);
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(ProgressBar.VISIBLE);

        // Starts fade in animation
        Image myImageView = findComponentById(ResourceTable.Id_loadingImage);
        AnimatorScatter scatter = AnimatorScatter.getInstance(getContext());
        myFadeInAnimation = (AnimatorProperty) scatter.parse(ResourceTable.Animation_fade_in);
        myFadeInAnimation.setTarget(myImageView);
        myFadeInAnimation.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Null if user re-launch aTalk while it is in shutting down cycle.
        if (mProgressBar != null)
            mProgressBar.setVisibility(Component.HIDE);
        if (myFadeInAnimation != null)
            myFadeInAnimation.end();
    }

    @Override
    protected void start(BundleContext osgiContext)
            throws Exception {
        super.start(osgiContext);
        runOnUiThread(() -> {
            if (restoreIntent != null) {
                // Starts restore intent
                startAbility(restoreIntent);
                terminateAbility();
            }
            else {
                // Perform software version update check on first launch - for debug version only
                Operation operation = new Intent.OperationBuilder()
                        .withDeviceId("")
                        .withBundleName(getBundleName())
                        .withAbilityName(OnlineUpdateService.class)
                        .build();

                Intent updateIntent = new Intent();
                updateIntent.setOperation(operation);
                updateIntent.setParam(OnlineUpdateService.ACTION, OnlineUpdateService.ACTION_AUTO_UPDATE_START);
                startAbility(updateIntent);
            }

            // Start home screen Ability
            Class<?> activityClass = aTalkApp.getHomeScreenAbilityClass();
            if (!startOnReboot || !aTalk.class.equals(activityClass)) {
                switchAbility(activityClass);
            }
            else {
                startOnReboot = false;
                terminateAbility();
            }
        });
    }
}
