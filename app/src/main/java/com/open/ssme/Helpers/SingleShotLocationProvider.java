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
    private static LocationListener standardListener;
    private static boolean locationUpdateReceived = false;
    private static boolean timedOut = false;


    public static interface LocationCallback extends android.location.LocationListener {
        public void timedOut();
        public void WifiOn();

    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService
                (Context.CONNECTIVITY_SERVICE));
        Network[] networks = connectivityManager.getAllNetworks();
        if (networks == null || networks.length == 0) {
            return false;

        } else {
            for (Network network : networks) {
                NetworkInfo info = connectivityManager.getNetworkInfo(network);
                if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (info.isAvailable() && info.isConnected()) {
                        return true;
                    }
                }

            }
        }
        return false;

    }

    public static void getSingleUpdate(final Context context, long timeOut, final LocationCallback listener) {

        final LocationManager locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        if (isWifiConnected(context)) {
            listener.WifiOn();
            if (standardListener != null)
                locationManager.removeUpdates(standardListener);
        } else {
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isGPSEnabled) {
                locationUpdateReceived = false;
                timedOut = false;

                standardListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (!timedOut) {
                            listener.onLocationChanged(location);
                            locationUpdateReceived = true;
                        }
                    }
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        if (!timedOut) {
                            listener.onStatusChanged(provider, status, extras);
                        }
                    }
                    @Override
                    public void onProviderEnabled(String provider) {
                        if (!timedOut) {
                            listener.onProviderEnabled(provider);
                        }

                    }
                    @Override
                    public void onProviderDisabled(String provider) {
                        if (!timedOut) {
                            listener.onProviderDisabled(provider);
                        }

                    }
                };

                final android.os.Handler timeoutHandler = new android.os.Handler();
                Runnable timeoutRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!locationUpdateReceived) {
                            /**
                             * Timed out
                             */
                            timedOut = true;
                            listener.timedOut();
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                                return;
                            }
                            locationManager.removeUpdates(standardListener);
                        } else {
                            /**
                             * Successfully retrieved within timeout
                             */
                        }
                    }
                };
                timeoutHandler.removeCallbacks(timeoutRunnable);
                timeoutHandler.postDelayed(timeoutRunnable, timeOut);
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, standardListener, null);

            }
        }
    }
}