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

import com.android.volley.RequestQueue;
import com.example.root.openssme.SocialNetwork.Gate;
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

import java.text.DateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class LocationService extends Service implements LocationListener, Observer{
    private final String TAG = LocationService.class.getSimpleName();
    private Timer mTimer;
    private TimerTask mTimerTask;
    public int counter=0;
    private Notification mNotification;
    public static final String RECIVER_FILTER ="com.example.root.LocationService.RestartServer";
    private static final String NOTIFICATION_DELETED_ACTION = "com.example.root.LocationService.delete_notofication";
    public static final int NOTIFICATION_ID = 100;


    public GoogleConnection mGoogleConnection;
    public LocationRequest mLocationRequest;
    private boolean mIsNotificationActive;
    private boolean mIsLocationUpdatesOn = false;
    private Location mCurrentLocation;
    private int mLocationSamplesCounter;

    private LocationManager mLocationManager;
    private double mCurrentSpeed;
    private Location [] mLastTwoLocation;
    private long prev_time,cur_time;

    private NotificationManager mNotificationManager;
    private long mNextUpdate = 3000;
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
        Log.d(TAG, "Service is Starting...");

        Initialize();

        if (mGoogleConnection.getGoogleApiClient().isConnected()) {

            //InitializeNotification();

            startLocationUpdates();
        } else {
            connectClient();
        }
        return START_STICKY;
    }

    private void Initialize()
    {
        mHandler = new Handler();
        mLastTwoLocation = new Location[2];
        startTimer();

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

    public void StopLocationUpdates() {
        if (mIsLocationUpdatesOn) {
            if (mGoogleConnection.getGoogleApiClient().isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleConnection.getGoogleApiClient(), this);
                mIsLocationUpdatesOn = false;
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
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        RemoteViews notificationView = new RemoteViews(this.getPackageName(),R.layout.notification);


        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        mNotification = new NotificationCompat.Builder(this)
                .setContentTitle("Connection Service")
                .setTicker("Connection Service")
                .setContentText("Connection Service")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContent(notificationView)
                .setOngoing(true).build();



        startForeground(101,
                mNotification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        stoptimertask();

        // Disconnecting the client invalidates it.
        if (mGoogleConnection != null) {
            mGoogleConnection.disconnect();
            mGoogleConnection.deleteObserver(this);
        }
        StopLocationUpdates();
        unregisterReceiver(receiver);

        Intent broadcastIntent = new Intent(RECIVER_FILTER);
        sendBroadcast(broadcastIntent);
    }



    public void startTimer() {
        //set a new Timer
        mTimer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 1 second
        mTimer.schedule(mTimerTask, 1000, 1000); //
    }

    /**
     * it sets the timer to print the counter every x seconds
     */
    public void initializeTimerTask() {
        mTimerTask = new TimerTask() {
            public void run() {
                Log.d(TAG, "in timer ++++  "+ (counter++));
                    if (counter % 2 ==0)
                        LocationBroadcast();

                //there are no gates
                if(ListGateComplexPref.getInstance().gates.size()<1){
                    Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+ "No gates()");
                    StopLocationUpdates();
                }
                if(mLastTwoLocation[0]==null || mCurrentSpeed<1) return;

                else
                {
                    //calc all the gates location from the current location
                    CalcGatesDistancsAndETA();

                    ClacDistancLogic();

                    //calc the closest[0] gate ETA
                    DoWork();

                }
            }
        };
    }



    /**
     * not needed
     */
    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
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

        mLocationSamplesCounter++;
        if (mLocationSamplesCounter % 2 ==0)
        {
            calc_speed();
            prev_time = cur_time;

        }
        else
            cur_time = System.currentTimeMillis() / 1000L;


        if (mCurrentLocation==null)
        {
            mLastTwoLocation[0] = mCurrentLocation;

        }
        else
        {
            mLastTwoLocation[1] = mLastTwoLocation[0];
            mLastTwoLocation[0] = location;
        }
        mCurrentLocation = location;



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

        if (ListGateComplexPref.getInstance().gates.size() > 0) {

            for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {
                Location GateLocation = LatLngToLocation(ListGateComplexPref.getInstance().gates.get(i).location);
                ListGateComplexPref.getInstance().gates.get(i).distance = new Double(mLastTwoLocation[0].distanceTo(GateLocation));

                double distance =  ListGateComplexPref.getInstance().gates.get(i).distance / 1000; //to Km
                double time = (distance / mCurrentSpeed) * 60; //minutes
                ListGateComplexPref.getInstance().gates.get(i).ETA = time;

                Log.d(TAG,"===========CalcGatesDistancsAndETA function========");
                Log.d(TAG,"Gate Name: " + ListGateComplexPref.getInstance().gates.get(i).gateName);
                Log.d(TAG,"distance: " + distance);
                Log.d(TAG,"minues: " + time);
                Log.d(TAG,"Speed: " + mCurrentSpeed);


            }
            ListGateComplexPref.getInstance().sort();

        }
        //set the closest gate status (inside the radius) and the active (locker) params

    }

    private void ClacDistancLogic(){

        if (ListGateComplexPref.getInstance().gates.isEmpty()) return;
        //is inside gate radius
        if (ListGateComplexPref.getInstance().gates.get(0).distance <= Settings.getInstance().getOpen_distance()){
            ListGateComplexPref.getInstance().gates.get(0).status = true;

        }
        else
            ListGateComplexPref.getInstance().gates.get(0).status = false;

        //active the gate
        if (ListGateComplexPref.getInstance().gates.get(0).distance > Settings.getInstance().getOpen_distance() * 5)
            ListGateComplexPref.getInstance().gates.get(0).active = true;

    }

    public void DoWork() {

        long nextUpdate = 0;
        Log.d(TAG,"====Current Settings====");
        Log.d(TAG,"GPS Open Distance: "+Settings.getInstance().getGps_distance());
        Log.d(TAG,"Gate Radius Open Distance: "+Settings.getInstance().getOpen_distance());



        double ETA = ListGateComplexPref.getInstance().gates.get(0).ETA;


        if (ListGateComplexPref.getInstance().getClosestETA() > Settings.getInstance().getGps_distance()) {
            Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+ "=====Out Side GPS Open Distance=====");

            nextUpdate = (long)((ETA / 2) * 60 * 1000);


        }
        //on the way x minutes before the gate, start massive GPS request
        else if (!ListGateComplexPref.getInstance().gates.get(0).status){
            Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+ "=====massive GPS request=====");
            nextUpdate = 3000;
        }



        //arrived to the gate
        if (ListGateComplexPref.getInstance().gates.get(0).status && ListGateComplexPref.getInstance().gates.get(0).active){

            StopLocationUpdates();

            //lock this block of code
            ListGateComplexPref.getInstance().gates.get(0).active = false;

            Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+ "===========OpenSSME===========");
            Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+ "Make the Call to " + ListGateComplexPref.getInstance().gates.get(0).phone);
            //call the gate
            MakeTheCall();
            //start background notification
            MakeTheCallOnBack();

        }

        if (nextUpdate!=mNextUpdate)
        {
            mNextUpdate = nextUpdate;
            ChangeLocationRequest(mNextUpdate);

        }

        Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +"Gate: "+ ListGateComplexPref.getInstance().gates.get(0).gateName);
        Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+"Distance "+ ListGateComplexPref.getInstance().getClosestDistance());
        Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+"Speed "+ mCurrentSpeed);
        Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +" ETA: "+ ETA);
        Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+"Next Update "+ mNextUpdate);
        Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+"Is Active "+ ListGateComplexPref.getInstance().gates.get(0).active);
        Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+"Is Inside Radius "+ ListGateComplexPref.getInstance().gates.get(0).status);

    }

    private void calc_speed()
    {
        Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +" ====calc_speed function=====");
        if (mLastTwoLocation[0]==null || mLastTwoLocation[1]==null) return;
        //Computation for speed
        double distance_between_points = mLastTwoLocation[0].distanceTo(mLastTwoLocation[1]);
        float loc_distance =(new Double(distance_between_points)).longValue();
        Log.d(TAG,"cur_time: " + cur_time);
        Log.d(TAG,"prev_time: " + prev_time);
        Log.d(TAG,"loc_distance: " + loc_distance);

        mCurrentSpeed = loc_distance/(cur_time - prev_time) * 3.6; //to Km per hour
        Log.d(TAG,"Speed: "+ mCurrentSpeed);


        if (mCurrentSpeed<30 || mCurrentSpeed>200 || (Double.isNaN(mCurrentSpeed))){ //bad result ignore
            mCurrentSpeed=80; //in cas
            Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+ "Deffult Speed set to: "+ mCurrentSpeed);
        }
        else
        {
            Log.d(TAG,DateFormat.getDateTimeInstance().format(new Date()) +": "+ "ETA = distance / time: "+ loc_distance/(cur_time - prev_time));

        }



    }



    private void LocationBroadcast() {
        if (ListGateComplexPref.getInstance().gates.size() > 0) {
            Intent intent = new Intent(Constants.LOCATION_SERVICE);
            intent.addFlags(Constants.LOCATION_UPDATE_FLAG);
            intent.putExtra(Constants.LOCATION, mCurrentLocation);
            intent.putExtra(Constants.DISTANCE, ListGateComplexPref.getInstance().gates.get(0).distance);
            intent.putExtra(Constants.GOOGLE_CONNECTION, GoogleConnection.getInstance(this).getGoogleApiClient().isConnected());
            intent.putExtra(Constants.NEXT_UPDATE, mNextUpdate);
            intent.putExtra(Constants.SPEED, mCurrentSpeed+"");

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
