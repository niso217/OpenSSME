package com.open.ssme.Service;

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
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.open.ssme.Activity.ExitActivity;
import com.open.ssme.Activity.MainActivity;
import com.open.ssme.Common.GoogleConnection;
import com.open.ssme.Helpers.GoogleMatrixRequest;
import com.open.ssme.Helpers.LocationHelper;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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


public class OpenSSMEService extends Service implements GoogleMatrixRequest.Geo {
    private final String TAG = OpenSSMEService.class.getSimpleName();
    private int counter = -5;
    private static boolean doTerminate;
    public static boolean mCodeBlocker;
    private NotificationManager mNotificationManager;
    private long mGPSUpdateInterval = 0;
    private long mWhenToDispatch = 0;
    private Constants.LocationType mCurrentLocationType = Constants.LocationType.SINGLE_UPDATE;
    private LocationHelper mLocationHelper;
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

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_CALL:
                    MakeTheCall(context);
                    break;
            }
        }
    };


    private void InitService() {

        mLocationHelper = new LocationHelper(this);
        mTimer = new Timer();

        mCodeBlocker = false;
        doTerminate = false;

        InitNotificationIntent();

        setUpNotificationManager();

        RegisterReciver();

        InitServiceNotification();

        Worker();


    }

    private void setUpNotificationManager() {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    }


    private void RegisterReciver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_CALL);
        registerReceiver(receiver, filter);
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
            MakeTheCall(context);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationHelper.destroy();
        mLocationHelper = null;
        unregisterReceiver(receiver);
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
            }},0, 1000);

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

            if (mLocationHelper.getLocation() != null) {

                if (!mCodeBlocker) {

                    CalcGatesDistanceAndETA();

                    SetGateStatus();

                    ActiveGate();

                    UpdateNotification();

                    UpdateIntervalAlgorithm();

                    WriteToLog();

                    ListViewBroadcast();

                    //if (counter % GOOGLE_MATRIX_API_REQ_TIME == 0)
                    //DistanceMatrixRequest();


                }
            }
            else{
                if (counter==DEFAULT_CHECK_GPS)
                    mLocationHelper.GetSingleLocationRequest();
            }

        }
        else {
            if (mLocationHelper.isLocationUpdatesOn()) {
                mGPSUpdateInterval = 0;
                mLocationHelper.StopLocationUpdates();

            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public static void MakeTheCall(Context context) {

        if (Settings.getInstance().isSound())
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
            ListGateComplexPref.getInstance().gates.get(i).distance = new Double(mLocationHelper.getLocation().distanceTo(GateLocation));
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
        if (ListGateComplexPref.getInstance().getClosestGate().distance > Settings.getInstance().getOpen_distance() * DEFAULT_ACTIVE_COEFFICIENT)
            ListGateComplexPref.getInstance().getClosestGate().active = true;
    }

    public void UpdateIntervalAlgorithm() {

        long nextUpdateInterval = DEFAULT_LOCATION_INTERVAL;
        long WhenToDispatch = ONE_SECONDS;

        if (ListGateComplexPref.getInstance().getClosestGate().active) {

            //away, set the next update to to ETA / 2
            if (ListGateComplexPref.getInstance().getClosestGate().status == GateStatus.ONWAY) {
                Log.d(TAG, "=====Out Side GPS Open Distance=====");
                nextUpdateInterval = ONWAY_INTERVAL;
                WhenToDispatch = (long) ((ListGateComplexPref.getInstance().getClosestETA() / DEFAULT_ACTIVE_COEFFICIENT));
            }
            //on the way x minutes before the gate, start massive GPS request
            else if (ListGateComplexPref.getInstance().getClosestGate().status == GateStatus.ALMOST) {
                Log.d(TAG, "=====Massive GPS Request=====");
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
            mWhenToDispatch = WhenToDispatch;
            mLocationHelper.ChangeLocationRequest(mGPSUpdateInterval, WhenToDispatch);

        }
    }

    private void WriteToLog() {
        Log.d(TAG, "====Settings====");
        Log.d(TAG, "GPS Open Distance: " + Settings.getInstance().getGps_distance());
        Log.d(TAG, "Gate Radius Open Distance: " + Settings.getInstance().getOpen_distance());
        Log.d(TAG, "====Location====");
        Log.d(TAG, "Location Updates Is: " + mLocationHelper.isLocationUpdatesOn());
        Log.d(TAG, "Next Location Update: " + mWhenToDispatch / 1000 + " Seconds");
        Log.d(TAG, "Interval: " + mGPSUpdateInterval / 1000 + " Seconds");
        Log.d(TAG, "Last Location Update: " + mLocationHelper.getLastLocationUpdate());
        Log.d(TAG, "=====Gate Details=====");
        Log.d(TAG, "Gate Name: " + ListGateComplexPref.getInstance().getClosestGate().gateName);
        Log.d(TAG, "Distance To Gate: " + Floor(ListGateComplexPref.getInstance().getClosestGate().distance));
        Log.d(TAG, "ETA: " + Floor(ListGateComplexPref.getInstance().getClosestGate().ETA));
        Log.d(TAG, "Active: " + ListGateComplexPref.getInstance().getClosestGate().active);
        Log.d(TAG, "Speed: " + mLocationHelper.getSpeed());
    }


    private void OpenSSME() {
        ListGateComplexPref.getInstance().getClosestGate().active = false;
        MakeTheCall(this);
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

    private double Floor(double value) {
        return Math.floor(value * 100) / 100;
    }


    private void DistanceMatrixRequest() {
        String destinations = "";
        String origins = mLocationHelper.getLatitude() + "," + mLocationHelper.getLongitude();
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


}