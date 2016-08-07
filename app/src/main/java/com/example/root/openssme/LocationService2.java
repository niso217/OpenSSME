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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.SocialNetwork.Settings;
import com.example.root.openssme.Utils.CalculateDistanceTime;
import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.common.GoogleConnection;
import com.example.root.openssme.common.State;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

/**
 * Created by manuel on 21/07/13.
 */
public class LocationService2 extends Service implements
        LocationListener, Observer,CalculateDistanceTime.taskCompleteListener {

    protected static final String TAG = LocationService2.class.getSimpleName();
    public static final String ACTION_NEW_LOCATION = "com.manuelpeinado.locationservice.action.newlocation";
    private static final String NOTIFICATION_DELETED_ACTION = "delete_notofication";
    public static final int NOTIFICATION_ID = 100;


    public static final String EXTRA_LOCATION = "location";
    private NotificationManager mNotificationManager;
    private LocationRequest mLocationRequest;
    public GoogleConnection mGoogleConnection;
    private Location mCurrentLocation;
    private boolean mIgnoreFLAG;
    private long mNextUpdate = 10000;
    private CalculateDistanceTime distance_task;


        private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case Intent.ACTION_CALL:
                    MakeTheCall();
                    ChangeLocationRequest(mNextUpdate =  30 * 1000);
                    break;
                case NOTIFICATION_DELETED_ACTION:
                    break;

            }
        }
    };


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");

        //get an instant of the googleConnection
        mGoogleConnection = GoogleConnection.getInstance(this);

        //register the observer
        mGoogleConnection.addObserver(this);

        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        RegisterReciver();

        distance_task = new CalculateDistanceTime(this);
        distance_task.setLoadListener(this);

        setupLocationRequestBalanced(3000);

        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        if (mGoogleConnection.getGoogleApiClient().isConnected()) {
            startLocationUpdates();
        } else {
            connectClient();
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        // Disconnecting the client invalidates it.
        if (mGoogleConnection != null) {
            mGoogleConnection.disconnect();
            mGoogleConnection.deleteObserver(this);
        }
        StopLocationUpdates();
        unregisterReceiver(receiver);

        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {

        //save current location
        mCurrentLocation = location;

        //ignore logic inside onLocationChanged to avoid looping
        if (mIgnoreFLAG){
            mIgnoreFLAG = false;
            LocationBroadcast(mNextUpdate);
            return;
        }

        //there are no gates
        if(ListGateComplexPref.getInstance().gates.size()<1){
            StopLocationUpdates();
        }
        else
        {
            //calc all the gates location from the current location
            CalcGatesDistancs(location);

            //calc the closest[0] gate ETA
            distance_task.getDirectionsUrl(ListGateComplexPref.getInstance().gates.get(0).location, LocationToLatLng(location));



        }
        LocationBroadcast(mNextUpdate);




    }

    @Override
    public void taskCompleted(String[] time_distance) {

             //we have results
            if (time_distance.length>1){

                //set the closest ETA
                ListGateComplexPref.getInstance().gates.get(0).ETA = Double.valueOf(time_distance[1].replaceAll("[^\\d.]", ""));

                //on the way, samle the http request
                if (ListGateComplexPref.getInstance().getClosestETA() > Settings.getInstance().getGps_distance())
                {

                    int ClosestETA = (int)ListGateComplexPref.getInstance().getClosestETA();
                    long ETA = TimeUnit.MINUTES.toMillis(ClosestETA);
                    Log.d(TAG,"Starting new location update in "+ ClosestETA / 5);
                    mNextUpdate = ETA / 5;

                }
                //on the way x minutes before the gate, start massive GPS request
                else if (!ListGateComplexPref.getInstance().gates.get(0).status){
                    mNextUpdate = 3000;
                }



                //arrived to the gate
                if (ListGateComplexPref.getInstance().gates.get(0).status && ListGateComplexPref.getInstance().gates.get(0).active){

                        //StopLocationUpdates();

                        //lock this block of code
                        ListGateComplexPref.getInstance().gates.get(0).active = false;
                        Log.d(TAG, "Make the Call to " + ListGateComplexPref.getInstance().gates.get(0).phone);
                        //call the gate
                        MakeTheCall();
                        //start background notification
                        MakeTheCallOnBack();
                        //set the GPS interval
                        mNextUpdate = 15 * 60 * 1000;


                }

                ChangeLocationRequest(mNextUpdate);
                Log.d(TAG,"Next Update "+ mNextUpdate);
                Log.d(TAG,"Is Active "+ ListGateComplexPref.getInstance().gates.get(0).active);
                Log.d(TAG,"Is Inside Radius "+ ListGateComplexPref.getInstance().gates.get(0).status);
                Log.d(TAG,"ETA "+ ListGateComplexPref.getInstance().getClosestETA());
                Log.d(TAG,"Distance "+ ListGateComplexPref.getInstance().getClosestDistance());

            }

    }

        private void RegisterReciver(){
        IntentFilter filter = new IntentFilter(Intent.ACTION_CALL);
        filter.addAction(NOTIFICATION_DELETED_ACTION);
        registerReceiver(receiver, filter);
    }

    public void connectClient() {

        // Connect the client.
        if (mGoogleConnection != null && !mGoogleConnection.getGoogleApiClient().isConnected()) {
            mGoogleConnection.connect();
        }
    }

    private void ChangeLocationRequest(long ETA)
    {
        //turn on IgnoreFlag
        mIgnoreFLAG = true;
        StopLocationUpdates();
        setupLocationRequestBalanced(ETA);
        startLocationUpdates();
    }

    protected void startLocationUpdates() {

        if (mGoogleConnection.getGoogleApiClient().isConnected()) {

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

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleConnection.getGoogleApiClient(), mLocationRequest, this);
            }
    }

    public void setupLocationRequestBalanced(long Interval) {
        //change the time of location updates
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(Interval)
                .setFastestInterval(Interval);


    }

    public void StopLocationUpdates() {
            if (mGoogleConnection.getGoogleApiClient().isConnected()) {
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleConnection.getGoogleApiClient(), this);
                }
            }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable != mGoogleConnection) {
            return;
        }

        switch ((State) data) {

            case OPENED:
                Log.d(TAG, "OPENED");
                startLocationUpdates();
                // We are signed in!
                // Retrieve some profile information to personalize our app for the user.
                break;
            case CLOSED:
                Log.d(TAG, "CLOSED");


                break;
        }
    }

    private void CalcGatesDistancs(Location CurrentLocation) {

        if (ListGateComplexPref.getInstance().gates.size() > 0) {

            Location to = new Location("");
            for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {
                to.setLatitude(ListGateComplexPref.getInstance().gates.get(i).location.latitude);
                to.setLongitude(ListGateComplexPref.getInstance().gates.get(i).location.longitude);
                ListGateComplexPref.getInstance().gates.get(i).distance = new Double(CurrentLocation.distanceTo(to));

            }
            ListGateComplexPref.getInstance().sort();

        }
        //set the closest gate status (inside the radius) and the active (locker) params
        ClacDistancLogic();



    }

    private void ClacDistancLogic(){
        if (ListGateComplexPref.getInstance().gates.get(0).distance <= Settings.getInstance().getOpen_distance())
            ListGateComplexPref.getInstance().gates.get(0).status = true;
        else
            ListGateComplexPref.getInstance().gates.get(0).status = false;

        if (ListGateComplexPref.getInstance().gates.get(0).distance > Settings.getInstance().getOpen_distance() * 3)
            ListGateComplexPref.getInstance().gates.get(0).active = true;
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

    private LatLng LocationToLatLng(Location location){
        return new LatLng(location.getLatitude(),location.getLongitude());
    }

        private void MakeTheCall() {

            Intent intent = new Intent(Intent.ACTION_CALL);

            intent.setData(Uri.parse("tel:" + ListGateComplexPref.getInstance().gates.get(0).phone));

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "No Phone Call Premission", Toast.LENGTH_LONG).show();
                return;
            }
            this.startActivity(intent);

    }

        public void MakeTheCallOnBack() {

            Intent intent = new Intent(NOTIFICATION_DELETED_ACTION);
            PendingIntent pendintIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, callIntent, 0);

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