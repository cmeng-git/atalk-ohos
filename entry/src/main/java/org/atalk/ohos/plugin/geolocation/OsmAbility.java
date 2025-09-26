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

import ohos.aafwk.content.Intent;
import ohos.location.Location;
import ohos.net.ConnectionProperties;
import ohos.net.NetHandle;
import ohos.net.NetManager;
import ohos.net.NetStatusCallback;
import ohos.utils.PacMap;

import java.util.ArrayList;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.service.httputil.OkHttpUtils;

import timber.log.Timber;

/**
 * Default osMap view activity ported from osmdroid.
 * Created by plusminus on 00:23:14 - 03.10.2008
 *
 * @author Eng Chong Meng
 */
public class OsmAbility extends BaseAbility {
    private static final String MAP_FRAGMENT_TAG = "org.osmdroid.MAP_FRAGMENT_TAG";
    private OsmSlice osmSlice;
    private Location mLocation;
    private ArrayList<Location> mLocations = null;

    private int mLocationFetchMode = GeoConstants.ZERO_FIX;

    /**
     * The idea behind that is to force a MapView refresh when switching from offline to online.
     * If you don't do that, the map may display - when online - approximated tiles
     * - that were computed when offline
     * - that could be replaced by downloaded tiles
     * - but as the display is not refreshed there's no try to get better tiles
     *
     * @since 6.0
     */
     NetStatusCallback networkCallback = new NetStatusCallback() {
        @Override
        public void onConnectionPropertiesChanged(NetHandle handle, ConnectionProperties connectionProperties) {
            try {
                osmSlice.invalidateMapView();
            } catch (NullPointerException e) {
                // lazy handling of an improbable NPE
                Timber.e("Network callback exception: %s", e.getMessage());
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        this.setUIContent(ResourceTable.Layout_osm_map_main);

        Configuration.getInstance().setUserAgentValue(OkHttpUtils.getUserAgent());

        // noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        NetManager netManager = NetManager.getInstance(getContext());
        netManager.addDefaultNetStatusCallback(networkCallback);

//        MatchingSkills filters = new MatchingSkills();
//        filters.addEvent(WifiEvents.EVENT_CONN_STATE);
//        CommonEventSubscribeInfo subscribeInfo = new CommonEventSubscribeInfo(filters);
//        subscribeInfo.setPriority(100);
//        NetworkSubscriber subscriber = new NetworkSubscriber(subscribeInfo);
//        try {
//            CommonEventManager.subscribeCommonEvent(subscriber);
//        } catch (RemoteException e) {
//            Timber.w("Subcribe to network event failed %s", e.getMessage());
//        }


        if (mInState == null) {
            mLocationFetchMode = intent.getIntParam(GeoIntentKey.LOCATION_FETCH_MODE, GeoConstants.FOLLOW_ME_FIX);
            mLocation = intent.getSerializableParam(GeoIntentKey.LOCATION);
            mLocations = intent.getSequenceableArrayListParam(GeoIntentKey.LOCATION_LIST);
        }
        else {
            mLocationFetchMode = mInState.getInt(GeoIntentKey.LOCATION_FETCH_MODE, GeoConstants.FOLLOW_ME_FIX);
            mLocation = mInState.getParcelable(GeoIntentKey.LOCATION);
            mLocations = mInState.getParcelableArrayList(GeoIntentKey.LOCATION_LIST);
        }

        FragmentManager fm = getSupportFragmentManager();
        osmSlice = (OsmSlice) fm.findFragmentByTag(MAP_FRAGMENT_TAG);
        if (osmSlice == null) {
            osmSlice = new OsmSlice();
            PacMap args = new PacMap();
            //args.putInt(GeoIntentKey.LOCATION_FETCH_MODE, mLocationFetchMode);
            //args.putParcelable(GeoIntentKey.LOCATION, mLocation);
            //args.putParcelableArrayList(GeoIntentKey.LOCATION_LIST, mLocations);
            args.putIntValue(GeoIntentKey.LOCATION_FETCH_MODE, mLocationFetchMode);
            args.putObjectValue(GeoIntentKey.LOCATION, mLocation);
            args.putObjectValue(GeoIntentKey.LOCATION_LIST, mLocations);
            osmSlice.setArguments(args);
            fm.beginTransaction().add(ResourceTable.Id_map_container, osmSlice, MAP_FRAGMENT_TAG).commit();
        }
    }

    @Override
    protected void onActive() {
        super.onActive();
        aTalkApp.setCurrentAbility(this);
    }

    @Override
    public void onSaveAbilityState(PacMap outState) {
        super.onSaveAbilityState(outState);
        outState.putLongValue(GeoIntentKey.LOCATION_FETCH_MODE, mLocationFetchMode);
        outState.putObjectValue(GeoIntentKey.LOCATION, mLocation);
        outState.putObjectValue(GeoIntentKey.LOCATION_LIST, mLocations);
    }

    @Override
    public boolean onSupportNavigateUp() {
        terminateAbility();
        return true;
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkReceiver);
        super.onStop();
    }

    /**
     * Move the marker to the new Location location on the street map view
     *
     * @param location the new location to animate to
     */
    public void showLocation(Location location) {
        if (osmSlice == null) {
            osmSlice = (OsmSlice) getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);
        }

        if (osmSlice != null) {
            osmSlice.showLocation(location);
        }
    }
}
