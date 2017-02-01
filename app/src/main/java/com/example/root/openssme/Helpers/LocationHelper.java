package com.example.root.openssme.Helpers;

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
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.common.GoogleConnection;
import com.example.root.openssme.common.State;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import static android.content.Context.LOCATION_SERVICE;
import static com.example.root.openssme.Utils.Constants.DEFAULT_CHECK_WIFI_TASK;
import static com.example.root.openssme.Utils.Constants.DEFAULT_LOCATION_INTERVAL;
import static com.example.root.openssme.Utils.Constants.PROVIDERS_CHANGED;

/**
 * Created by nirb on 31/01/2017.
 */


public class LocationHelper extends BroadcastReceiver implements LocationListener, Observer {

    private GoogleConnection mGoogleConnection;
    private final String TAG = LocationHelper.class.getSimpleName();
    private Context mContext;
    private boolean mIsLocationUpdatesOn;
    public  boolean mIsGPSOn;
    private LocationRequest mLocationRequest;
    private Runnable mHandlerTask;
    private Handler mHandler;
    private LocationManager mLocationManager;
    private Location mCurrentLocation;
    private LatLng mCurrentLatLng;
    private double mCurrentSpeed;
    private double mCurrentLongitude;
    private double mCurrentLatitude;
    private double mCurrentAccuracy;


    public LocationHelper(Context context) {

        this.mContext = context;

        mHandler = new Handler();

        mGoogleConnection = GoogleConnection.getInstance(context);
        mGoogleConnection.addObserver(this);

        setupLocationRequestBalanced(DEFAULT_LOCATION_INTERVAL);

        if (mGoogleConnection.getGoogleApiClient().isConnected())
            StartLocationUpdates();
        else
            connectClient();

        InitLocationManager();
        reCheckWifiConnection();
        mIsGPSOn = IsGpsActive();

    }

    public LocationHelper()
    {

    }

    private void reCheckWifiConnection() {
        mHandlerTask = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(mHandlerTask, DEFAULT_CHECK_WIFI_TASK);
                isWifiConnected();
            }
        };
        mHandlerTask.run();
    }


    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "=====Location Changed=====");
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


    @Override
    public void update(Observable observable, Object data) {
        if (mGoogleConnection != null && observable != mGoogleConnection) {
            return;
        }
        switch ((State) data) {
            case OPENED:
                Log.d(TAG, "Google Api Connected");
                StartLocationUpdates();
                break;
            case CLOSED:
                Log.d(TAG, "Google Api Disconnected");
                break;
        }
    }

    public void connectClient() {
        if (mGoogleConnection != null && !mGoogleConnection.getGoogleApiClient().isConnected()) {
            mGoogleConnection.connect();
        }
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


    public void setupLocationRequestBalanced(long Interval) {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(Interval)
                .setFastestInterval(Interval);
    }

    public void ChangeLocationRequest(long ETA) {
        Log.d(TAG, "=====Change Location Request To " + ETA + "=====");
        StopLocationUpdates();
        setupLocationRequestBalanced(ETA);
        StartLocationUpdates();
    }

    private void isWifiConnected() {
        ConnectivityManager connectivityManager = ((ConnectivityManager) mContext.getSystemService
                (Context.CONNECTIVITY_SERVICE));
        Network[] networks = connectivityManager.getAllNetworks();
        if (networks == null || networks.length == 0) {
            StartLocationUpdates();
            Log.d(TAG, "=====Wifi Is Off Starting Location Updates=====");
        } else {
            for (Network network : networks) {
                NetworkInfo info = connectivityManager.getNetworkInfo(network);
                if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (info.isAvailable() && info.isConnected() && mIsLocationUpdatesOn) {
                        StopLocationUpdates();
                        Log.d(TAG, "=====Wifi Is On Stopping Location Updates=====");
                        break;
                    }
                } else {
                    if (!mIsLocationUpdatesOn) {
                        StartLocationUpdates();
                        Log.d(TAG, "=====Wifi Is Off Starting Location Updates=====");
                    }
                }
            }
        }
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


    public void destroy() {
        Log.d(TAG, "=====Kill Location Helper=====");

        if (mGoogleConnection.getGoogleApiClient() != null) {
            mGoogleConnection.disconnect();
            mGoogleConnection.deleteObserver(this);
        }
        mHandler.removeCallbacks(mHandlerTask);
        StopLocationUpdates();

    }

    public boolean isIsLocationUpdatesOn() {
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

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches(PROVIDERS_CHANGED))
        {
            mIsGPSOn = IsGpsActive();
            GPSChangedBroadcast();
        }
    }
}
