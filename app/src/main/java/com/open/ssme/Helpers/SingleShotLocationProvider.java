package com.open.ssme.Helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by nirb on 09/02/2017.
 */

public class SingleShotLocationProvider {
    private static final String TAG = SingleShotLocationProvider.class.getSimpleName();
    private static LocationListener mLocationListener;

    public static interface LocationCallback {
        public void onNewLocationAvailable(Location location);
    }

    // calls back to calling thread, note this is for low grain: if you want higher precision, swap the
    // contents of the else and if. Also be sure to check gps permission/settings are allowed.
    // call usually takes <10ms
    public static void requestSingleUpdate(final Context context, final LocationCallback callback) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (isWifiConnected(context)){
            if (mLocationListener!=null)
            locationManager.removeUpdates(mLocationListener);

            callback.onNewLocationAvailable(null);

        }
        else {
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isGPSEnabled) {
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        callback.onNewLocationAvailable(new Location(LatLngToLocation(new LatLng(location.getLatitude(), location.getLongitude()))));
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                }, null);
            }
        }

    }

    private static Location LatLngToLocation(LatLng latlang) {
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        loc.setLatitude(latlang.latitude);
        loc.setLongitude(latlang.longitude);
        return loc;
    }

    private static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService
                (Context.CONNECTIVITY_SERVICE));
        Network[] networks = connectivityManager.getAllNetworks();
        if (networks == null || networks.length == 0) {
            Log.d(TAG, "=====Wifi Is Off Starting Location Updates=====");
            return false;

        } else {
            for (Network network : networks) {
                NetworkInfo info = connectivityManager.getNetworkInfo(network);
                if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (info.isAvailable() && info.isConnected()) {
                        Log.d(TAG, "=====Wifi Is On Stopping Location Updates=====");
                        return true;
                    }
                }

            }
        }
        Log.d(TAG, "=====Wifi Is Off Starting Location Updates=====");
        return false;

    }
}



