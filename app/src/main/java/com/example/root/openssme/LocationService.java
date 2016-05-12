package com.example.root.openssme;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.root.openssme.Adapter.GateAdapter;
import com.example.root.openssme.SocialNetwork.Gate;
import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.SocialNetwork.User;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.common.GoogleConnection;
import com.example.root.openssme.common.State;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class LocationService extends Service implements
        ResultCallback<LocationSettingsResult>,
        LocationListener,
        Observer,
        Response.Listener,
        Response.ErrorListener {

    protected static final String TAG = LocationService.class.getSimpleName();
    public static final String REQUEST_TAG = "MainVolleyActivity";
    public GoogleConnection mGoogleConnection;
    private long UPDATE_INTERVAL = 60000;  /* 60 secs */
    private long FASTEST_INTERVAL = 3000; /* 5 secs */
    public LocationRequest mLocationRequest;
    private Location mLocationServices;
    private RequestQueue mQueue;
    private Handler mHandler;
    private boolean mLocker = true;

    @Override
    public void onCreate() {

        super.onCreate();
        mQueue = CustomVolleyRequestQueue.getInstance(getApplication())
                .getRequestQueue();


        mGoogleConnection = GoogleConnection.getInstance(this);
        mGoogleConnection.addObserver(this);
        mHandler = new Handler();


        setupLocationRequestBalanced();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connectClient();
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
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
    }


    @Override
    public void onDestroy() {
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
    }

    @Override
    /*
      Called by startLocationUpdates, updates every UPDATE_INTERVAL seconds
     */
    public void onLocationChanged(Location location) {
        if (location != null) {

            float distanse = CalcDistanc(location);


            //we send the last broadcast and stop the location service
            if (ListGateComplexPref.getInstance().gates.get(0).status) {
                //unlock notification block
                mLocker = true;

                Log.d(TAG, "Stop Location Updats, Almost There Wihing : " + distanse + " Meters");
                StopLocationUpdates();

                Intent intent = new Intent(Constants.LOCATION_SERVICE);
                intent.addFlags(Constants.LOCATION_UPDATE_FLAG);
                intent.putExtra(Constants.LOCATION, location);
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);

                MakeTheCall();

                Log.d(TAG, "Start Api Location Updates Every " + Constants.API_REFRESH + "Seconds");
                StartApiLocationUpdate();
            }


        }
    }

    private float CalcDistanc(Location from) {
        Location loc = new Location("dummyprovider");
        loc.setLatitude(ListGateComplexPref.getInstance().gates.get(0).location.latitude);
        loc.setLongitude(ListGateComplexPref.getInstance().gates.get(0).location.longitude);
        float distance = from.distanceTo(loc);

        if (distance <= Constants.DISTANCE_TO_OPEN) {
            ListGateComplexPref.getInstance().gates.get(0).status = true;
        } else
            ListGateComplexPref.getInstance().gates.get(0).status = false;

        Log.d(TAG, "Are We Inside The Radius Of The Gate? " + ListGateComplexPref.getInstance().gates.get(0).status);

        return distance;

    }




    /*
        try to get current location, if available start location updates,
        else request additional settings
     */
    public void askForLocation() {


        mLocationServices = LocationServices.FusedLocationApi.getLastLocation(mGoogleConnection.getGoogleApiClient());
        if (mLocationServices != null) {
            Log.d(TAG, "GPS location was found!");
            ApiLocationUpdate();
        } else {
            Log.d(TAG, "Current location was null, enable GPS on emulator!");

        }
    }


    private String SetUpURLString(ArrayList<Gate> gates) {
        mLocationServices = LocationServices.FusedLocationApi.getLastLocation(mGoogleConnection.getGoogleApiClient());
        String str = "";

        if (mLocationServices != null) {


            StringBuilder sbDestinations = new StringBuilder();

            for (int i = 0; i < gates.size(); i++) {
                sbDestinations.append(gates.get(i).location.latitude + "," + gates.get(i).location.longitude);
                if (i != gates.size() - 1) {
                    sbDestinations.append("|");
                }

            }
            str = "http://maps.googleapis.com/maps/api/distancematrix/json" +

                    "?origins=" +
                    mLocationServices.getLatitude() + "," + mLocationServices.getLongitude() +
                    "&destinations=" +
                    sbDestinations.toString() +
                    "&mode=driving&sensor=false";

        }
        return str;

    }

    /*
        start the location updates
     */
    protected void startLocationUpdates() {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleConnection.getGoogleApiClient(),
                    mLocationRequest, this);

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
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {


    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable != mGoogleConnection) {
            return;
        }

        switch ((State) data) {

            case OPENED:
                Log.d(TAG, "OPENED");
                // We are signed in!
                // Retrieve some profile information to personalize our app for the user.
                break;
            case CLOSED:
                Log.d(TAG, "CLOSED");


                break;
        }
    }

    public void StopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleConnection.getGoogleApiClient(), this);
    }


    @Override
    public void onErrorResponse(VolleyError error) {


    }

    @Override
    public void onResponse(Object response) {
        double distance = Double.MAX_VALUE;
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
                distance = Double.parseDouble(object_distance.get("value").toString().split(" ")[0]);

                ListGateComplexPref.getInstance().gates.get(i).ETA = duration;
                ListGateComplexPref.getInstance().gates.get(i).distance = distance;

                Log.d(TAG, "Respond From Http Request, Distance : " + distance);
                Log.d(TAG, "Respond From Http Request, Duration : " + duration);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ListGateComplexPref.getInstance().sort();

        Intent intent = new Intent(Constants.LOCATION_SERVICE);
        intent.addFlags(Constants.LOCATION_UPDATE_FLAG);
        intent.putExtra(Constants.DISTANCE, ListGateComplexPref.getInstance().gates.get(0).distance);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        StartApiLocationUpdate();

    }

    private void ApiLocationUpdate() {
        if (mLocationServices != null && ListGateComplexPref.getInstance().gates != null) {

            //calc the status of the gate, if inside the radius
            CalcDistanc(mLocationServices);

            //start the http request
            String url = SetUpURLString(ListGateComplexPref.getInstance().gates);
            Log.d(TAG, "Request URL" + url);
            final CustomJSONObjectRequest jsonRequest = new CustomJSONObjectRequest(Request.Method
                    .GET, url,
                    new JSONObject(), this, this);
            jsonRequest.setTag(REQUEST_TAG);
            mQueue.add(jsonRequest);
        }
    }

    private void StartApiLocationUpdate() {

        //outside the radius
        //lees then 2 min to the gate
        if (ListGateComplexPref.getInstance().gates.get(0).ETA < Constants.START_LOCATAION_UPDATE && !ListGateComplexPref.getInstance().gates.get(0).status) {

            Log.d(TAG, "API Location Were Stoped");
            StopApiLocationUpdate();
            Log.d(TAG, "Start Location Updates");
            startLocationUpdates();

        } else {

            //look if the user is outside the gate raduis
            if (ListGateComplexPref.getInstance().gates.get(0).distance > Constants.DISTANCE_TO_OPEN) {
                Log.d(TAG, "Outside Gate Radius");
                //gate not in radius
                ListGateComplexPref.getInstance().gates.get(0).status = false;

            }
            //start the Api Location Update in Constants.API_REFRESH seconds
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Start Api Location Update From Response");
                    ApiLocationUpdate();

                }
            }, Constants.API_REFRESH);

            //ask user if he want to reopen the current gate
            //the user is inside the rdius
            //mLocker true  - first time entering
            if (mLocker && ListGateComplexPref.getInstance().gates.get(0).status) {
                mLocker = false;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Log.d(TAG, "Start Notification after 2 minunes");
                        MakeTheCallOnBack();
                    }
                }, 1000 * 10);
            }


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

    /** calculates the distance between two locations in MILES */
    public double distance(double lat1, double lng1, double lat2, double lng2) {

        double earthRadius = 3958.75; // in miles, change to 6371 for kilometer output

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);

        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double dist = earthRadius * c;

        return dist; // output distance, in MILES
    }

    public void MakeTheCallOnBack() {
        Toast.makeText(this, "call", Toast.LENGTH_SHORT).show();
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + ListGateComplexPref.getInstance().gates.get(0).phone));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, callIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.gate)
                .setContentTitle(this.getResources().getString(R.string.app_name))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        notificationBuilder.setStyle(inboxStyle);
        inboxStyle.setBigContentTitle("OpenSSME");
        inboxStyle.addLine("OpenSSME");
        notificationBuilder.setStyle(inboxStyle);

        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        int NOTIFICATION_ID = 100;
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void MakeTheCall() {

        Intent intent = new Intent(Intent.ACTION_CALL);

        intent.setData(Uri.parse("tel:" + ListGateComplexPref.getInstance().gates.get(0).phone));

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        }
        this.startActivity(intent);
    }
    private void requestPermission() {
        //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, mRequestCode);
    }


    }




