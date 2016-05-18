package com.example.root.openssme;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
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
import com.example.root.openssme.Utils.PrefUtils;
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
import java.util.Calendar;
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
    public LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private LocationManager mLocationManager;
    private RequestQueue mQueue;
    private Handler mCallHandler;
    private Handler mHandler;
    private boolean mLocker = true;

    @Override
    public void onCreate() {

        //get the last user settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mQueue = CustomVolleyRequestQueue.getInstance(getApplication())
                .getRequestQueue();

        mLocationManager = (LocationManager) this.getSystemService(
                Context.LOCATION_SERVICE);


        mGoogleConnection = GoogleConnection.getInstance(this);
        mGoogleConnection.addObserver(this);
        mHandler = new Handler();
        mCallHandler = new Handler();


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

        stopSelf();
    }

    @Override
    /*
      Called by startLocationUpdates, updates every UPDATE_INTERVAL seconds
     */
    public void onLocationChanged(Location location) {
        if (location != null) {

            mCurrentLocation = location;

            CalcDistanc();

            LocationBroadcast();

            //we send the last broadcast and stop the location service
            if (ListGateComplexPref.getInstance().gates.size() > 0 && ListGateComplexPref.getInstance().gates.get(0).status) {
                //unlock notification block
                mLocker = true;

                Log.d(TAG, "Stop Location Updats, Almost There");
                StopLocationUpdates();


                MakeTheCall();

                Log.d(TAG, "Start Api Location Updates Every " + Constants.API_REFRESH + "Seconds");
                StartApiLocationUpdate();
            }


        }
    }

    public void GetCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,
                new android.location.LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        mCurrentLocation = location;
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



    private void CalcDistanc() {

        if (mCurrentLocation != null && ListGateComplexPref.getInstance().gates.size() > 0) {

            Location to = new Location("");
            float distance;

            for (int i = 0; i <ListGateComplexPref.getInstance().gates.size() ; i++) {
                to.setLatitude(ListGateComplexPref.getInstance().gates.get(i).location.latitude);
                to.setLongitude(ListGateComplexPref.getInstance().gates.get(i).location.longitude);
                 distance = mCurrentLocation.distanceTo(to);
                ListGateComplexPref.getInstance().gates.get(i).distance = new Double(distance);

            }
            ListGateComplexPref.getInstance().sort();

            to.setLatitude(ListGateComplexPref.getInstance().gates.get(0).location.latitude);
            to.setLongitude(ListGateComplexPref.getInstance().gates.get(0).location.longitude);
             distance = mCurrentLocation.distanceTo(to);



            if (distance <= PrefUtils.getSettings(this, Constants.OPEN_DISTANCE)) {
                ListGateComplexPref.getInstance().gates.get(0).status = true;
                Toast.makeText(this, "Inside the Radius: Distance:" + distance + " ETA: " + ListGateComplexPref.getInstance().gates.get(0).ETA, Toast.LENGTH_SHORT);
            } else {
                ListGateComplexPref.getInstance().gates.get(0).status = false;
                Toast.makeText(this, "OutSite The Radius Distance: " + distance + " ETA: " + ListGateComplexPref.getInstance().gates.get(0).ETA, Toast.LENGTH_SHORT);

            }

            Log.d(TAG, "Are We Inside The Radius Of The Gate? " + ListGateComplexPref.getInstance().gates.get(0).status);

        }
    }


    /*
        try to get current location, if available start location updates,
        else request additional settings
     */
    public void askForLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Location Premission", Toast.LENGTH_LONG).show();
            return;
        }
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleConnection.getGoogleApiClient());
        if (mCurrentLocation != null) {
            Log.d(TAG, "GPS location was found!");
            ApiLocationUpdate();
        } else {
            Log.d(TAG, "Current location was null, enable GPS on emulator!");

        }
    }


    private String SetUpURLString() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Location Premission", Toast.LENGTH_LONG).show();
            return "";
        }
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
                    "&mode=driving&sensor=false&key=AIzaSyBtPT-uUi5oZ7ls7ysY8Zs8v8sWFApaCkM";

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
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleConnection.getGoogleApiClient(), this);
    }


    @Override
    public void onErrorResponse(VolleyError error) {


    }

            private void ApiLocationUpdate() {

                GetCurrentLocation();

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

                Log.d(TAG, "Respond From Http Request, Distance : " + distance);
                Log.d(TAG, "Respond From Http Request, Duration : " + duration);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ListGateComplexPref.getInstance().sort();

        LocationBroadcast();


        StartApiLocationUpdate();

    }

    private void LocationBroadcast(){
        if (ListGateComplexPref.getInstance().gates.size() > 0) {
            Intent intent = new Intent(Constants.LOCATION_SERVICE);
            intent.addFlags(Constants.LOCATION_UPDATE_FLAG);
            intent.putExtra(Constants.LOCATION, mCurrentLocation);
            intent.putExtra(Constants.SOURCE_FUNCTION, Constants.GOOGLE_API);
            intent.putExtra(Constants.DISTANCE, ListGateComplexPref.getInstance().gates.get(0).distance);
            intent.putExtra(Constants.LAST_UPDATE, Calendar.getInstance().get(Calendar.SECOND));
            intent.putExtra(Constants.GOOGLE_CONNECTION, GoogleConnection.getInstance(this).getGoogleApiClient().isConnected());

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }



    private void StartApiLocationUpdate() {


            //outside the radius
            //lees then 2 min to the gate
            if (ListGateComplexPref.getInstance().gates.size() > 0 && ListGateComplexPref.getInstance().gates.get(0).ETA <= PrefUtils.getSettings(this, Constants.START_LOCATAION_UPDATE) && !ListGateComplexPref.getInstance().gates.get(0).status) {

                Log.d(TAG, "API Location Were Stoped");
                StopApiLocationUpdate();
                Log.d(TAG, "Start Location Updates");
                startLocationUpdates();

            } else {


                //look if the user is outside the gate raduis
                if (ListGateComplexPref.getInstance().gates.size() > 0 && ListGateComplexPref.getInstance().gates.get(0).distance > PrefUtils.getSettings(this, Constants.OPEN_DISTANCE) * 2) {
                    Log.d(TAG, "Outside Gate Radius");
                    //gate not in radius
                    ListGateComplexPref.getInstance().gates.get(0).status = false;

                }
                //start the Api Location Update in Constants.API_REFRESH seconds
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Start Api Location Update From Response");
                        StopApiLocationUpdate();
                        ApiLocationUpdate();

                    }
                }, Constants.API_REFRESH);

                //ask user if he want to reopen the current gate
                //the user is inside the rdius
                //mLocker true  - first time entering
                if (mLocker && ListGateComplexPref.getInstance().gates.size() > 0 && ListGateComplexPref.getInstance().gates.get(0).status) {
                    mLocker = false;
                    mCallHandler.postDelayed(new Runnable() {
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
        if (ListGateComplexPref.getInstance().gates.size() > 0) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + ListGateComplexPref.getInstance().gates.get(0).phone));
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, callIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.gate)
                    .setContentTitle(this.getResources().getString(R.string.app_name))
                    .setContentText("Click To Open Gate")
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
    }

    private void MakeTheCall() {

        if (ListGateComplexPref.getInstance().gates.size() > 0) {

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




