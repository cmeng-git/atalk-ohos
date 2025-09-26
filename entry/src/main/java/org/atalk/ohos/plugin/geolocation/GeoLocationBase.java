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
package org.atalk.ohos.plugin.geolocation;

import java.util.Locale;

import ohos.aafwk.content.Intent;
import ohos.agp.animation.Animator;
import ohos.agp.animation.AnimatorProperty;
import ohos.agp.animation.AnimatorValue;
import ohos.agp.components.Button;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.ProgressBar;
import ohos.agp.components.Slider;
import ohos.agp.components.Text;
import ohos.location.Location;
import ohos.utils.PacMap;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;

import timber.log.Timber;

/**
 * GeoLocationBase class for updating Location info and displaying map view if desired
 *
 * @author Eng Chong Meng
 */
public class GeoLocationBase extends BaseAbility implements Component.ClickedListener,
        Slider.ValueChangedListener, GeoLocationListener {
    public static final String SHARE_ALLOW = "Share_Allow";

    private static LocationListener mCallBack;
    private static GeoLocationDelegate mGeoLocationDelegate;
    private Location mLocation = null;
    private AnimatorProperty mAnimation;
    private static boolean isGpsShare = false;
    private boolean isFollowMe;
    private boolean mShareAllow = false;
    private boolean mShowMap = false;
    protected boolean mSVP_Started;
    protected int mLocationFetchMode;

    private static int gpsMinDistance = 50;        // meters
    private static int sendTimeInterval = 60;      // seconds
    private final int gpsDistanceStep = 5;  // meters
    private final int timeIntervalStep = 10; // seconds

    private Text mLatitudeTextView;
    private Text mLongitudeTextView;
    private Text mAltitudeTextView;
    private Text mLocationAddressTextView;
    private Slider mSeekDistanceInterval;

    private Button mBtnFollowMe;
    private Checkbox mBtnGpsShare;

    private boolean mDemo = false;
    private float delta = 0; // for demo

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setMainTitle(ResourceTable.String_location);
        isFollowMe = (mGeoLocationDelegate != null);
        if (isFollowMe) {
            mGeoLocationDelegate.unSubscribe();
            mGeoLocationDelegate = null;
        }
        mGeoLocationDelegate = new GeoLocationDelegate(this, this);
        mGeoLocationDelegate.onStart(intent);

        setUIContent(ResourceTable.Layout_geo_location);
        mLatitudeTextView = findComponentById(ResourceTable.Id_latitude_textview);
        mLongitudeTextView = findComponentById(ResourceTable.Id_longitude_textview);
        mAltitudeTextView = findComponentById(ResourceTable.Id_altitude_textview);
        mLocationAddressTextView = findComponentById(ResourceTable.Id_locationAddress_textview);

        Button mBtnSingleFix = findComponentById(ResourceTable.Id_btn_single_fix);
        mBtnSingleFix.setClickedListener(this);
        mBtnFollowMe = findComponentById(ResourceTable.Id_btn_follow_me);
        mBtnFollowMe.setText(String.format(getString(ResourceTable.String_follow_me_start), gpsMinDistance, sendTimeInterval));
        mBtnFollowMe.setClickedListener(this);

        mAnimation = new AnimatorProperty(mBtnFollowMe);
        mAnimation.alphaFrom(0.0f).alpha(1.0f);
        mAnimation.setDuration(1000);
        mAnimation.setLoopedCount(AnimatorValue.INFINITE);
        mAnimation.setCurveType(Animator.CurveType.LINEAR);

        mBtnGpsShare = findComponentById(ResourceTable.Id_gps_share);
        mBtnGpsShare.setEnabled(mShareAllow);
        mBtnGpsShare.setCheckedStateChangedListener((buttonView, isChecked) -> {
            isGpsShare = isChecked;
        });
        mBtnGpsShare.setChecked(mShareAllow && isGpsShare);

        mSeekDistanceInterval = findComponentById(ResourceTable.Id_seekDistanceInterval);
        mSeekDistanceInterval.setMaxValue(100);
        mSeekDistanceInterval.setProgressValue(gpsMinDistance / gpsDistanceStep);
        mSeekDistanceInterval.setValueChangedListener(this);

        Slider seekTimeInterval = findComponentById(ResourceTable.Id_seekTimeInterval);
        seekTimeInterval.setMaxValue(100);
        int progress = (sendTimeInterval - timeIntervalStep) / timeIntervalStep;
        if (progress < 0)
            progress = 0;
        seekTimeInterval.setProgressValue(progress);
        seekTimeInterval.setValueChangedListener(this);

        // Long press for demo at 0m and 2S interval
        mBtnFollowMe.setLongClickedListener(v -> {
            mDemo = true;
            mSeekDistanceInterval.setProgressValue(0);
            sendTimeInterval = 2;
            mBtnFollowMe.simulateClick();
        });
    }

    @Override
    public void onSaveAbilityState(PacMap outState) {
        super.onSaveAbilityState(outState);
        outState.putBooleanValue(SHARE_ALLOW, mShareAllow);
    }

    public void onRestoreAbilityState(PacMap inState) {
        super.onRestoreAbilityState(inState);
        mShareAllow = inState.getBooleanValue(SHARE_ALLOW, false);
    }

    @Override
    protected void onActive() {
        super.onActive();
        aTalkApp.setCurrentAbility(this);
        mLocation = null;
        mSVP_Started = false;
        mShowMap = false;
        mDemo = false;

        if (isFollowMe) {
            updateSendButton(false);
            mAnimation.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFollowMe && (mGeoLocationDelegate != null)) {
            mGeoLocationDelegate.unSubscribe();
            mGeoLocationDelegate = null;
        }
    }

    @Override
    public void onClick(Component view) {
        switch (view.getId()) {
            case ResourceTable.Id_btn_single_fix:
                mLocationFetchMode = GeoConstants.SINGLE_FIX;
                if (isFollowMe) {
                    updateSendButton(true);
                    stopLocationUpdates();
                }
                mShowMap = true;
                GeoLocationRequest geoLocationRequest = new GeoLocationRequest.GeoLocationRequestBuilder()
                        .setLocationFetchMode(mLocationFetchMode)
                        .setAddressRequest(true)
                        .setLocationUpdateMinTime(0L)
                        .setLocationUpdateMinDistance(0.0f)
                        .setFallBackToLastLocationTime(3000)
                        .build();

                requestLocationUpdates(geoLocationRequest);
                break;

            case ResourceTable.Id_btn_follow_me:
                mLocationFetchMode = (mDemo) ? GeoConstants.ZERO_FIX : GeoConstants.FOLLOW_ME_FIX;
                if (isFollowMe) {
                    updateSendButton(true);
                    stopLocationUpdates();
                }
                else {
                    updateSendButton(false);
                    mShowMap = true;
                    geoLocationRequest = new GeoLocationRequest.GeoLocationRequestBuilder()
                            .setLocationFetchMode(mLocationFetchMode)
                            .setAddressRequest(true)
                            .setLocationUpdateMinTime(sendTimeInterval * 1000L)
                            .setLocationUpdateMinDistance(gpsMinDistance)
                            .setFallBackToLastLocationTime(sendTimeInterval * 500L)
                            .build();

                    requestLocationUpdates(geoLocationRequest);
                }
        }
    }

    private void updateSendButton(boolean followMe) {
        if (followMe) {
            isFollowMe = false;
            mBtnFollowMe.setText(getString(ResourceTable.String_follow_me_start, gpsMinDistance, sendTimeInterval));
            mAnimation.end();
            mAnimation.cancel();
        }
        else {
            isFollowMe = true;
            mBtnFollowMe.setText(getString(ResourceTable.String_follow_me_stop, gpsMinDistance, sendTimeInterval));
            mAnimation.start();
        }
    }

    public boolean isGpsShare() {
        return isFollowMe && isGpsShare;
    }

    @Override
    public void onLocationPermissionGranted() {
        showToast("Location permission granted");
    }

    @Override
    public void onLocationPermissionDenied() {
        showToast("Location permission denied");
    }

    public void onLocationReceived(Location location, String locAddress) {
        if (mDemo) {
            delta += 0.0001;
            location.setLatitude(location.getLatitude() + delta);
            location.setLongitude(location.getLongitude() - delta);
        }

        String mLatitude = String.valueOf(location.getLatitude());
        String mLongitude = String.valueOf(location.getLongitude());
        String mAltitude = String.format(Locale.US, "%.03fm", location.getAltitude());

        mLatitudeTextView.setText(mLatitude);
        mLongitudeTextView.setText(mLongitude);
        mAltitudeTextView.setText(mAltitude);
        mLocationAddressTextView.setText(locAddress);

        Timber.d("Update map needed: %s %s %s", isFollowMe, (mLocation != null) ? Location.calculateDistance(
                location.getLatitude(), location.getLongitude(), mLocation.getLatitude(), mLocation.getLongitude()) : 0, location);
        // aTalkApp.showToastMessage("on Location Received: " + ((mLocation != null) ? location.distanceTo(mLocation) : 0) + "; " + location);
        mLocation = location;

        if (mBtnGpsShare.isChecked() && (mCallBack != null)) {
            mCallBack.onResult(location, locAddress);
        }
        if (mShowMap)
            showStreetMap(location);
    }

    /**
     * To be implemented by app if show streetMap is desired after a new Location is received.
     *
     * @param location at which the pointer is place and map centered
     */
    public void showStreetMap(Location location) {
    }

    @Override
    public void onLocationReceivedNone() {
        showToast("No location received");
    }

    @Override
    public void onLocationProviderEnabled() {
        showToast("Location services are now ON");
    }

    @Override
    public void onLocationProviderDisabled() {
        showToast("Location services are still Off");
    }

    /**
     * Notification that the progress level has changed. Clients can use the fromUser parameter
     * to distinguish user-initiated changes from those that occurred programmatically.
     *
     * @param slider The r whose progress has changed
     * @param progress The current progress level. This will be in the range 0..max where max
     * was set by {@link ProgressBar#setMaxValue(int)}. (The default value for max is 100.)
     * @param fromUser True if the progress change was initiated by the user.
     */
    @Override
    public void onProgressUpdated(Slider slider, int progress, boolean fromUser) {
        if (slider == mSeekDistanceInterval)
            gpsMinDistance = progress * gpsDistanceStep;
        else {
            if (progress == 0)
                sendTimeInterval = 5;
            else
                sendTimeInterval = (progress) * timeIntervalStep;
        }

        mBtnFollowMe.setText(getString(ResourceTable.String_follow_me_start, gpsMinDistance, sendTimeInterval));
        // mBtnFollowMe.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    @Override
    public void onTouchStart(Slider slider) {
    }

    @Override
    public void onTouchEnd(Slider slider) {
        if (isFollowMe) {
            mBtnFollowMe.setText(getString(ResourceTable.String_follow_me_stop, gpsMinDistance, sendTimeInterval));
        }
        showToastMessage(ResourceTable.String_apply_new_location_setting);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public static void registeredLocationListener(LocationListener listener) {
        mCallBack = listener;
    }

    public interface LocationListener {
        void onResult(Location location, String locAddress);

    }

    protected Location getLastKnownLocation() {
        return mGeoLocationDelegate.getLastKnownLocation();
    }

    @Override
    public void onRequestPermissionsFromUserResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsFromUserResult(requestCode, permissions, grantResults);
        mGeoLocationDelegate.onRequestPermissionsFromUserResult(requestCode, grantResults);
    }

    private void requestLocationUpdates(GeoLocationRequest geoLocationRequest) {
        mGeoLocationDelegate.requestLocationUpdate(geoLocationRequest);
    }

    private void stopLocationUpdates() {
        mGeoLocationDelegate.stopLocationUpdates();
    }
}