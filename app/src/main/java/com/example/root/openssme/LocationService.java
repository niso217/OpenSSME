package com.example.root.openssme;

/**
 * Created by nirb on 07/12/2016.
 */

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.SocialNetwork.Settings;
import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.common.GoogleConnection;
import com.example.root.openssme.common.State;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.example.root.openssme.Utils.Constants.GateStatus;


import java.text.DateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;


public class LocationService extends Service implements LocationListener, Observer{
    private final String TAG = LocationService.class.getSimpleName();
    private Timer mTimer;
    private TimerTask mTimerTask;
    public int counter=0;
    Runnable m_handlerTask ;
    private int mClosesetGateIndex;
    private Notification mNotification;
    public static final String RECIVER_FILTER ="com.example.root.LocationService.RestartServer";
    private static final String NOTIFICATION_DELETED_ACTION = "com.example.root.LocationService.delete_notofication";
    public static final String STOP_SERVICE ="server.http.android.androidhttpserver.StopService";
    public static final int DRIVING_SPEED = 100;
    private static boolean doTerminate;

    public static boolean mCodeBlocker;
    public static final int NOTIFICATION_ID = 100;


    public GoogleConnection mGoogleConnection;
    public LocationRequest mLocationRequest;
    private boolean mIsNotificationActive;
    private boolean mIsLocationUpdatesOn;
    private Location mCurrentLocation;

    private LocationManager mLocationManager;
    private double mCurrentSpeed;

    private NotificationManager mNotificationManager;
    private long mNextUpdate = 1000;
    private Handler mHandler;
    private Handler mCallHandler;
    public LocationService() {
        super();
    }



    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIsNotificationActive = false;

