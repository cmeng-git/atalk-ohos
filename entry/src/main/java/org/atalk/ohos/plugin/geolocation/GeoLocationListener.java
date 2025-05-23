package org.atalk.ohos.plugin.geolocation;

import ohos.location.Location;

public interface GeoLocationListener
{
    void onLocationPermissionGranted();

    void onLocationPermissionDenied();

    void onLocationReceived(Location location, String locAddress);

    void onLocationReceivedNone();

    void onLocationProviderEnabled();

    void onLocationProviderDisabled();
}