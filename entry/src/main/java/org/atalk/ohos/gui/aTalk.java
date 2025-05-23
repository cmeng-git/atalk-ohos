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

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.PageSlider;
import ohos.agp.components.PageSliderProvider;
import ohos.app.Context;
import ohos.bundle.ElementName;
import ohos.bundle.IBundleManager;
import ohos.rpc.RemoteException;
import ohos.security.SystemPermission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.actionbar.ActionBarStatusSlice;
import org.atalk.ohos.gui.call.CallHistorySlice;
import org.atalk.ohos.gui.chat.chatsession.ChatSessionSlice;
import org.atalk.ohos.gui.chatroomslist.ChatRoomListSlice;
import org.atalk.ohos.gui.contactlist.ContactListSlice;
import org.atalk.ohos.gui.menu.MainMenuAbility;
import org.atalk.ohos.gui.webview.WebViewSlice;
import org.atalk.util.ChangeLog;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;

import timber.log.Timber;

/**
 * The main <code>Ability</code> for aTalk application with pager slider for both contact and chatRoom list windows.
 *
 * @author Eng Chong Meng
 */
public class aTalk extends MainMenuAbility {
    /**
     * A map reference to find the FragmentPagerAdapter's fragmentTag (String) by a given position (Integer)
     */
    private static final Map<Integer, AbilitySlice> mFragmentTags = new HashMap<>();

    public final static int CL_FRAGMENT = 0;
    public final static int CRL_FRAGMENT = 1;
    public final static int CHAT_SESSION_FRAGMENT = 2;
    public final static int CALL_HISTORY_FRAGMENT = 3;

    // android Permission Request Code
    public static final int PRC_CAMERA = 2000;
    public static final int PRC_GET_CONTACTS = 2001;
    public static final int PRC_RECORD_AUDIO = 2002;
    public static final int PRC_WRITE_EXTERNAL_STORAGE = 2003;

    public final static int Theme_Change = 1;
    public final static int Locale_Change = 2;
    public static int mPrefChange = 0;
    private static final ArrayList<aTalk> mInstances = new ArrayList<>();

    /**
     * The number of pages (wizard steps) to show.
     */
    private static final int NUM_PAGES = 5;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private PageSlider mPager;

