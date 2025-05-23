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

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.IntentParams;
import ohos.aafwk.content.Operation;
import ohos.location.Location;

import org.atalk.ohos.aTalkApp;

/**
 * OSM class for displaying map view
 *
 * @author Eng Chong Meng
 */
public class GeoLocationAbility extends GeoLocationBase {
    private OsmAbility mSVP = null;

    @Override
    protected void onActive() {
        super.onActive();
        mSVP = null;
    }

    public void showStreetMap(Location location) {
        if (!mSVP_Started) {
            mSVP_Started = true;

            Operation operation = new Intent.OperationBuilder()
                    .withDeviceId("")
                    .withBundleName(getBundleName())
                    .withAbilityName(OsmAbility.class)
                    .build();

            Intent intent = new Intent();

            IntentParams intentParams = new IntentParams();
            intentParams.setParam(GeoIntentKey.LOCATION, location);

            intent.setParam(GeoIntentKey.LOCATION_FETCH_MODE, mLocationFetchMode);
            intent.setParam(GeoIntentKey.LOCATION, intentParams);
            intent.setOperation(operation);
            startAbility(intent);

        } else if (GeoConstants.ZERO_FIX == mLocationFetchMode) {
            if (mSVP == null) {
                Ability currentAbility = aTalkApp.getCurrentAbility();
                if (currentAbility != null) {
                    if (currentAbility instanceof OsmAbility) {
                        mSVP = (OsmAbility) currentAbility;
                    }
                }
            }
            if (mSVP != null) {
                mSVP.showLocation(location);
            }
        }
    }
}
