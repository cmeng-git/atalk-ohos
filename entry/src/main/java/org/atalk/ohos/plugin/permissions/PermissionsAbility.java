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
package org.atalk.ohos.plugin.permissions;

import java.util.LinkedList;
import java.util.List;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.bundle.IBundleManager;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;
import ohos.security.SystemPermission;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.Splash;
import org.atalk.ohos.util.LogUtil;

import timber.log.Timber;

import static ohos.bundle.IBundleManager.PERMISSION_GRANTED;

/**
 * Sample activity showing the permission request process with Dexter.
 */
public class PermissionsAbility extends BaseAbility implements Component.ClickedListener {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = PermissionsAbility.class.getSimpleName();
    protected static List<String> permissionList = new LinkedList<>();

    static {
        permissionList.add(SystemPermission.CAMERA);
        permissionList.add(SystemPermission.READ_CONTACTS);
        permissionList.add(SystemPermission.LOCATION);
        permissionList.add(SystemPermission.PLACE_CALL);
        permissionList.add(SystemPermission.MICROPHONE);
        permissionList.add(SystemPermission.ACCESS_NOTIFICATION_POLICY);
        permissionList.add(SystemPermission.POWER_OPTIMIZATION);
        permissionList.add(SystemPermission.WRITE_USER_STORAGE);
       permissionList.add(SystemPermission.READ_USER_STORAGE);
    }