            switch (intent.getAction()) {
                case Intent.ACTION_CALL:

                    MakeTheCall();
                    startLocationUpdates();
                    break;
                case NOTIFICATION_DELETED_ACTION:
                    break;

            }
        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "=====Service is Starting...=====");
        InitializeNotification();
        Initialize();
        if (mGoogleConnection.getGoogleApiClient().isConnected()) {

            startLocationUpdates();
        } else {
            connectClient();
        }
        return START_STICKY;
    }

    private void Initialize()
    {
        mCodeBlocker = false;
        doTerminate = false;
        mHandler = new Handler();
        initializeTimerTask();

        mGoogleConnection = GoogleConnection.getInstance(this);
        mGoogleConnection.addObserver(this);

        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        setupLocationRequestBalanced(mNextUpdate);

        RegisterReciver();



    }


    private void RegisterReciver(){
        IntentFilter filter = new IntentFilter(Intent.ACTION_CALL);
        filter.addAction(NOTIFICATION_DELETED_ACTION);
        registerReceiver(receiver, filter);
    }

    private void ChangeLocationRequest(long ETA)
    {

        StopLocationUpdates();
        setupLocationRequestBalanced(ETA);
        startLocationUpdates();
    }

    public  void StopLocationUpdates() {
        if (mIsLocationUpdatesOn) {
            if (mGoogleConnection.getGoogleApiClient().isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleConnection.getGoogleApiClient(), this);
                mIsLocationUpdatesOn = false;
                Log.d(TAG,"Location Updates: "+mIsLocationUpdatesOn);
            }
        }
    }

    public void setupLocationRequestBalanced(long Interval) {
        //change the time of location updates
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(Interval)
                .setFastestInterval(Interval);
    }

    private void InitializeNotification()
    {
        Intent notificationIntent = new Intent(STOP_SERVICE);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        RemoteViews notificationView = new RemoteViews(getApplicationContext().getPackageName(),R.layout.notification);


        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.gate);

        Intent buttonCloseIntent = new Intent(getApplicationContext(), NotificationCloseButtonHandler.class);
        buttonCloseIntent.putExtra("action", "close");


        PendingIntent buttonClosePendingIntent = pendingIntent.getBroadcast(getApplicationContext(), 0, buttonCloseIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.btnStop, buttonClosePendingIntent);

        mNotification = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Connection Service")
                .setTicker("Connection Service")
                .setContentText("Connection Service")
                .setSmallIcon(R.drawable.gate)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContent(notificationView)
                .setOngoing(true).build();



        startForeground(101,
                mNotification);
    }

    public static class NotificationCloseButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.stopService(new Intent(context, LocationService.class));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // Disconnecting the client invalidates it.
        if (mGoogleConnection != null) {
            mGoogleConnection.disconnect();
            mGoogleConnection.deleteObserver(this);
        }
        StopLocationUpdates();
        unregisterReceiver(receiver);
        mHandler.removeCallbacks(m_handlerTask);
        RestartService();

    }

    private void RestartService()
    {
        if (!doTerminate)
        {
            Intent broadcastIntent = new Intent(RECIVER_FILTER);
            sendBroadcast(broadcastIntent);
        }
    }


    /**
     * it sets the timer to print the counter every x seconds
     */
    public void initializeTimerTask() {

        m_handlerTask = new Runnable()
        {
            @Override
            public void run() {
                Log.d(TAG, "in timer ++++  "+ (counter++));
                mHandler.postDelayed(m_handlerTask, 1000);

                //there are no gates
                if(ListGateComplexPref.getInstance().gates.size()==0 && mIsLocationUpdatesOn){
                    Log.d(TAG,"No Gates Found, Stopping Location Updates");
                    StopLocationUpdates();
                    stopSelf();
                    return;
                }

                if (!mCodeBlocker && mCurrentLocation!=null)
                {
                    //calc all the gates location from the current location
                    CalcGatesDistancsAndETA();

                    ClacDistancLogic();

                    //calc the closest[0] gate ETA
                    DoWork();


                }
                DATABroadcast();



            }
        };
        m_handlerTask.run();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ChangeNotoficationContent(boolean isConnected)
    {
        NotificationManager mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification);
        //contentView.setImageViewResource(R.id.circle, isConnected ? R.drawable.on : R.drawable.off);
        mNotification.contentView = contentView;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mNotification.contentIntent = contentIntent;

        mNotification.flags |= Notification.FLAG_NO_CLEAR; //Do not clear the notification
        if (isConnected)
        mNotification.defaults |= Notification.DEFAULT_SOUND ; // Sound
        else
            mNotification.defaults = 0; // Sound


        mNotificationManager.notify(101, mNotification);


    }

    public void connectClient() {

        // Connect the client.
        if (mGoogleConnection != null && !mGoogleConnection.getGoogleApiClient().isConnected()) {
            mGoogleConnection.connect();
        }
    }


    @Override
    public void onLocationChanged(Location location) {


        Log.d(TAG,"=====Location Changed=====");

        mCurrentLocation = location;

        mCurrentSpeed = location.getSpeed();

        LocationBroadcast();

    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable != mGoogleConnection) {
            return;
        }

        switch ((State) data) {


            case OPENED:
                Log.d(TAG, DateFormat.getDateTimeInstance().format(new Date()) +": "+ "OPENED");
                startLocationUpdates();
                // We are signed in!
                // Retrieve some profile information to personalize our app for the user.
                break;
            case CLOSED:
                Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+ "CLOSED");


                break;
        }
    }

    protected void startLocationUpdates() {


        if (mGoogleConnection.getGoogleApiClient().isConnected() && !mIsLocationUpdatesOn) {

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
            mIsLocationUpdatesOn = true;
        }
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

    private void CalcGatesDistancsAndETA() {


            for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {
                Location GateLocation = LatLngToLocation(ListGateComplexPref.getInstance().gates.get(i).location);
                ListGateComplexPref.getInstance().gates.get(i).distance = new Double(mCurrentLocation.distanceTo(GateLocation));

                double distance =  ListGateComplexPref.getInstance().gates.get(i).distance / 1000; //to Km
                double time = (distance / DRIVING_SPEED) * 60; //minutes
                ListGateComplexPref.getInstance().gates.get(i).ETA = time;
            }
            ListGateComplexPref.getInstance().sort();




    }

    private void ClacDistancLogic(){



        //is inside gate radius
        if (ListGateComplexPref.getInstance().gates.get(0).distance <= Settings.getInstance().getOpen_distance()){
            ListGateComplexPref.getInstance().gates.get(0).status = GateStatus.HOME;

        }
        else if (ListGateComplexPref.getInstance().gates.get(0).distance <= Settings.getInstance().getGps_distance() * 1000)
        {
            ListGateComplexPref.getInstance().gates.get(0).status = GateStatus.ALMOST;

        }
        else
            ListGateComplexPref.getInstance().gates.get(0).status = GateStatus.ONWAY;


        //active the gate
        if (ListGateComplexPref.getInstance().gates.get(0).distance > Settings.getInstance().getOpen_distance() * 5)
            ListGateComplexPref.getInstance().gates.get(0).active = true;



    }

    public void DoWork() {

        long nextUpdate = 30000;

        if (ListGateComplexPref.getInstance().gates.get(0).active)
        {
        if (ListGateComplexPref.getInstance().getClosestETA() > Settings.getInstance().getGps_distance()) {
            Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+ "=====Out Side GPS Open Distance=====");
            nextUpdate = (long)((ListGateComplexPref.getInstance().gates.get(0).ETA / 2)  * 60  * 1000);


        }
        //on the way x minutes before the gate, start massive GPS request
        else if (ListGateComplexPref.getInstance().gates.get(0).status==GateStatus.ALMOST){
            Log.d(TAG,"=====Massive GPS Request=====");
            nextUpdate = 3000;
        }



        //arrived to the gate
        if (ListGateComplexPref.getInstance().gates.get(0).status==GateStatus.HOME){

            //lock this block of code
            ListGateComplexPref.getInstance().gates.get(0).active = false;

            Log.d(TAG,"===========OpenSSME===========");
            Log.d(TAG,"Make The Call To " + ListGateComplexPref.getInstance().gates.get(0).phone);
            //call the gate
            MakeTheCall();
            //start background notification
            MakeTheCallOnBack();

            PrefUtils.setCurrentGate(ListGateComplexPref.getInstance(),getApplicationContext());


        }
        }

        if (nextUpdate!=mNextUpdate)
        {
            mNextUpdate = nextUpdate;
            ChangeLocationRequest(mNextUpdate);

        }
        Log.d(TAG,"====Current Settings====");
        Log.d(TAG,"GPS Open Distance: "+Settings.getInstance().getGps_distance());
        Log.d(TAG,"Gate Radius Open Distance: "+Settings.getInstance().getOpen_distance());
        Log.d(TAG,"Location Updates: "+mIsLocationUpdatesOn);
        Log.d(TAG,"Next Location Update: " + mNextUpdate);

        Log.d(TAG,"===========Gate Details========");
        Log.d(TAG,"Gate Name: " + ListGateComplexPref.getInstance().gates.get(0).gateName);
        Log.d(TAG,"distance: " + ListGateComplexPref.getInstance().gates.get(0).distance);
        Log.d(TAG,"ETA: " + ListGateComplexPref.getInstance().gates.get(0).ETA);
        Log.d(TAG,"Speed: " + mCurrentSpeed);




    }

    private void LocationBroadcast()
    {
        Intent intent = new Intent(Constants.LOCATION_SERVICE);
        intent.addFlags(Constants.LOCATION_UPDATE_FLAG);
        intent.putExtra(Constants.LOCATION, mCurrentLocation);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    private void DATABroadcast() {

            Intent intent = new Intent(Constants.LOCATION_SERVICE_DATA);
            intent.addFlags(Constants.DATA_UPDATE_FLAG);

        if (!ListGateComplexPref.getInstance().gates.isEmpty())
        {
            intent.putExtra(Constants.DISTANCE, ((Math.floor(ListGateComplexPref.getInstance().gates.get(0).distance * 0.001 * 100) / 100)) +" Km");
            intent.putExtra(Constants.GPS, mIsLocationUpdatesOn+"");
            intent.putExtra(Constants.SPEED, mCurrentSpeed==0 ? "No Movement" : mCurrentSpeed+"");
            intent.putExtra(Constants.GATE_NAME, ListGateComplexPref.getInstance().gates.get(0).gateName);
            intent.putExtra(Constants.ETA, (Math.floor(ListGateComplexPref.getInstance().gates.get(0).ETA *100) /100)+"");
            intent.putExtra(Constants.GATE_RADIUS, ListGateComplexPref.getInstance().gates.get(0).status.status());
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        }



    }

    private Location LatLngToLocation(LatLng latlang)
    {
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        loc.setLatitude(latlang.latitude);
        loc.setLongitude(latlang.longitude);
        return loc;
    }


}
