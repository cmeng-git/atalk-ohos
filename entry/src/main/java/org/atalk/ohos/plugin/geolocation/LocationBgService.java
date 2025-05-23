/*
 * aTalk, android VoIP and Instant Messaging client
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.IntentParams;
import ohos.aafwk.content.Operation;
import ohos.event.commonevent.CommonEventData;
import ohos.event.commonevent.CommonEventManager;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.location.GeoAddress;
import ohos.location.GeoConvert;
import ohos.location.Location;
import ohos.location.Locator;
import ohos.location.LocatorCallback;
import ohos.location.RequestParam;
import ohos.rpc.RemoteException;

import timber.log.Timber;

/**
 * GeoLocation service that retrieve current location update, and broadcast to the intended receiver.
 * Use the best available Location provider on the device in onStart()
 *
 * @author Eng Chong Meng
 */
public class LocationBgService extends Ability {
    private static final long NO_FALLBACK = 0;
    private Locator mLocator;
    private LocationCallback mLocationCallback;
    private EventHandler mEventHandler;
    private int mLocationMode;
    private boolean mAddressRequest;
    private int mLocationUpdateMinTime = 0;
    private int mLocationUpdateMinDistance = 0;
    private long fallBackToLastLocationTime;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);

        mLocator = new Locator(getContext());
        mLocationCallback = new LocationCallback();
        mEventHandler = new EventHandler(EventRunner.create());
    }

    @SuppressWarnings("MissingPermission")
    @Override
    protected void onCommand(Intent intent, boolean restart, int startId) {
        super.onCommand(intent, restart, startId);

        String actionIntent = intent.getAction();
        // action not defined on gps service first startup
        if (actionIntent == null) {
            return;
        }

        Timber.d("Location background service start command %s", actionIntent);
        if (actionIntent.equals(GeoConstants.ACTION_LOCATION_FETCH_START)) {
            GeoLocationRequest geoLocationRequest = intent.getSerializableParam(GeoIntentKey.LOCATION_REQUEST);
            mLocationMode = geoLocationRequest.getLocationFetchMode();
            mAddressRequest = geoLocationRequest.getAddressRequest();
            mLocationUpdateMinTime = geoLocationRequest.getLocationUpdateMinTime();
            mLocationUpdateMinDistance = geoLocationRequest.getLocationUpdateMinDistance();
            fallBackToLastLocationTime = geoLocationRequest.getFallBackToLastLocationTime();
            requestLocationUpdates();
        }
        // Tells the system to not try to recreate the service after it has been killed.
        else if (actionIntent.equals(GeoConstants.ACTION_LOCATION_FETCH_STOP)) {
            stopLocationService();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdates() {
        Timber.i("Requesting location updates");
        startFallbackToLastLocationTimer();

        // Use higher accuracy location fix for SINGLE_FIX request
        // int quality = (GeoConstants.SINGLE_FIX == mLocationMode) ?
        //         RequestParam.PRIORITY_ACCURACY : RequestParam.PRIORITY_FAST_FIRST_FIX;
        try {
            RequestParam requestParam = new RequestParam(RequestParam.PRIORITY_FAST_FIRST_FIX);
            requestParam.setTimeInterval(mLocationUpdateMinTime);
            requestParam.setDistanceInterval(mLocationUpdateMinDistance);
            if (GeoConstants.SINGLE_FIX == mLocationMode) {
                mLocator.requestOnce(requestParam, mLocationCallback);
            }
            else {
                mLocator.startLocating(requestParam, mLocationCallback);
            }
        } catch (Exception unlikely) {
            Timber.e("Lost location permission. Could not request updates:%s", unlikely.getMessage());
        }
    }

    @SuppressWarnings("MissingPermission")
    private void startFallbackToLastLocationTimer() {
        if (fallBackToLastLocationTime != NO_FALLBACK) {
            mEventHandler.removeTask(getLastLocation);
            mEventHandler.postTask(getLastLocation, fallBackToLastLocationTime);
        }
    }

    private final Runnable getLastLocation = new Runnable() {
        @Override
        public void run() {
            Location location = mLocator.getCachedLocation();
            Timber.d("Fallback location received: %s", location);
            onLocationChanged(location);
        }
    };

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    private void stopLocationService() {
        if (mEventHandler != null)
            mEventHandler.removeTask(getLastLocation);
        mEventHandler = null;

        if (mLocator != null) {
            // Do not set mLocationManager=null, startService immediately after softSelf will not execute onStart()
            try {
                mLocator.stopLocating(mLocationCallback);
            } catch (Exception ex) {
                Timber.w("Unable to de-attach location listener: %s", ex.getMessage());
            }
        }
        // Timber.d("Stop Location Manager background service");
        terminateAbility();
    }

    private class LocationCallback implements LocatorCallback {

        @Override
        public void onLocationReport(Location location) {
            onLocationChanged(location);
        }

        @Override
        public void onStatusChanged(int i) {

        }

        @Override
        public void onErrorReport(int i) {

        }
    }

    public void onLocationChanged(Location location) {
        // Timber.d("New location received: %s", location);
        if (location != null) {
            // force to a certain location for testing
            // ocation.setLatitude(34.687274);
            // location.setLongitude(135.525453);
            // location.setAltitude(12.023f);

            GeoPreferenceUtil.getInstance(this).saveLastKnownLocation(location);
            String locAddress = null;
            if (mAddressRequest) {
                locAddress = getLocationAddress(location);
            }

            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withAction(GeoConstants.INTENT_LOCATION_RECEIVED)
                    .build();
            intent.setOperation(operation);

            // Notify anyone listening for broadcasts about the new location.
            IntentParams intentParams = new IntentParams();
            intentParams.setParam(GeoIntentKey.LOCATION, location);

            intent.setParam(GeoIntentKey.LOCATION, intentParams);
            intent.setParam(GeoIntentKey.ADDRESS, locAddress);
            CommonEventData eventData = new CommonEventData(intent);
            try {
                CommonEventManager.publishCommonEvent(eventData);
            } catch (RemoteException e) {
                Timber.w("%s", e.getMessage());
            }
        }
        else {
            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withAction(GeoConstants.INTENT_NO_LOCATION_RECEIVED)
                    .build();
            intent.setOperation(operation);
            CommonEventData eventData = new CommonEventData(intent);
            try {
                CommonEventManager.publishCommonEvent(eventData);
            } catch (RemoteException e) {
                Timber.w("%s", e.getMessage());
            }
        }
        mEventHandler.removeTask(getLastLocation);
    }

    /**
     * To get address location from coordinates
     *
     * @param loc location from which the address is being retrieved
     *
     * @return the Address
     */
    private String getLocationAddress(Location loc) {
        String locAddress = "No service available or no address found";

        GeoConvert gcd = new GeoConvert(Locale.getDefault());
        if (gcd.isGeoAvailable())
            return locAddress;

        List<GeoAddress> addresses;
        try {
            addresses = gcd.getAddressFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                GeoAddress address = addresses.get(0);
                locAddress = address.toString();
            }
        } catch (IllegalArgumentException | IOException e) {
            locAddress = e.getMessage();
            Timber.e("Get location address: %s", locAddress);
        }
        return locAddress;
    }
}