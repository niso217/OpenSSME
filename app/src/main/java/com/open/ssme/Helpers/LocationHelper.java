package com.open.ssme.Helpers;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.open.ssme.Utils.Constants;
import com.open.ssme.Common.GoogleConnection;
import com.open.ssme.Common.State;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.LOCATION_SERVICE;
import static com.open.ssme.Utils.Constants.DEFAULT_CHECK_WIFI_TASK;
import static com.open.ssme.Utils.Constants.DEFAULT_LOCATION_INTERVAL;
import static com.open.ssme.Utils.Constants.LOCATION_UPDATE_TIME_OUT;
import static com.open.ssme.Utils.Constants.NOW;
import static com.open.ssme.Utils.Constants.PROVIDERS_CHANGED;
import static com.open.ssme.Utils.Constants.UPDATE_INTERVAL;

/**
 * Created by nirb on 31/01/2017.
 */


public class LocationHelper implements Observer {

    private GoogleConnection mGoogleConnection;
    private final String TAG = LocationHelper.class.getSimpleName();
    private Context mContext;
    private boolean mIsLocationUpdatesOn;
    private LocationRequest mLocationRequest;
    private Handler mLocationHandler;
    private LocationManager mLocationManager;
    private Location mCurrentLocation;
    private String mLastLocationUpdate;
    private LatLng mCurrentLatLng;
    private double mCurrentSpeed;
    private double mCurrentLongitude;
    private double mCurrentLatitude;
    private double mCurrentAccuracy;
    private Runnable mLocationRunnable;
    private AlarmManager mAlarmManager;
    private PendingIntent mAlarmPendingIntent;



    public LocationHelper(Context context) {

        this.mContext = context;
        InitAlarmManager();
        InitRunnable();
        mLocationHandler = new Handler();
        mGoogleConnection = GoogleConnection.getInstance(context);
        ChangeLocationRequest(UPDATE_INTERVAL, NOW);
        InitLocationManager();


    }



    private void InitRunnable() {
        mLocationRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Fired Handler In : " + new SimpleDateFormat("hh:mm:ss").format(new Date()));
                StartLocationUpdates();
            }
        };
    }


    private void reCheckLocation(final long WhenToDispatch) {
        removeLocationHandlerCallbacks();
        mLocationHandler.postDelayed(mLocationRunnable, WhenToDispatch);
        Log.d(TAG, "Asked For Handler In: " + new SimpleDateFormat("hh:mm:ss").format(new Date()) +
                " Dispatch Time: " + new SimpleDateFormat("hh:mm:ss").format(new Date(System.currentTimeMillis() + WhenToDispatch)) + "=====");
    }


    private void InitAlarmManager() {
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(mContext, LocationHelper.class);
        mAlarmPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
    }

//    private void setAlarm(long time){
//
//        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                SystemClock.elapsedRealtime() +
//                        time, mAlarmPendingIntent);
//
//    }


    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult result) {
            SetLocationData(result.getLastLocation());
        }

        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            mIsLocationUpdatesOn = locationAvailability.isLocationAvailable();
            Log.d(TAG, "=====Location Availability Is: " + mIsLocationUpdatesOn + "=====");
        }

    };


    private void InitLocationManager() {
        mLocationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
    }


    private void StartLocationUpdates() {
        if (mGoogleConnection != null && mGoogleConnection.getGoogleApiClient().isConnected()) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleConnection.getGoogleApiClient(), mLocationRequest, mLocationCallback, null);
        }
    }

    public void StopLocationUpdates() {
        if (mGoogleConnection != null && mGoogleConnection.getGoogleApiClient().isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleConnection.getGoogleApiClient(), mLocationCallback);
        mIsLocationUpdatesOn = false;
        Log.d(TAG, "=====Location Availability Is: " + mIsLocationUpdatesOn + "=====");

    }


    public void setupLocationRequestBalanced(long Interval) {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(Interval)
                .setFastestInterval(Interval);
    }

    public void ChangeLocationRequest(long ETA, long WhenToDispatch) {
        Log.d(TAG, "=====Change Location Interval To " + ETA / 1000 + " Seconds, Dispatch In " + WhenToDispatch / 1000 + " Seconds=====");

        if (mGoogleConnection.getGoogleApiClient().isConnected())
            StartUpdates(ETA, WhenToDispatch);
        else
            mGoogleConnection.getGoogleApiClient().connect();

    }

    private void StartUpdates(long ETA, long WhenToDispatch) {
        StopAllLocationServices();
        setupLocationRequestBalanced(ETA);
        reCheckLocation(WhenToDispatch);
        //setAlarm(WhenToDispatch);
    }


    private void SetLocationData(Location location) {
        Log.d(TAG, "=====Location Changed In " + new SimpleDateFormat("hh:mm:ss").format(new Date()) + "=====");
        mCurrentLocation = location;
        mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        mCurrentLatitude = location.getLatitude();
        mCurrentLongitude = location.getLongitude();
        if (location.hasSpeed())
            mCurrentSpeed = location.getSpeed();
        if (location.hasAccuracy())
            mCurrentAccuracy = location.getAccuracy();

        setLastLocationUpdate(new SimpleDateFormat("hh:mm:ss").format(new Date()));

    }


    public void removeLocationHandlerCallbacks() {
        if (mLocationHandler != null) {
            mLocationHandler.removeCallbacks(mLocationRunnable);
        }
    }

    private void StopAllLocationServices() {
        removeLocationHandlerCallbacks();
        StopLocationUpdates();
    }


    public void destroy() {
        Log.d(TAG, "=====Kill Location Helper=====");
        StopAllLocationServices();
        // Disconnecting the client invalidates it.
        if (mGoogleConnection != null) {
            mGoogleConnection.disconnect();
        }

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


    public GoogleConnection getGoogleConnection() {
        return mGoogleConnection;
    }


    public String getLastLocationUpdate() {
        return mLastLocationUpdate;
    }

    public void setLastLocationUpdate(String mLastLocationUpdate) {
        this.mLastLocationUpdate = mLastLocationUpdate;
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable != mGoogleConnection) {
            return;
        }
        switch ((State) data) {

            case OPENED:
                Log.d(TAG, "Connected to Google Api Client");
                StartUpdates(UPDATE_INTERVAL, NOW);
                break;
            case CLOSED:
                Log.d(TAG, "Disconnected from Google Api Client");
                break;
        }
    }
}
