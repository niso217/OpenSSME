package com.open.ssme.Service;

/**
 * Created by nirb on 07/12/2016.
 */

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.open.ssme.Activity.ExitActivity;
import com.open.ssme.Activity.MainActivity;
import com.open.ssme.Common.GoogleConnection;
import com.open.ssme.Common.State;
import com.open.ssme.Helpers.GoogleMatrixRequest;
import com.open.ssme.Helpers.SingleShotLocationProvider;
import com.open.ssme.Objects.Gate;
import com.open.ssme.Objects.GoogleMatrixResponse;
import com.open.ssme.R;
import com.open.ssme.Objects.ListGateComplexPref;
import com.open.ssme.Objects.Settings;
import com.open.ssme.Utils.Constants;
import com.open.ssme.Utils.PrefUtils;
import com.open.ssme.Utils.Constants.GateStatus;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import static com.open.ssme.Utils.Constants.ALMOST_INTERVAL;
import static com.open.ssme.Utils.Constants.ASK_SMS_PREMISSION;
import static com.open.ssme.Utils.Constants.DEFAULT_ACTIVE_COEFFICIENT;
import static com.open.ssme.Utils.Constants.DEFAULT_CHECK_GPS;
import static com.open.ssme.Utils.Constants.DEFAULT_CHECK_WIFI_TASK;
import static com.open.ssme.Utils.Constants.DEFAULT_LOCATION_INTERVAL;
import static com.open.ssme.Utils.Constants.DEFAULT_RUN_SERVICE_TASK;
import static com.open.ssme.Utils.Constants.DISTANCE_MATRIX_SUFFIX;
import static com.open.ssme.Utils.Constants.DRIVING_SPEED;
import static com.open.ssme.Utils.Constants.GOOGLE_MATRIX_API_REQ_TIME;
import static com.open.ssme.Utils.Constants.NOW;
import static com.open.ssme.Utils.Constants.ONE_SECONDS;
import static com.open.ssme.Utils.Constants.ONWAY_INTERVAL;
import static com.open.ssme.Utils.Constants.RESTART_SERVICE;
import static com.open.ssme.Utils.Constants.UPDATE_INTERVAL;


public class OpenSSMEService extends Service implements GoogleMatrixRequest.Geo,Observer {
    private final String TAG = OpenSSMEService.class.getSimpleName();
    private int counter = -5;
    private static boolean doTerminate;
    public static boolean mCodeBlocker;
    private NotificationManager mNotificationManager;
    private long mGPSUpdateInterval = 0;
    private long mWhenToDispatch = 0;
    private Constants.LocationType mCurrentLocationType = Constants.LocationType.SINGLE_UPDATE;
    //private LocationHelper mLocationHelper;
    private PendingIntent mActivityIntent, mCallGateIntent, mStopServiceIntent;
    private boolean mIsWifiOn;
    private Timer mTimer;

