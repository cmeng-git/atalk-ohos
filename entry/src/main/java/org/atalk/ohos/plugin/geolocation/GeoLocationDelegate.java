package org.atalk.ohos.plugin.geolocation;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.IntentParams;
import ohos.aafwk.content.Operation;
import ohos.bundle.IBundleManager;
import ohos.event.commonevent.CommonEventData;
import ohos.event.commonevent.CommonEventManager;
import ohos.event.commonevent.CommonEventSubscribeInfo;
import ohos.event.commonevent.CommonEventSubscriber;
import ohos.event.commonevent.MatchingSkills;
import ohos.location.Location;
import ohos.location.Locator;
import ohos.rpc.RemoteException;

import org.atalk.ohos.util.LogUtil;

public class GeoLocationDelegate {
    private static final String TAG = GeoLocationDelegate.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST = 100;
    private static final int ENABLE_LOCATION_SERVICES_REQUEST = 101;

    private GeoCommonEventSubscriber mEventSubscriber;
    private final GeoLocationListener mGeoLocationListener;
    private final Ability mActivity;

    private Locator mLocator;
    private GeoLocationRequest mGeoLocationRequest;

    public GeoLocationDelegate(Ability activity, GeoLocationListener geoLocationListener) {
        mActivity = activity;
        mGeoLocationListener = geoLocationListener;
    }

    public void onStart(Intent intent) {
        mLocator = new Locator(mActivity);
        subscribe();
    }

    public void onStop() {
        unSubscribe();
        stopLocationUpdates();
    }

    private void subscribe() {
        MatchingSkills filter = new MatchingSkills();
        filter.addEvent(GeoConstants.INTENT_LOCATION_RECEIVED);
        filter.addEvent(GeoConstants.INTENT_NO_LOCATION_RECEIVED);

        CommonEventSubscribeInfo subscribeInfo = new CommonEventSubscribeInfo(filter);
        mEventSubscriber = new GeoCommonEventSubscriber(subscribeInfo);
        try {
            CommonEventManager.subscribeCommonEvent(mEventSubscriber);
        } catch (RemoteException e) {
            LogUtil.error(TAG, "subscribeCommonEvent occur exception. " + e.getMessage());
        }
    }

    public void unSubscribe() {
        try {
            CommonEventManager.unsubscribeCommonEvent(mEventSubscriber);
        } catch (RemoteException e) {
            LogUtil.error(TAG, "Unsubscribe exception: " + e.getMessage());
        }
    }

    private class GeoCommonEventSubscriber extends CommonEventSubscriber {
        GeoCommonEventSubscriber(CommonEventSubscribeInfo info) {
            super(info);
        }

        @Override
        public void onReceiveEvent(CommonEventData eventData) {
            Intent intent = eventData.getIntent();
            String action = intent.getOperation().getAction();

            if (GeoConstants.INTENT_LOCATION_RECEIVED.equals(action)) {
                Location location = intent.getSerializableParam(GeoIntentKey.LOCATION);
                String locAddress = intent.getStringParam(GeoIntentKey.ADDRESS);
                mGeoLocationListener.onLocationReceived(location, locAddress);
            }
            else if (GeoConstants.INTENT_NO_LOCATION_RECEIVED.equals(action)) {
                mGeoLocationListener.onLocationReceivedNone();
            }
        }
    }

    private void startLocationBGService() {
        if (!mLocator.isLocationSwitchOn())
            mLocator.requestEnableLocation();
        else {
            Operation operation = new Intent.OperationBuilder()
                    .withBundleName(mActivity.getBundleName())
                    .withAbilityName(LocationBgService.class)
                    .build();

            IntentParams intentParams = new IntentParams();
            intentParams.setParam(GeoIntentKey.LOCATION_REQUEST, mGeoLocationRequest);

            Intent intent = new Intent();
            intent.setAction(GeoConstants.ACTION_LOCATION_FETCH_START);
            intent.setParam(GeoIntentKey.LOCATION_REQUEST, intentParams);
            intent.setOperation(operation);
            mActivity.startAbility(intent, 0);
        }
    }

    public void stopLocationUpdates() {
        Operation operation = new Intent.OperationBuilder()
                .withBundleName(mActivity.getBundleName())
                .withAbilityName(LocationBgService.class)
                .build();

        Intent intent = new Intent();
        intent.setAction(GeoConstants.ACTION_LOCATION_FETCH_STOP);
        intent.setOperation(operation);
        mActivity.startAbility(intent, 0);
    }

    public void requestLocationUpdate(GeoLocationRequest geoLocationRequest) {
        if (geoLocationRequest == null)
            throw new IllegalStateException("geoLocationRequest can't be null");

        mGeoLocationRequest = geoLocationRequest;
        checkForPermissionAndRequestLocation();
    }

    protected void checkForPermissionAndRequestLocation() {
        if (!mLocator.isLocationSwitchOn()) {
            mLocator.requestEnableLocation();
        }
        else {
            startLocationBGService();
        }
    }

    public void onRequestPermissionsFromUserResult(int requestCode, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == IBundleManager.PERMISSION_GRANTED) {
                requestLocationUpdate(mGeoLocationRequest);
                mGeoLocationListener.onLocationPermissionGranted();
            }
            else {
                mGeoLocationListener.onLocationPermissionDenied();
            }
        }
    }

    public Location getLastKnownLocation() {
        return GeoPreferenceUtil.getInstance(mActivity).getLastKnownLocation();
    }
}