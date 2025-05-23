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
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Text;
import ohos.app.Context;
import ohos.data.DatabaseHelper;
import ohos.data.preferences.Preferences;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.utils.PacMap;

import org.atalk.ohos.agp.components.MenuInflater;
import org.atalk.ohos.gui.actionbar.ActionBarUtil;
import org.atalk.ohos.gui.util.LocaleHelper;
import org.atalk.ohos.gui.util.ThemeHelper;

/**
 * BaseAbility implements the support of user set Theme and locale.
 * All app activities must extend BaseAbility inorder to support Theme and locale.
 */
public class BaseAbility extends Ability {
    public static final String ACTION_SEND = "ability.intent.SEND";
    public static final String ACTION_SEND_TO = "ability.intent.SEND_TO";
    public static final String ACTION_SEND_MULTIPLE = "ability.intent.SEND_MULTIPLE";
    public static final String EXTRA_EMAIL = "ability.intent.EXTRA_EMAIL";
    public static final String EXTRA_SUBJECT = "ability.intent.EXTRA_SUBJECT";
    public static final String EXTRA_TEXT = "ability.intent.EXTRA_TEXT";
    public static final String EXTRA_TEXT2 = "ability.intent.EXTRA_TEXT2";
    public static final String EXTRA_STREAM = "ability.intent.EXTRA_STREAM";

    public final static int RESULT_OK = -1;
    /**
     * The EXIT action name that is broadcast to all OSGiActivities
     */
    protected static final String ACTION_EXIT = "org.atalk.ohos.exit";

    /**
     * UI thread handler used to call all operations that access data model.
     * This guarantees that it is accessed from the main thread.
     */
    public final static EventHandler uiHandler = new EventHandler(EventRunner.getMainEventRunner());

    public PacMap savedInstanceState;

    /**
     * Override Ability#onStart() to support Theme setting
     * Must setTheme() before super.onStart(intent), otherwise user selected Theme is not working
     */
    @Override
    protected void onStart(Intent intent) {
        // Always call setTheme() method in base class and before super.onStart(intent)
        ThemeHelper.setTheme(this);
        super.onStart(intent);
        configureToolBar();
        // Registers exit action ExitActionListener
        addActionRoute(ACTION_EXIT, ExitActionListener.class.getName());
    }

    /**
     * Override Ability#attachBaseContext() to support Locale setting.
     * Language value is initialized in Application class with user selected language.
     */
    @Override
    public void attachBaseContext(Context base) {
        Context context = LocaleHelper.setLocale(base);
        super.attachBaseContext(context);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        aTalkApp.setCurrentAbility(this);
    }

    @Override
    protected void onActive() {
        super.onActive();
        aTalkApp.setCurrentAbility(this);
     }
    /**
     * Called when an activity is destroyed.
     */
    @Override
    protected void onStop() {
        super.onStop();
    }


    /**
     * Convenience method which starts a new abilityClass for given <code>abilityClass</code> class
     *
     * @param abilityClass the Ability class
     */
    protected void startAbility(Class<?> abilityClass) {
	    // Intent intent = new Intent().setElementName(getBundleName(), abilityClass);

        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(getBundleName())
                .withAbilityName(abilityClass)
                .build();
        intent.setOperation(operation);
        startAbility(intent);
        terminateAbility();
    }

    /**
     * Convenience method that switches from one activity to another.
     *
     * @param activityClass the activity class
     */
    protected void switchAbility(Class<?> activityClass) {
        startAbility(activityClass);
        terminateAbility();
    }

    /**
     * Convenience method that switches from one activity to another.
     *
     * @param abilityIntent the next activity <code>Intent</code>
     */
    protected void switchAbility(Intent abilityIntent) {
        startAbility(abilityIntent);
        terminateAbility();
    }

    protected void configureToolBar() {
        // Find the toolbar view inside the activity layout - aTalk cannot use ToolBar; has layout problems
        // Toolbar toolbar = findComponentById(ResourceTable.Id_my_toolbar);
        // if (toolbar != null)
        //   setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // mActionBar.setDisplayOptions(ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_CUSTOM );
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setCustomView(ResourceTable.Layout_action_bar);

            // Disable up arrow on home activity
            Class<?> homeActivity = aTalkApp.getHomeScreenActivityClass();
            if (this.getClass().equals(homeActivity)) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setHomeButtonEnabled(false);

                Text tv = findComponentById(ResourceTable.Id_actionBarStatus);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            }
            ActionBarUtil.setTitle(this, getTitle());
            ActionBarUtil.setAvatar(this, ResourceTable.Media_ic_icon);
        }
    }

    public static Preferences getPreferenceStore() {
        DatabaseHelper dbHelper = new DatabaseHelper(aTalkApp.getInstance());
        return dbHelper.getPreferences("org.atalk.ohos.preferences");
    }

    @Override
    public void onRestoreAbilityState(PacMap inState) {
        super.onRestoreAbilityState(inState);
        savedInstanceState = inState;
    }

    /**
     * Set preference title using android inbuilt toolbar
     *
     * @param resId preference tile resourceID
     */
    public void setMainTitle(int resId) {
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
//                    | ActionBar.DISPLAY_USE_LOGO
//                    | ActionBar.DISPLAY_SHOW_TITLE);
//
//            actionBar.setLogo(ResourceTable.Media_ic_icon);
//            actionBar.setTitle(resId);
//        }
    }

    public MenuInflater getMenuInflater() {
        return new MenuInflater(getContext());
    }

    /**
     * listener for {@link BaseAbility#ACTION_EXIT} and then finishes this <code>Ability</code>
     */
    static public class ExitActionListener extends AbilitySlice {
        @Override
        protected void onStart(Intent intent) {
            terminateAbility();
        }
    }

    /**
     * Convenience method for running code on UI thread looper.
     *
     * @param action <code>Runnable</code> action to execute on UI thread.
     */
    public static void runOnUiThread(Runnable action) {
        if (EventRunner.getMainEventRunner().isCurrentRunnerThread()) {
            action.run();
        }
        else {
            // Post action to the ui looper
            uiHandler.postTask(action);
        }
    }
}
