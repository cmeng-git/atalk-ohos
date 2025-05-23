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

import ohos.utils.Parcel;

/**
 * Parcelable  geoLocation request with builder implementation.
 * Replace gms LocationRequest for both gms and fdroid support
 *
 * @author Eng Chong Meng
 */
public class GeoLocationRequest extends Parcel {
    final private int mLocationFetchMode;
    final boolean mAddressRequest;
    final int mUpdateMinTime;
    final int mUpdateMinDistance;
    final int mFallBackToLastLocationTime;

    public GeoLocationRequest(
            int locationFetchMode,
            boolean addressRequest,
            int updateMinTime,
            int updateMinDistance,
            int fallBackToLastLocationTime)
    {
        mLocationFetchMode = locationFetchMode;
        mAddressRequest = addressRequest;
        mUpdateMinTime = updateMinTime;
        mUpdateMinDistance = updateMinDistance;
        mFallBackToLastLocationTime = fallBackToLastLocationTime;
    }

    public int getLocationFetchMode()
    {
        return mLocationFetchMode;
    }

    public boolean getAddressRequest()
    {
        return mAddressRequest;
    }

    public int getLocationUpdateMinTime()
    {
        return mUpdateMinTime;
    }

    public int getLocationUpdateMinDistance()
    {
        return mUpdateMinDistance;
    }

    public long getFallBackToLastLocationTime()
    {
        return mFallBackToLastLocationTime;
    }

//    public static final Creator<GeoLocationRequest> CREATOR = new Creator<GeoLocationRequest>()
//    {
//        @Override
//        public GeoLocationRequest createFromParcel(Parcel in)
//        {
//            return new GeoLocationRequest(
//                    /* mLoctionFetchMode = */ in.readInt(),
//                    /* mAddressRequest= */ in.readByte() != 0,
//                    /* mUpdateMinTime= */ in.readLong(),
//                    /* mUpdateMinDistance= */ in.readFloat(),
//                    /* mFallBackToLastLocationTime= */ in.readLong()
//            );
//        }
//
//        // @Override
//        public GeoLocationRequest[] newArray(int size)
//        {
//            return new GeoLocationRequest[size];
//        }
//    };

    // @Override
    public int describeContents()
    {
        return 0;
    }

    // @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mLocationFetchMode);
        dest.writeByte((byte) (mAddressRequest ? 1 : 0));
        dest.writeLong(mUpdateMinTime);
        dest.writeFloat(mUpdateMinDistance);
        dest.writeLong(mFallBackToLastLocationTime);
    }

    public static class GeoLocationRequestBuilder
    {
        private boolean mAddressRequest = false;
        private int mLocationFetchMode;
        private int mUpdateMinTime;
        private int mUpdateMinDistance;
        private int mFallBackToLastLocationTime;

        public GeoLocationRequestBuilder setLocationFetchMode(int locationFetchMode)
        {
            mLocationFetchMode = locationFetchMode;
            return this;
        }

        public GeoLocationRequestBuilder setAddressRequest(boolean addressRequest)
        {
            mAddressRequest = addressRequest;
            return this;
        }

        public GeoLocationRequestBuilder setLocationUpdateMinTime(int minTime)
        {
            mUpdateMinTime = minTime;
            return this;
        }

        public GeoLocationRequestBuilder setLocationUpdateMinDistance(int minDistance)
        {
            mUpdateMinDistance = minDistance;
            return this;
        }

        public GeoLocationRequestBuilder setFallBackToLastLocationTime(int fallBackToLastLocationTime)
        {
            mFallBackToLastLocationTime = fallBackToLastLocationTime;
            return this;
        }

        public GeoLocationRequest build()
        {
            return new GeoLocationRequest(
                    mLocationFetchMode,
                    mAddressRequest,
                    mUpdateMinTime,
                    mUpdateMinDistance,
                    mFallBackToLastLocationTime);
        }
    }
}