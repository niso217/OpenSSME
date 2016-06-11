package com.example.root.openssme;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.SocialNetwork.Settings;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


import java.util.Calendar;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;

import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.common.GoogleConnection;
import com.example.root.openssme.common.State;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class LocationService extends Service implements

        LocationListener,
        Observer,
        Response.Listener,
        Response.ErrorListener {

    protected static final String TAG = LocationService.class.getSimpleName();
    public static final String REQUEST_TAG = "MainVolleyActivity";
    public static final int NOTIFICATION_ID = 100;
    private static final String NOTIFICATION_DELETED_ACTION = "delete_notofication";


    public GoogleConnection mGoogleConnection;
    public LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private LocationManager mLocationManager;
    private RequestQueue mQueue;
    private NotificationManager mNotificationManager;
    private Handler mHandler;
    private Handler mCallHandler;
    private boolean mLocker = true;
    private boolean mIsLocationUpdatesOn = false;
    private long mNextApiUpdate = Constants.API_REFRESH_GO;
    private double mDistaceBeforeGPSUpdates = Double.MAX_VALUE;
    private boolean mIsNotificationActive;


    @Override
    public void onCreate() {


        mQueue = CustomVolleyRequestQueue.getInstance(getApplication())
                .getRequestQueue();

        mLocationManager = (LocationManager) this.getSystemService(
                Context.LOCATION_SERVICE);


        mGoogleConnection = GoogleConnection.getInstance(this);
        mGoogleConnection.addObserver(this);
        mHandler = new Handler();
        mCallHandler = new Handler();

        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        mIsNotificationActive = false;

        setupLocationRequestBalanced();


        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mGoogleConnection.getGoogleApiClient().isConnected()) {
            askForLocation();
        } else {
            connectClient();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void connectClient() {

        // Connect the client.
        if (mGoogleConnection != null && !mGoogleConnection.getGoogleApiClient().isConnected()) {
            mGoogleConnection.connect();
        }
    }

    public void setupLocationRequestBalanced() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_INTERVAL);
    }


    @Override
    public void onDestroy() {

        StopApiLocationUpdate();

        // Disconnecting the client invalidates it.
        if (mGoogleConnection != null) {
            mGoogleConnection.disconnect();
            mGoogleConnection.deleteObserver(this);
        }
        if (mQueue != null) {
            mQueue.cancelAll(REQUEST_TAG);
        }
        Log.d(TAG, "Location Distroy");

        super.onDestroy();

        unregisterReceiver(receiver);

        stopSelf();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {


            mCurrentLocation = location;

            CalcDistanc();

            IsInsideTheRadius();

            StartApiLocationUpdate();


        }
    }

    private void CalcDistanc() {

        if (mCurrentLocation != null && ListGateComplexPref.getInstance().gates.size() > 0) {

            Location to = new Location("");
            for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {
                to.setLatitude(ListGateComplexPref.getInstance().gates.get(i).location.latitude);
                to.setLongitude(ListGateComplexPref.getInstance().gates.get(i).location.longitude);
                ListGateComplexPref.getInstance().gates.get(i).distance = new Double(mCurrentLocation.distanceTo(to));

            }
            ListGateComplexPref.getInstance().sort();
        }

        if (ListGateComplexPref.getInstance().gates.get(0).distance > mDistaceBeforeGPSUpdates * 2) {
            GetCurrentLocation();
        }
    }


    private void IsInsideTheRadius() {

        if (ListGateComplexPref.getInstance().gates.get(0).distance <= Settings.getInstance().getOpen_distance()) {
            ListGateComplexPref.getInstance().gates.get(0).status = true;
            Toast.makeText(this, "Inside the Radius: Distance:" + ListGateComplexPref.getInstance().gates.get(0).distance + " ETA: " + ListGateComplexPref.getInstance().gates.get(0).ETA, Toast.LENGTH_SHORT);

        } else {
            ListGateComplexPref.getInstance().gates.get(0).status = false;
            //clear all call notification
            Toast.makeText(this, "OutSite The Radius Distance: " + ListGateComplexPref.getInstance().gates.get(0).distance + " ETA: " + ListGateComplexPref.getInstance().gates.get(0).ETA, Toast.LENGTH_SHORT);

        }


        Log.d(TAG, "Are We Inside The Radius Of The Gate? " + ListGateComplexPref.getInstance().gates.get(0).status);

    }

    public void GetCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String provider = "";
        switch (Settings.getInstance().getService_provider()) //get the provider from settings
        {
            case 1:
                provider = LocationManager.NETWORK_PROVIDER;
                break;
            case 2:
                provider = LocationManager.GPS_PROVIDER;
                break;
        }
        mLocationManager.requestSingleUpdate(provider,
                new android.location.LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        StopLocationUpdates();
                        Log.d(TAG, "New Location arrived from request Single Update From Network Provider");
                        mCurrentLocation = location;
                        ApiLocationUpdate();
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

    /*
        try to get current location, if available start location updates,
        else request additional settings
     */
    public void askForLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleConnection.getGoogleApiClient());
        if (mCurrentLocation != null) {
            Log.d(TAG, "GPS location was found!");
            GetCurrentLocation();
        } else {
            Log.d(TAG, "Current location was null, enable GPS on emulator!");

        }
    }


    private String SetUpURLString() {
        String str = "";
        if (mCurrentLocation != null) {
            StringBuilder sbDestinations = new StringBuilder();

            for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {
                sbDestinations.append(ListGateComplexPref.getInstance().gates.get(i).location.latitude + "," + ListGateComplexPref.getInstance().gates.get(i).location.longitude);
                if (i != ListGateComplexPref.getInstance().gates.size() - 1) {
                    sbDestinations.append("|");
                }
            }
            str = "https://maps.googleapis.com/maps/api/distancematrix/json" +

                    "?origins=" +
                    mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude() +
                    "&destinations=" +
                    sbDestinations.toString() +
                    "&mode=" + Settings.getInstance().getProfile() +
                    "&sensor=false" +
                    "&key=" + getResources().getString(R.string.google_app_id);
        }
        return str;
    }

    /*
        start the location updates
     */
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Location Premission", Toast.LENGTH_LONG).show();

            return;
        }
        if (!mIsLocationUpdatesOn) {
            if (mGoogleConnection.getGoogleApiClient().isConnected()) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleConnection.getGoogleApiClient(),
                        mLocationRequest, this);
                mIsLocationUpdatesOn = true;

            }
        }


    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return new MyBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }


    @Override
    public void update(Observable observable, Object data) {
        if (observable != mGoogleConnection) {
            return;
        }

        switch ((State) data) {

            case OPENED:
                Log.d(TAG, "OPENED");
                askForLocation();
                // We are signed in!
                // Retrieve some profile information to personalize our app for the user.
                break;
            case CLOSED:
                Log.d(TAG, "CLOSED");


                break;
        }
    }

    public void StopLocationUpdates() {
        if (mIsLocationUpdatesOn) {
            if (mGoogleConnection.getGoogleApiClient().isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleConnection.getGoogleApiClient(), this);
                mIsLocationUpdatesOn = false;
            }
        }
    }


    private void ApiLocationUpdate() {

        if (ListGateComplexPref.getInstance().gates.size() > 0) {

            //start the http request
            String url = SetUpURLString();
            Log.d(TAG, "Request URL" + url);
            final CustomJSONObjectRequest jsonRequest = new CustomJSONObjectRequest(Request.Method
                    .GET, url,
                    new JSONObject(), this, this);
            jsonRequest.setTag(REQUEST_TAG);
            mQueue.add(jsonRequest);
        }
    }

    @Override
    public void onResponse(Object response) {
        try {
            JSONObject root = new JSONObject(response.toString());
            JSONArray array_rows = root.getJSONArray("rows");
            JSONObject object_rows = array_rows.getJSONObject(0);
            JSONArray array_elements = object_rows.getJSONArray("elements");

            for (int i = 0; i < array_elements.length(); i++) {
                JSONObject object_elements = array_elements.getJSONObject(i);
                JSONObject object_duration = object_elements.getJSONObject("duration");
                JSONObject object_distance = object_elements.getJSONObject("distance");


                double duration = Integer.parseInt(object_duration.get("value").toString().split(" ")[0]);
                double distance = Double.parseDouble(object_distance.get("value").toString().split(" ")[0]);

                ListGateComplexPref.getInstance().gates.get(i).ETA = duration;
                ListGateComplexPref.getInstance().gates.get(i).distance = distance;

                mDistaceBeforeGPSUpdates = distance;


                Log.d(TAG, "Respond From Http Request, Distance : " + distance);
                Log.d(TAG, "Respond From Http Request, Duration : " + duration);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ListGateComplexPref.getInstance().sort();

        IsInsideTheRadius();

        StartApiLocationUpdate();

    }

    @Override
    public void onErrorResponse(VolleyError error) {


    }

    private void LocationBroadcast(long seconds) {
        if (ListGateComplexPref.getInstance().gates.size() > 0) {
            Intent intent = new Intent(Constants.LOCATION_SERVICE);
            intent.addFlags(Constants.LOCATION_UPDATE_FLAG);
            intent.putExtra(Constants.LOCATION, mCurrentLocation);
            intent.putExtra(Constants.DISTANCE, ListGateComplexPref.getInstance().gates.get(0).distance);
            intent.putExtra(Constants.NEXT_UPDATE, seconds);
            intent.putExtra(Constants.GOOGLE_CONNECTION, GoogleConnection.getInstance(this).getGoogleApiClient().isConnected());

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }


    /*
        ETA - from the http request to google Matrix
        Distance - from the http request to google Matrix
     */
    private void StartApiLocationUpdate() {

        if (ListGateComplexPref.getInstance().gates.size() < 1) { //there are no gates, return
            return;
        }

        if (ListGateComplexPref.getInstance().gates.get(0).status) //inside the radius
        {


            if (mIsLocationUpdatesOn) { //just entered the gate radius


                Log.d(TAG, "Stop GPS Location Updates");
                StopLocationUpdates();

                Log.d(TAG, "Make the Call to " + ListGateComplexPref.getInstance().gates.get(0).phone);

                MakeTheCall();

                StartOpenGateNotification(); //send notofication to open gate
            }

            mNextApiUpdate = Constants.API_REFRESH_HOME;



        }
        else //outside the radius
        {
            if (!mIsLocationUpdatesOn) { //location updates is off

                if (ListGateComplexPref.getInstance().gates.get(0).ETA <= Settings.getInstance().getGps_distance())//ETA is smaller then the GPS updates value

                {
                    Log.d(TAG, "Start GPS Location Updates");

                    mNextApiUpdate = Constants.API_REFRESH_GPS;

                    startLocationUpdates(); //start gps updates

                } else //ETA is greater then  GPS updates value
                {
                    mNextApiUpdate = (long) (ListGateComplexPref.getInstance().gates.get(0).ETA * 1000 / 2);
                    StopLocationUpdates();
                }
            }

        }
        StartGoogleDistanceApi(mNextApiUpdate);


        Log.d(TAG, "Next Google Distance Http Request In " + mNextApiUpdate / 1000 + " Seconds");

    }


    private void StartGoogleDistanceApi(long seconds) {
        if (seconds != Constants.API_REFRESH_GPS) {
            //start the Api Location Update in Constants.API_REFRESH seconds
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Start Api Location Update From Response");

                    StopApiLocationUpdate();
                    GetCurrentLocation(); //get a sample gps location

                }
            }, seconds);
        }

        LocationBroadcast(seconds);
    }

    private void StartOpenGateNotification() {

        if (!mIsNotificationActive){
            mCallHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    MakeTheCallOnBack();

                }
            }, Constants.LEAVE_NOTIFICATION_IN);
        }




    }


    public void StopApiLocationUpdate() {
        mHandler.removeCallbacksAndMessages(null);
    }


    class MyBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }


    public void MakeTheCallOnBack() {
        if (ListGateComplexPref.getInstance().gates.size() > 0) {

            mIsNotificationActive = true;

            Intent intent = new Intent(NOTIFICATION_DELETED_ACTION);
            PendingIntent pendintIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            registerReceiver(receiver, new IntentFilter(NOTIFICATION_DELETED_ACTION));

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, callIntent, 0);
            registerReceiver(receiver, new IntentFilter(Intent.ACTION_CALL));

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.gate)
                    .setContentTitle(this.getResources().getString(R.string.app_name))
                    .setContentText("Click To Open Gate")
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setDeleteIntent(pendintIntent)
                    .setContentIntent(mPendingIntent);

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            notificationBuilder.setStyle(inboxStyle);
            inboxStyle.setBigContentTitle("OpenSSME");
            inboxStyle.addLine("OpenSSME");
            notificationBuilder.setStyle(inboxStyle);
            mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());



        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIsNotificationActive = false;

            switch (intent.getAction()) {
                case Intent.ACTION_CALL:
                    MakeTheCall();
                    break;
                case NOTIFICATION_DELETED_ACTION:
                    break;

            }
            unregisterReceiver(this);
        }
    };

    private void MakeTheCall() {

        if (ListGateComplexPref.getInstance().gates.size() > 0 && !mIsNotificationActive) {


            Intent intent = new Intent(Intent.ACTION_CALL);

            intent.setData(Uri.parse("tel:" + ListGateComplexPref.getInstance().gates.get(0).phone));

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "No Phone Call Premission", Toast.LENGTH_LONG).show();
                return;
            }
            this.startActivity(intent);


        }
    }
}









