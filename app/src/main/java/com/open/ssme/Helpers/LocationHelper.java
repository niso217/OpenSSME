package com.open.ssme.Helpers;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.open.ssme.Utils.Constants;
import com.open.ssme.Common.GoogleConnection;
import com.open.ssme.Common.State;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;


import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.LOCATION_SERVICE;
import static com.open.ssme.Utils.Constants.DEFAULT_CHECK_WIFI_TASK;
import static com.open.ssme.Utils.Constants.DEFAULT_LOCATION_INTERVAL;
import static com.open.ssme.Utils.Constants.LOCATION_UPDATE_TIME_OUT;
import static com.open.ssme.Utils.Constants.PROVIDERS_CHANGED;

/**
 * Created by nirb on 31/01/2017.
 */


public class LocationHelper extends BroadcastReceiver implements LocationListener {

    private GoogleConnection mGoogleConnection;
    private final String TAG = LocationHelper.class.getSimpleName();
    private Context mContext;
    private boolean mIsLocationUpdatesOn;
    private boolean mIsSingleLocationUpdatesOn;
    public boolean mIsGPSOn;
    private LocationRequest mLocationRequest;
    private Handler mLocationHandler;
    private LocationManager mLocationManager;
    private Location mCurrentLocation;
    private LatLng mCurrentLatLng;
    private double mCurrentSpeed;
    private double mCurrentLongitude;
    private double mCurrentLatitude;
    private double mCurrentAccuracy;

    public LocationHelper(Context context) {

        this.mContext = context;

        mGoogleConnection = GoogleConnection.getInstance(context);
        setupLocationRequestBalanced(DEFAULT_LOCATION_INTERVAL);
        InitLocationManager();
        StartLocationUpdates();
        mIsGPSOn = IsGpsActive();
    }

    public LocationHelper() {

    }