    public OpenSSMEService() {
        super();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "=====Service is Starting...=====");
        InitService();
        return START_STICKY;
    }



    private void InitService() {

        //mLocationHelper = new LocationHelper(this);
        StartLocationHelper();
        mTimer = new Timer();

        mCodeBlocker = false;
        doTerminate = false;

        InitNotificationIntent();

        setUpNotificationManager();

        InitServiceNotification();

        Worker();


    }

    private void setUpNotificationManager() {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    }



    private void InitNotificationIntent() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addCategory(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.setClass(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mActivityIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        //building and attaching the stop button.
        Intent StopService = new Intent(this, NotificationStopService.class);
        mStopServiceIntent = mActivityIntent.getBroadcast(this, 0, StopService, 0);

        // building and attaching the phone Icon ImageView.
        Intent CallGate = new Intent(this, NotificationCallGate.class);
        mCallGateIntent = mActivityIntent.getBroadcast(this, 0, CallGate, 0);
    }

    private NotificationCompat.Builder BuildNotification() {
        // Building the notification
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.openssme_logo) // notification icon
                .setContentTitle(getString(R.string.app_name)) // notification title
                .setContentText(getString(R.string.running)) // content text
                .setContentIntent(mActivityIntent)
                .setColor(ContextCompat.getColor(this, R.color.PrimaryColor))
                .addAction(R.drawable.ic_call_black_18dp, getString(R.string.call) + " " + ListGateComplexPref.getInstance().getClosestGate().gateName, mCallGateIntent)
                .addAction(R.drawable.ic_power_settings_new_black_18dp, getString(R.string.switch_off), mStopServiceIntent)
                .setOngoing(true);
    }

    void InitServiceNotification() {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            startForeground(Constants.FOREGROUND_SERVICE, BuildNotification().build());
        else
            Toast.makeText(this, getString(R.string.higher_ver), Toast.LENGTH_LONG).show();
    }

    private void UpdateNotification() {
        mNotificationManager.notify(Constants.FOREGROUND_SERVICE, BuildNotification().build());

    }

    /**
     * Called when user clicks the "play/pause" button on the on-going system Notification.
     */
    public static class NotificationStopService extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Terminate(context);
        }
    }

    /**
     * Called when user clicks the "skip" button on the on-going system Notification.
     */
    public static class NotificationCallGate extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            MakeTheCall(context,false);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"=====On Destroy=====");
        super.onDestroy();
        destroy();
        stopUpdates();
        RestartService();
    }

    private void stopUpdates() {
        if (mTimer!=null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

    private void RestartService() {
        if (!doTerminate) {
            Log.d(TAG, "Restarting OpenSSME Service");
            Intent broadcastIntent = new Intent(RESTART_SERVICE);
            sendBroadcast(broadcastIntent);
        }
    }

    public void Worker() {
        Log.d(TAG, "=====Starting Worker...=====");
        mTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                DoRun();
            }},NOW, DEFAULT_RUN_SERVICE_TASK);

    }

    private static void Terminate(Context context) {
        doTerminate = true;
        Intent stop = new Intent(context, OpenSSMEService.class);
        context.stopService(stop);
    }

    private void DoRun(){
        //indicate that the service is running
        Log.d(TAG, "" + (counter++));

        //there are no gates stop this service
        if (ListGateComplexPref.getInstance().gates.size() == 0) {
            Log.d(TAG, "No Gates Found, Stopping Location Service");
            stopSelf();
            return;
        }

        if (counter % DEFAULT_CHECK_WIFI_TASK == 0)
            WifiStatus();


        if (!mIsWifiOn) {

            if (getLocation() != null) {

                if (!mCodeBlocker) {

                    CalcGatesDistanceAndETA();

                    SetGateStatus();

                    ActiveGate();

                    UpdateNotification();

                    UpdateIntervalAlgorithm();

                    ListViewBroadcast();

                    //if (counter % GOOGLE_MATRIX_API_REQ_TIME == 0)
                    //DistanceMatrixRequest();


                }
            }
            else{
                if (counter%DEFAULT_CHECK_GPS==0)
                    ChangeLocationRequest(UPDATE_INTERVAL,NOW);
                //GetSingleLocationRequest();
            }

        }
        else {
            if (isLocationUpdatesOn()) {
                mGPSUpdateInterval = 0;
                StopLocationUpdates();

            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public static void MakeTheCall(Context context, boolean sound) {

        if (Settings.getInstance().isSound() && sound)
            PlayNotificationSound(context);

        Intent intent = new Intent(Intent.ACTION_CALL);

        intent.setData(Uri.parse("tel:" + ListGateComplexPref.getInstance().getClosestGate().phone));

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, context.getString(R.string.request_call), Toast.LENGTH_LONG).show();
            return;
        }
        context.startActivity(intent);

    }

    private void WifiStatus() {
        if (!Settings.getInstance().isWifi())
            mIsWifiOn = false;
        else
            mIsWifiOn = SingleShotLocationProvider.isWifiConnected(getApplicationContext());
    }

    private void CalcGatesDistanceAndETA() {

        for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {
            Location GateLocation = ListGateComplexPref.getInstance().gates.get(i).Location;
            ListGateComplexPref.getInstance().gates.get(i).distance = new Double(getLocation().distanceTo(GateLocation));
            double distance = ListGateComplexPref.getInstance().gates.get(i).distance / 1000; //to Km
            double time = (distance / DRIVING_SPEED) * 60; //to minutes
            ListGateComplexPref.getInstance().gates.get(i).ETA = time;
        }
        ListGateComplexPref.getInstance().sort();
    }

    private void SetGateStatus() {
        //inside gate radius
        if (ListGateComplexPref.getInstance().getClosestGate().distance <= Settings.getInstance().getOpen_distance()) {
            ListGateComplexPref.getInstance().getClosestGate().status = GateStatus.HOME;

            //inside the gps distance
        } else if (ListGateComplexPref.getInstance().getClosestGate().ETA <= Settings.getInstance().getGps_distance()) {
            ListGateComplexPref.getInstance().getClosestGate().status = GateStatus.ALMOST;

            //on the way
        } else
            ListGateComplexPref.getInstance().getClosestGate().status = GateStatus.ONWAY;

    }

    private void ActiveGate() {
        //active the gate if the open distance is * 2
        for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {
            if (ListGateComplexPref.getInstance().gates.get(i).distance > Settings.getInstance().getOpen_distance() * DEFAULT_ACTIVE_COEFFICIENT)
                ListGateComplexPref.getInstance().gates.get(i).active = true;
        }
    }

    public void UpdateIntervalAlgorithm() {

        long nextUpdateInterval = DEFAULT_LOCATION_INTERVAL;
        long WhenToDispatch = ONE_SECONDS;

        if (ListGateComplexPref.getInstance().getClosestGate().active) {

            //away, set the next update to to ETA / 2
            if (ListGateComplexPref.getInstance().getClosestGate().status == GateStatus.ONWAY) {
                nextUpdateInterval = ONWAY_INTERVAL;
                WhenToDispatch = (long) ((ListGateComplexPref.getInstance().getClosestETA() / DEFAULT_ACTIVE_COEFFICIENT));
            }
            //on the way x minutes before the gate, start massive GPS request
            else if (ListGateComplexPref.getInstance().getClosestGate().status == GateStatus.ALMOST) {
                nextUpdateInterval = ALMOST_INTERVAL;
                WhenToDispatch = NOW;
            }
            //arrived to the gate, deactivate gate
            else {
                OpenSSME();
            }

        }

        SetUpInterval(nextUpdateInterval, WhenToDispatch);

    }

    private void SetUpInterval(long interval, long WhenToDispatch) {

        if (interval != mGPSUpdateInterval || mWhenToDispatch != WhenToDispatch) {
            mGPSUpdateInterval = interval;
            MapBroadcast();
            mWhenToDispatch = WhenToDispatch;
            WriteToLog();
            ChangeLocationRequest(mGPSUpdateInterval, WhenToDispatch);

        }
    }

    private void WriteToLog() {
        Log.d(TAG, "====Status====");
        Log.d(TAG, "Status: " +ListGateComplexPref.getInstance().getClosestGate().status.toString());
        Log.d(TAG, "====Settings====");
        Log.d(TAG, "GPS Open Distance: " + Settings.getInstance().getGps_distance());
        Log.d(TAG, "Gate Radius Open Distance: " + Settings.getInstance().getOpen_distance());
        Log.d(TAG, "====Location====");
        Log.d(TAG, "Location Updates Is: " + isLocationUpdatesOn());
        Log.d(TAG, "Next Location Update: " + mWhenToDispatch / 1000 + " Seconds");
        Log.d(TAG, "Interval: " + mGPSUpdateInterval / 1000 + " Seconds");
        Log.d(TAG, "Last Location Update: " + getLastLocationUpdate());
        Log.d(TAG, "=====Gate Details=====");
        Log.d(TAG, "Gate Name: " + ListGateComplexPref.getInstance().getClosestGate().gateName);
        Log.d(TAG, "Distance To Gate: " + Floor(ListGateComplexPref.getInstance().getClosestGate().distance));
        Log.d(TAG, "ETA: " + Floor(ListGateComplexPref.getInstance().getClosestGate().ETA));
        Log.d(TAG, "Active: " + ListGateComplexPref.getInstance().getClosestGate().active);
        Log.d(TAG, "Speed: " + getSpeed());
    }


    private void OpenSSME() {
        ListGateComplexPref.getInstance().getClosestGate().active = false;
        MakeTheCall(this,true);
        Log.d(TAG, "=====OpenSSME=====");
        Log.d(TAG, "Make The Call To " + ListGateComplexPref.getInstance().getClosestGate().phone);
        PrefUtils.setCurrentGate(ListGateComplexPref.getInstance(), getApplicationContext());

        if (Settings.getInstance().isTerminate()) {
            ExitApplication();
        }

    }

    private void ExitApplication() {
        doTerminate = true;
        stopSelf();
        ExitActivity.exitApplication(getApplicationContext());
    }

    private void ListViewBroadcast() {
        Intent intent = new Intent(Constants.DATA_CHANGED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void MapBroadcast() {
        Intent intent = new Intent(Constants.STATUS_CHANGED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private double Floor(double value) {
        return Math.floor(value * 100) / 100;
    }


    private void DistanceMatrixRequest() {
        String destinations = "";
        String origins = getLatitude() + "," + getLongitude();
        Iterator<Gate> iterator = ListGateComplexPref.getInstance().gates.iterator();

        while (iterator.hasNext()) {
            Gate current = iterator.next();
            destinations += "|" + current.location.latitude + "," + current.location.longitude;
        }
        String url = DISTANCE_MATRIX_SUFFIX +
                "units=metric&" +
                "origins=" + origins + "&" +
                "destinations=" + destinations.substring(1) + "&" +
                "key=" + getResources().getString(R.string.google_api_key);
        new GoogleMatrixRequest(OpenSSMEService.this).execute(url);
    }

    @Override
    public void setGoogleMatrixResponse(GoogleMatrixResponse response) {
        List<String> distance = new ArrayList<>();
        List<String> duration = new ArrayList<>();

        try {
            distance = response.getDistance();
            duration = response.getDuration();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        if (distance.size() == duration.size() && distance.size() == ListGateComplexPref.getInstance().gates.size()) {
            for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {
                ListGateComplexPref.getInstance().gates.get(i).setGoogleDistance(distance.get(i));
                ListGateComplexPref.getInstance().gates.get(i).setGoogleETA(duration.get(i));
            }

        }

    }

    private static void PlayNotificationSound(Context context) {
        Uri defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(context, defaultRingtoneUri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });
            mediaPlayer.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //=======================================LOCATION HELPER=====================================

    private GoogleConnection mGoogleConnection;
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


    public void StartLocationHelper() {

        InitRunnable();
        mLocationHandler = new Handler();
        mGoogleConnection = GoogleConnection.getInstance(getApplicationContext());
        ChangeLocationRequest(UPDATE_INTERVAL,NOW);
        InitLocationManager();

    }

    private void InitRunnable() {
        mLocationRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"Fired Handler In : " + new SimpleDateFormat("hh:mm:ss").format(new Date()));
                StartLocationUpdates();
            }
        };
    }


    private void reCheckLocation(final long WhenToDispatch) {
        mLocationHandler.postDelayed(mLocationRunnable, WhenToDispatch);
        Log.d(TAG,"Asked For Handler In: " + new SimpleDateFormat("hh:mm:ss").format(new Date()) +
                " Dispatch Time: " + new SimpleDateFormat("hh:mm:ss").format(new Date(System.currentTimeMillis() + WhenToDispatch)) + "=====");
    }



    public void GetSingleLocationRequest() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                SingleReq();
            }
        });
    }

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


    private void SingleReq(){
        SingleShotLocationProvider.getSingleUpdate(getApplicationContext(), 60000, new SingleShotLocationProvider.LocationCallback() {
            @Override
            public void timedOut() {
                Log.d(TAG,"timedOut");
            }

            @Override
            public void WifiOn() {

            }

            @Override
            public void onLocationChanged(Location location) {
                SetLocationData(location);
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
        });
    }

    private void InitLocationManager() {
        mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
    }

    private boolean IsGpsActive() {
        return mLocationManager != null ? mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) : false;
    }


    private void StartLocationUpdates() {
        if (mGoogleConnection != null && mGoogleConnection.getGoogleApiClient().isConnected()) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleConnection.getGoogleApiClient(), mLocationRequest, mLocationCallback,null);
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
    }


    private void SetLocationData(Location location) {
        Log.d(TAG, "=====Location Changed In " + new SimpleDateFormat("hh:mm:ss").format(new Date()) +"=====");
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