    /**
     * Called when the activity is starting. Initializes the corresponding call interface.
     *
     * @param intent <code>Ability</code> <code>Intent</code>.
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);

        // Checks if OSGi has been started and if not starts LauncherAbility which will restore this Ability from its Intent.
        if (postRestoreIntent()) {
            return;
        }

        setUIContent(ResourceTable.Layout_main_view);
        if (savedInstanceState == null) {
            // Inserts ActionBar functionality
            // getSupportFragmentManager().beginTransaction().add(new ActionBarStatusSlice(), "action_bar").commit();
            setMainRoute(ActionBarStatusSlice.class.getCanonicalName());
        }

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findComponentById(ResourceTable.Id_mainPageSlider);
        // The pager adapter, which provides the pages to the view pager widget.
        // mFragmentManager = getSupportFragmentManager();
        PageSliderProvider mPagerAdapter = new MainPagerProvider();
        mPager.setProvider(mPagerAdapter);
        mPager.setReboundEffect(true);
        // mPager.setComponentTransition(true, new DepthPageTransformer());

        handleIntent(intent);

        // allow 15 seconds for first launch login to complete before showing history log if the activity is still active
        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            uiHandler.postTask(() -> {
                if (!isTerminating()) {
                    cl.getLogDialog().show();
                }
            }, 15000);
        }
    }

    /**
     * Called when new <code>Intent</code> is received(this <code>Ability</code> is launched in <code>singleTask</code> mode.
     *
     * @param intent new <code>Intent</code> data.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Decides what should be displayed based on supplied <code>Intent</code> and instance state.
     *
     * @param intent <code>Ability</code> <code>Intent</code>.
     */
    private void handleIntent(Intent intent) {
        mInstances.add(this);

        String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            String query = intent.getStringParam(SearchBar.QUERY);
            Timber.w("Search intent not handled for query: %s", query);
        }
        // Start aTalk with contactList UI for IM setup
        if (BaseAbility.ACTION_SEND_TO.equals(action)) {
            mPager.setCurrentPage(0);
        }
    }

    @Override
    protected void onActive() {
        super.onActive();
        /*
         * Need to restart whole app to make aTalkApp Locale change working
         * Note: Start aTalk Ability does not apply to aTalkApp Application class.
         */
        if (mPrefChange >= Locale_Change) {
            IBundleManager pm = getBundleManager();
            try {
                Intent intent = pm.getLaunchIntentForBundle(getBundleName());
                // ProcessPhoenix.triggerRebirth(this, intent);
                ElementName componentName = intent.getElement();
                Intent mainIntent = Intent.makeRestartAbilityMission(componentName);
                startAbility(mainIntent);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            Runtime.getRuntime().exit(0);
        }

        // Re-init aTalk to refresh the newly user selected language and theme;
        // else the main option menu is not updated
        else if (mPrefChange == Theme_Change) {
            mPrefChange = 0;
            terminateAbility();

            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withBundleName(getBundleName())
                    .withAbilityName(aTalk.class)
                    .build();
            intent.setOperation(operation);
            startAbility(intent, 0);
        }
    }

    /*
     * If the user is currently looking at the first page, allow the system to handle the
     * Back button. If Telephony fragment is shown, backKey closes the fragment only.
     * The call terminateAbility() on this activity and pops the back stack.
     */
    @Override
    public void onBackPressed() {
        if (mPager.getCurrentPage() == 0) {
            // mTelephony is not null if Telephony is closed by Cancel button.
            if (mTelephony != null) {
                if (!mTelephony.closeFragment()) {
                    terminateAbility();
                }
                mTelephony = null;
            }
            else {
                terminateAbility();
            }
        }
        else {
            // Otherwise, select the previous page.
            mPager.setCurrentPage(mPager.getCurrentPage() - 1);
        }
    }

    /**
     * Called when an activity is destroyed.
     */
    @Override
    protected void onStop() {
        super.onStop();

        synchronized (this) {
            BundleContext bundleContext = getBundleContext();
            if (bundleContext != null) {
                try {
                    stop(bundleContext);
                } catch (Throwable t) {
                    Timber.e(t, "Error stopping application:%s", t.getLocalizedMessage());
                    if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                }
            }
        }
    }

    public static void setPrefChange(int change) {
        if (Locale_Change == change)
            aTalkApp.showToastMessage(ResourceTable.String_settings_restart_require);

        mPrefChange |= change;
    }

    /**
     * A simple pager adapter that represents 3 Screen Slide PageFragment objects, in sequence.
     */
    private static class MainPagerProvider extends PageSliderProvider {
        @Override
        public AbilitySlice createPageInContainer(ComponentContainer componentContainer, int position) {
            AbilitySlice mSlice;

            switch (position) {
                case CL_FRAGMENT:
                    mSlice = new ContactListSlice(aTalk.getInstance());

                case CRL_FRAGMENT:
                    mSlice = new ChatRoomListSlice();

                case CHAT_SESSION_FRAGMENT:
                    mSlice = new ChatSessionSlice();

                case CALL_HISTORY_FRAGMENT:
                    mSlice = new CallHistorySlice();

                default: // if (position == WP_FRAGMENT){
                    mSlice = new WebViewSlice();
            }

            mFragmentTags.put(position, mSlice);
            return mSlice;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public void destroyPageFromContainer(ComponentContainer componentContainer, int i, Object o) {
        }

        @Override
        public boolean isPageMatchToObject(Component component, Object o) {
            return component.getClass().equals(o.getClass());
        }
    }

    /**
     * Get the fragment reference for the given position in pager
     *
     * @param position position in the mFragmentTags
     *
     * @return the requested fragment for the specified position or null
     */
    public static AbilitySlice getFragment(int position) {
        return mFragmentTags.get(position);
    }

    public static aTalk getInstance() {
        return mInstances.isEmpty() ? null : mInstances.get(0);
    }

    // =========== Runtime permission handlers ==========

    /**
     * Check the WRITE_EXTERNAL_STORAGE state; proceed to request for permission if requestPermission == true.
     * Require to support WRITE_EXTERNAL_STORAGE pending aTalk installed API version.
     *
     * @param callBack the requester activity to receive onRequestPermissionsResult()
     * @param requestPermission Proceed to request for the permission if was denied; check only if false
     *
     * @return the current WRITE_EXTERNAL_STORAGE permission state
     */
    public static boolean hasWriteStoragePermission(Context callBack, boolean requestPermission) {
        return hasPermission(callBack, requestPermission, PRC_WRITE_EXTERNAL_STORAGE,
                SystemPermission.WRITE_USER_STORAGE);
    }

    public static boolean hasPermission(Context callBack, boolean requestPermission, int requestCode, String permission) {
        // Timber.d(new Exception(),"Callback: %s => %s (%s)", callBack, permission, requestPermission);
        if (callBack.verifySelfPermission(permission) != IBundleManager.PERMISSION_GRANTED) {
            if (requestPermission) {
                if (callBack.canRequestPermission(permission)) {
                    callBack.requestPermissionsFromUser(new String[]{permission}, requestCode);
                } else {
                    showHintMessage(requestCode, permission);
                }
            }
            return false;
        }
        return true;
    }

    // ========== Media call resource permission requests ==========
    public static boolean isMediaCallAllowed(boolean isVideoCall) {
        // Check for resource permission before continue
        if (hasPermission(getInstance(), true, PRC_RECORD_AUDIO, SystemPermission.MICROPHONE)) {
            return !isVideoCall || hasPermission(getInstance(), true, PRC_CAMERA, SystemPermission.CAMERA);
        }
        return false;
    }

    public static void showHintMessage(int requestCode, String permission) {
        if (requestCode == PRC_RECORD_AUDIO) {
            aTalkApp.showToastMessage(ResourceTable.String_mic_permission_denied_feedback);
        }
        else if (requestCode == PRC_CAMERA) {
            aTalkApp.showToastMessage(ResourceTable.String_camera_permission_denied_feedback);
        }
        else {
            aTalkApp.showToastMessage(aTalkApp.getResString(ResourceTable.String_permission_rationale_title) + ": " + permission);
        }
    }

    @Override
    public void onRequestPermissionsFromUserResult(int requestCode, @NotNull String[] permissions,
            @NotNull int[] grantResults) {
        Timber.d("onRequestPermissionsFromUserResult: %s => %s", requestCode, permissions);
        super.onRequestPermissionsFromUserResult(requestCode, permissions, grantResults);
        if (requestCode == PRC_RECORD_AUDIO) {
            if ((grantResults.length != 0) && (IBundleManager.PERMISSION_GRANTED != grantResults[0])) {
                aTalkApp.showToastMessage(ResourceTable.String_mic_permission_denied_feedback);
            }
        } else if (requestCode == PRC_CAMERA) {
            if ((grantResults.length != 0) && (IBundleManager.PERMISSION_GRANTED != grantResults[0])) {
                aTalkApp.showToastMessage(ResourceTable.String_camera_permission_denied_feedback);
            }
        }
    }
}