    private void reCheckLocation(final long ETA) {
        mLocationHandler = new Handler();

        mLocationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SingeLocationRequest();
                mLocationHandler.postDelayed(this, ETA);
                Log.d(TAG, "=====ReCheck Location In " + ETA / 1000 + " Seconds=====");
            }
        }, ETA);

        Log.d(TAG, "=====ReCheck Location In " + ETA / 1000 + " Seconds=====");

    }


    @Override
    public void onLocationChanged(Location location) {
            SetLocationData(location);
    }


    private void InitLocationManager() {
        mLocationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
    }

    private boolean IsGpsActive() {
        return mLocationManager != null ? mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) : false;
    }


    private void StartLocationUpdates() {
        if (mGoogleConnection != null && mGoogleConnection.getGoogleApiClient().isConnected()) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleConnection.getGoogleApiClient(), mLocationRequest, this).setResultCallback(new ResultCallbacks<Status>() {
                @Override
                public void onSuccess(Status status) {
                    mIsLocationUpdatesOn = true;
                    Log.d(TAG, "Location Updates Is " + mIsLocationUpdatesOn);

                }

                @Override
                public void onFailure(Status status) {
                    mIsLocationUpdatesOn = false;
                    Log.d(TAG, "Location Updates Is " + mIsLocationUpdatesOn);
                }

            });
        }
    }

    public void StopLocationUpdates() {
        if (mGoogleConnection != null && mGoogleConnection.getGoogleApiClient().isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleConnection.getGoogleApiClient(), this).setResultCallback(new ResultCallbacks<Status>() {
                @Override
                public void onSuccess(Status status) {
                    mIsLocationUpdatesOn = false;
                    Log.d(TAG, "Location Updates Is " + mIsLocationUpdatesOn);

                }

                @Override
                public void onFailure(Status status) {
                    mIsLocationUpdatesOn = true;
                    Log.d(TAG, "Location Updates Is " + mIsLocationUpdatesOn);

                }
            });
        }
    }

    public void SingeLocationRequest() {
        SingleShotLocationProvider.getSingleUpdate(mContext, LOCATION_UPDATE_TIME_OUT,
                new SingleShotLocationProvider.LocationCallback() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (location != null) {
                            mIsSingleLocationUpdatesOn = true;
                            SetLocationData(location);
                            Log.d(TAG, "=====Successful Single Location Request!!!!===== ");
                        }
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {

                    }

                    @Override
                    public void onProviderEnabled(String s) {

                    }

                    @Override
                    public void onProviderDisabled(String s) {

                    }

                    @Override
                    public void timedOut() {
                        mIsSingleLocationUpdatesOn = true;
                        Log.d(TAG, "=====Single Location Request TimeOut After " + LOCATION_UPDATE_TIME_OUT / 1000 + " Seconds=====");
                    }

                    @Override
                    public void WifiOn() {
                        Log.d(TAG, "=====Wifi Is On Stopping Location Updates=====");
                    }
                });
        Log.d(TAG, "=====Single Location Request===== ");

    }


    public void setupLocationRequestBalanced(long Interval) {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(Interval)
                .setFastestInterval(Interval);
    }

    public void ChangeLocationRequest(long ETA, Constants.LocationType type) {
        Log.d(TAG, "=====Change Location Request To " + ETA / 1000 + " Seconds, With " + type.toString() + " Method=====");

        if (mGoogleConnection.getGoogleApiClient().isConnected()) {

            if (type == Constants.LocationType.SINGLE_UPDATE) {
                StopAllLocationServices();
                reCheckLocation(ETA);

            } else {
                StopAllLocationServices();
                setupLocationRequestBalanced(ETA);
                StartLocationUpdates();
            }
        }
    }


    private void SetLocationData(Location location) {
        Log.d(TAG, "=====Location Changed " + getLocationType() + "=====");
        mCurrentLocation = location;
        mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        mCurrentLatitude = location.getLatitude();
        mCurrentLongitude = location.getLongitude();
        if (location.hasSpeed())
            mCurrentSpeed = location.getSpeed();
        if (location.hasAccuracy())
            mCurrentAccuracy = location.getAccuracy();

        LocationBroadcast();

    }


    private void LocationBroadcast() {
        Intent intent = new Intent(Constants.LOCATION_SERVICE);
        intent.addFlags(Constants.LOCATION_UPDATE_FLAG);
        intent.putExtra(Constants.LOCATION, getLocation());
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private void GPSChangedBroadcast() {
        Intent intent = new Intent(PROVIDERS_CHANGED);
        intent.putExtra(Constants.GPS_STATUS, mIsGPSOn);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    public void removeLocationHandlerCallbacks() {
        if (mLocationHandler != null) {
            mLocationHandler.removeCallbacksAndMessages(null);
            mLocationHandler = null;
            mIsSingleLocationUpdatesOn = false;
        }
    }

    private void StopAllLocationServices() {
        removeLocationHandlerCallbacks();
        StopLocationUpdates();
    }

    private String getLocationType() {
        return isLocationUpdatesOn() == true ? Constants.LocationType.LOCATION_UPDATE.name() : Constants.LocationType.SINGLE_UPDATE.name();
    }

    public void destroy() {
        Log.d(TAG, "=====Kill Location Helper=====");
        StopAllLocationServices();

    }

    public boolean isLocationUpdatesOn() {
        return mIsLocationUpdatesOn;
    }

    public Location getLocation() {
        return mCurrentLocation;
    }

    public LatLng getLatLng() {
        return mCurrentLatLng;
    }

    public double getSpeed() {
        return mCurrentSpeed;
    }


    public double getLongitude() {
        return mCurrentLongitude;
    }

    public double getLatitude() {
        return mCurrentLatitude;
    }

    public double getAccuracy() {
        return mCurrentAccuracy;
    }


    public boolean isSingleLocationUpdatesOn() {
        return mIsSingleLocationUpdatesOn;
    }

    public GoogleConnection getGoogleConnection() {
        return mGoogleConnection;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches(PROVIDERS_CHANGED)) {
            mIsGPSOn = IsGpsActive();
            GPSChangedBroadcast();
        }
    }


}