    private static HiLogLabel label = new HiLogLabel(HiLog.LOG_APP, 0, TAG);

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);

        // Always request permission on first apk launch for android.M
        if (aTalkApp.permissionFirstRequest) {
            // see if we should show the splash screen and wait for it to complete before continue
            if (Splash.isFirstRun()) {
                Operation operation = new Intent.OperationBuilder()
                        .withBundleName(getBundleName())
                        .withAbilityName(Splash.class)
                        .build();
                intent.setOperation(operation);
            }

            setUIContent(ResourceTable.Layout_permissions_activity);
            Timber.i("Launching dynamic permission request for aTalk.");
            aTalkApp.permissionFirstRequest = false;
            initView();

            permissionList.removeIf(permission ->
                    verifySelfPermission(permission) == PERMISSION_GRANTED || !canRequestPermission(permission));
            if (!permissionList.isEmpty()) {
                requestPermissionsFromUser(permissionList.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            }
//            boolean permissionRequest = getPackagePermissionsStatus();
//            permissionsStatusUpdate();
//
//            if ((!permissionRequest) && !showBatteryOptimizationDialog) {
//                startLauncher();
//            }
//
//            // POST_NOTIFICATIONS is only valid for API-33 (TIRAMISU)
//            if (permissionList.contains(SystemPermission.POST_NOTIFICATIONS)) {
//                notificationsPermissionFeedbackView.setVisibility(Component.VISIBLE);
//                button_notifications.setVisibility(Component.VISIBLE);
//            } else {
//                notificationsPermissionFeedbackView.setVisibility(Component.HIDE);
//                button_notifications.setVisibility(Component.HIDE);
//            }
//
//            // handler for WRITE_EXTERNAL_STORAGE pending android API
//            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ||
//                    verifySelfPermission(this, SystemPermission.WRITE_EXTERNAL_STORAGE) == IBundleManager.PERMISSION_GRANTED) {
//                storagePermissionFeedbackView.setText(ResourceTable.String_permission_granted);
//                button_storage.setEnabled(false);
//            } else {
//                button_storage.setEnabled(true);
//            }
        } else {
            startLauncher();
        }
    }

    @Override
    public void onRequestPermissionsFromUserResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }
        for (int grantResult : grantResults) {
            if (grantResult != PERMISSION_GRANTED) {
                LogUtil.info(TAG, grantResult + " is denied , Some functions may be affected.");
            }
        }
    }

    private void startLauncher() {
        Class<?> activityClass = aTalkApp.getHomeScreenAbilityClass();
        Intent intent = new Intent();
        Operation operation =
                new Intent.OperationBuilder()
                        .withDeviceId("")
                        .withBundleName(getBundleName())
                        .withAbilityName(activityClass.getName())
                        .build();
        intent.setOperation(operation);
        startAbility(intent);
        terminateAbility();
    }

    private void initView() {
        findComponentById(ResourceTable.Id_camera_permission_button).setClickedListener(this);
        findComponentById(ResourceTable.Id_contacts_permission_button).setClickedListener(this);
        findComponentById(ResourceTable.Id_location_permission_button).setClickedListener(this);
        findComponentById(ResourceTable.Id_audio_permission_button).setClickedListener(this);
        findComponentById(ResourceTable.Id_phone_permission_button).setClickedListener(this);
        findComponentById(ResourceTable.Id_notifications_permission_button).setClickedListener(this);
        findComponentById(ResourceTable.Id_storage_permission_button).setClickedListener(this);

        findComponentById(ResourceTable.Id_all_permissions_button).setClickedListener(this);
        findComponentById(ResourceTable.Id_app_info_permissions_button).setClickedListener(this);
        findComponentById(ResourceTable.Id_button_done).setClickedListener(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startLauncher();
    }

    public void onClick(Component view) {
        Intent intent = new Intent();
        String actionName = "";
        switch (view.getId()) {

            case ResourceTable.Id_button_done:
                startLauncher();
                break;

            case ResourceTable.Id_all_permissions_button:
                actionName = "dialogMultiplePermissionsListener";
                if (!permissionList.isEmpty()) {
                    requestPermissionsFromUser(permissionList.toArray(new String[0]), PERMISSION_REQUEST_CODE);
                }
                break;

            case ResourceTable.Id_audio_permission_button:
                actionName = "audioPermissionListener";
                break;


            case ResourceTable.Id_camera_permission_button:
                actionName = "cameraPermissionListener";
                break;

            case ResourceTable.Id_contacts_permission_button:
                actionName = "contactsPermissionListener";
                break;

            case ResourceTable.Id_location_permission_button:
                actionName = "locationPermissionListener";
                break;

            case ResourceTable.Id_notifications_permission_button:
                actionName = "notificationsPermissionListener";
                break;

            case ResourceTable.Id_phone_permission_button:
                actionName = "phonePermissionListener";
                break;

            case ResourceTable.Id_storage_permission_button:
                actionName = "storagePermissionListener";
                break;

            case ResourceTable.Id_app_info_permissions_button:
                actionName = "Id_app_info_permissions_button";
//                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
//                        Uri.parse("package:" + this.getPackageName()));
//                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
//                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startAbility(myAppSettings);
                break;
        }

        if (verifySelfPermission(SystemPermission.CAMERA) != IBundleManager.PERMISSION_GRANTED) {
            HiLog.info(label,"Permission denied");
            // The application has not been granted the permission.
            if (canRequestPermission(SystemPermission.CAMERA)) {
                // Check whether permission authorization can be implemented via a dialog box (at initial request or when the user has not chosen the option of "don't ask again" after rejecting a previous request).
                requestPermissionsFromUser(
                        new String[] { SystemPermission.CAMERA }, PERMISSION_REQUEST_CODE);
            } else {
                // Display the reason why the application requests the permission and prompt the user to grant the permission.
                HiLog.info(label,"permission denied");
            }
        } else {
            // The permission has been granted.
            HiLog.info(label,"permission granted");
        }
    }

//    public void showPermissionRationale(final PermissionToken token) {
//        new DialogA.Builder(this).setTitle(ResourceTable.String_permission_rationale_title)
//                .setMessage(ResourceTable.String_permission_rationale_message)
//                .setNegativeButton(android.ResourceTable.String_cancel, (dialog, which) -> {
//                    dialog.dismiss();
//                    token.cancelPermissionRequest();
//                })
//                .setPositiveButton(android.ResourceTable.String_ok, (dialog, which) -> {
//                    dialog.dismiss();
//                    token.continuePermissionRequest();
//                })
//                .setOnDismissListener(dialog -> token.cancelPermissionRequest())
//                .show();
//    }

//    /**
//     * Update the granted permissions for the package
//     *
//     * @param permission permission view to be updated
//     */
//    public void showPermissionGranted(String permission) {
//        Text feedbackView = getFeedbackViewForPermission(permission);
//        if (feedbackView != null) {
//            feedbackView.setText(ResourceTable.String_permission_granted);
//            feedbackView.setTextColor(ContextCompat.getColor(this, ResourceTable.Color_permission_granted));
//        }
//    }
//
//    /**
//     * Update the denied permissions for the package
//     *
//     * @param permission permission view to be updated
//     */
//    public void showPermissionDenied(String permission, boolean isPermanentlyDenied) {
//        Text feedbackView = getFeedbackViewForPermission(permission);
//        if (feedbackView != null) {
//            feedbackView.setText(isPermanentlyDenied
//                    ? ResourceTable.String_permission_denied_permanently : ResourceTable.String_permission_denied);
//            feedbackView.setTextColor(ContextCompat.getColor(this, ResourceTable.Color_permission_denied));
//        }
//    }

//    /**
//     * Initialize all the permission listener required actions
//     */
//    private void createPermissionListeners() {
//        PermissionListener dialogOnDeniedPermissionListener;
//        PermissionListener feedbackViewPermissionListener = new AppPermissionListener(this);
//        MultiplePermissionsListener feedbackViewMultiplePermissionListener = new MultiplePermissionListener(this);
//
//        allPermissionsListener = new CompositeMultiplePermissionsListener(feedbackViewMultiplePermissionListener,
//                SnackbarOnAnyDeniedMultiplePermissionsListener.Builder
//                        .with(contentView, ResourceTable.String_all_permissions_denied_feedback)
//                        .withOpenSettingsButton(ResourceTable.String_permission_rationale_settings_button_text)
//                        .build());
//
//        DialogOnAnyDeniedMultiplePermissionsListener dialogOnAnyDeniedPermissionListener
//                = DialogOnAnyDeniedMultiplePermissionsListener.Builder
//                .withContext(this)
//                .withTitle(ResourceTable.String_all_permission_denied_dialog_title)
//                .withMessage(ResourceTable.String_all_permissions_denied_feedback)
//                .withButtonText(android.ResourceTable.String_ok)
//                .withIcon(ResourceTable.Media_ic_icon)
//                .build();
//        dialogMultiplePermissionsListener = new CompositeMultiplePermissionsListener(
//                feedbackViewMultiplePermissionListener, dialogOnAnyDeniedPermissionListener);
//
//        dialogOnDeniedPermissionListener = DialogOnDeniedPermissionListener.Builder
//                .withContext(this)
//                .withTitle(ResourceTable.String_audio_permission_denied_dialog_title)
//                .withMessage(ResourceTable.String_mic_permission_denied_feedback)
//                .withButtonText(android.ResourceTable.String_ok)
//                .withIcon(ResourceTable.Media_ic_icon)
//                .build();
//        audioPermissionListener = new CompositePermissionListener(feedbackViewPermissionListener,
//                dialogOnDeniedPermissionListener);
//
//        dialogOnDeniedPermissionListener = DialogOnDeniedPermissionListener.Builder
//                .withContext(this)
//                .withTitle(ResourceTable.String_camera_permission_denied_dialog_title)
//                .withMessage(ResourceTable.String_camera_permission_denied_feedback)
//                .withButtonText(android.ResourceTable.String_ok)
//                .withIcon(ResourceTable.Media_ic_icon)
//                .build();
//        cameraPermissionListener = new CompositePermissionListener(feedbackViewPermissionListener,
//                dialogOnDeniedPermissionListener);
//
//        dialogOnDeniedPermissionListener = DialogOnDeniedPermissionListener.Builder
//                .withContext(this)
//                .withTitle(ResourceTable.String_contacts_permission_denied_dialog_title)
//                .withMessage(ResourceTable.String_contacts_permission_denied_feedback)
//                .withButtonText(android.ResourceTable.String_ok)
//                .withIcon(ResourceTable.Media_ic_icon)
//                .build();
//        contactsPermissionListener = new CompositePermissionListener(feedbackViewPermissionListener,
//                dialogOnDeniedPermissionListener);
//
//        dialogOnDeniedPermissionListener = DialogOnDeniedPermissionListener.Builder
//                .withContext(this)
//                .withTitle(ResourceTable.String_location_permission_denied_dialog_title)
//                .withMessage(ResourceTable.String_location_permission_denied_feedback)
//                .withButtonText(android.ResourceTable.String_ok)
//                .withIcon(ResourceTable.Media_ic_icon)
//                .build();
//        locationPermissionListener = new CompositePermissionListener(feedbackViewPermissionListener,
//                dialogOnDeniedPermissionListener);
//
//        dialogOnDeniedPermissionListener = DialogOnDeniedPermissionListener.Builder
//                .withContext(this)
//                .withTitle(ResourceTable.String_notifications_permission_denied_dialog_title)
//                .withMessage(ResourceTable.String_notifications_permission_denied_feedback)
//                .withButtonText(android.ResourceTable.String_ok)
//                .withIcon(ResourceTable.Media_ic_icon)
//                .build();
//        notificationsPermissionListener = new CompositePermissionListener(feedbackViewPermissionListener,
//                dialogOnDeniedPermissionListener);
//
//        dialogOnDeniedPermissionListener = DialogOnDeniedPermissionListener.Builder
//                .withContext(this)
//                .withTitle(ResourceTable.String_phone_permission_denied_dialog_title)
//                .withMessage(ResourceTable.String_phone_permission_denied_feedback)
//                .withButtonText(android.ResourceTable.String_ok)
//                .withIcon(ResourceTable.Media_ic_icon)
//                .build();
//        phonePermissionListener = new CompositePermissionListener(feedbackViewPermissionListener,
//                dialogOnDeniedPermissionListener);
//
//        dialogOnDeniedPermissionListener = DialogOnDeniedPermissionListener.Builder
//                .withContext(this)
//                .withTitle(ResourceTable.String_storage_permission_denied_dialog_title)
//                .withMessage(ResourceTable.String_storage_permission_denied_feedback)
//                .withButtonText(android.ResourceTable.String_ok)
//                .withIcon(ResourceTable.Media_ic_icon)
//                .build();
//        storagePermissionListener = new CompositePermissionListener(feedbackViewPermissionListener,
//                dialogOnDeniedPermissionListener);
//
//        errorListener = new PermissionsErrorListener();
//    }

    /**
     * Get the view of the permission for update, null if view does not exist
     *
     * @param name permission name
     * @return the textView for the request permission
     */
//    private Text getFeedbackViewForPermission(String name) {
//        Text feedbackView;
//        switch (name) {
//            case SystemPermission.ACCESS_FINE_LOCATION:
//                feedbackView = locationPermissionFeedbackView;
//                break;
//            case SystemPermission.CAMERA:
//                feedbackView = cameraPermissionFeedbackView;
//                break;
//            case SystemPermission.POST_NOTIFICATIONS:
//                feedbackView = notificationsPermissionFeedbackView;
//                break;
//            case SystemPermission.READ_CONTACTS:
//                feedbackView = contactsPermissionFeedbackView;
//                break;
//            case SystemPermission.READ_PHONE_STATE:
//                feedbackView = phonePermissionFeedbackView;
//                break;
//            case SystemPermission.RECORD_AUDIO:
//                feedbackView = audioPermissionFeedbackView;
//                break;
//            case SystemPermission.WRITE_EXTERNAL_STORAGE:
//                feedbackView = storagePermissionFeedbackView;
//                break;
//            default:
//                feedbackView = null;
//        }
//        return feedbackView;
//    }
}
