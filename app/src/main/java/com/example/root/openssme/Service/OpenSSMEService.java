package com.example.root.openssme.Service;

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
import android.location.Location;
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

import com.example.root.openssme.Activity.MainActivity;
import com.example.root.openssme.Helpers.LocationHelper;
import com.example.root.openssme.R;
import com.example.root.openssme.Objects.ListGateComplexPref;
import com.example.root.openssme.Objects.Settings;
import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.Utils.Constants.GateStatus;

import static com.example.root.openssme.Utils.Constants.DEFAULT_ACTIVE_COEFFICIENT;
import static com.example.root.openssme.Utils.Constants.DEFAULT_LOCATION_INTERVAL;
import static com.example.root.openssme.Utils.Constants.DEFAULT_RUN_SERVICE_TASK;
import static com.example.root.openssme.Utils.Constants.DRIVING_SPEED;
import static com.example.root.openssme.Utils.Constants.RESTART_SERVICE;
import static com.example.root.openssme.Utils.Constants.UPDATE_INTERVAL;


public class OpenSSMEService extends Service {
    private final String TAG = OpenSSMEService.class.getSimpleName();
    private int counter;
    private Runnable mHandlerTask;
    private Handler mHandler;
    private Notification mNotification;
    private static boolean doTerminate;
    public static boolean mCodeBlocker;
    private NotificationManager mNotificationManager;
    private long mGPSUpdateInterval = DEFAULT_LOCATION_INTERVAL;
    private LocationHelper mLocationHelper;
    private PendingIntent mActivityIntent,mCallGateIntent,mStopServiceIntent;

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
        mHandler = new Handler();

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

    private void InitNotificationIntent()
    {
        Intent intent = new Intent(this, MainActivity.class);
         mActivityIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //building and attaching the stop button.
        Intent StopService = new Intent(this, NotificationStopService.class);
         mStopServiceIntent = mActivityIntent.getBroadcast(this, 0, StopService, 0);

        // building and attaching the phone Icon ImageView.
        Intent CallGate = new Intent(this, NotificationCallGate.class);
         mCallGateIntent = mActivityIntent.getBroadcast(this, 0, CallGate, 0);
    }

    private NotificationCompat.Builder BuildNotification()
    {
        // Building the notification
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.gate) // notification icon
                .setContentTitle("OpenSSME") // notification title
                .setContentText("Running. Tap to open.") // content text
                .setTicker("Showing button notification") // status bar message
                .setContentIntent(mActivityIntent)
                .setColor(ContextCompat.getColor(this,R.color.teal))
                .addAction(R.drawable.ic_call_black_18dp, "CALL " +ListGateComplexPref.getInstance().getClosestGate().gateName, mCallGateIntent)
                .addAction(R.drawable.ic_power_settings_new_black_18dp, "SWITCH OFF", mStopServiceIntent)
                .setOngoing(true);
    }

    void InitServiceNotification(){

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            startForeground(Constants.FOREGROUND_SERVICE,
                    BuildNotification().build());

        } else {
            Toast.makeText(this, "You need a higher version", Toast.LENGTH_LONG).show();
        }
    }

    private void UpdateNotification()
    {
        mNotificationManager.notify(
                Constants.FOREGROUND_SERVICE,
                BuildNotification().build());

    }
    /**
     * Called when user clicks the "play/pause" button on the on-going system Notification.
     */
    public static class NotificationStopService extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            doTerminate = true;
            Intent stop = new Intent(context, OpenSSMEService.class);
            context.stopService(stop);
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
        mHandler.removeCallbacks(mHandlerTask);
        RestartService();
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
        mHandlerTask = new Runnable() {
            @Override
            public void run() {
                //indicate that the service is running
                Log.d(TAG, "" + (counter++));
                mHandler.postDelayed(mHandlerTask, DEFAULT_RUN_SERVICE_TASK);

                //there are no gates stop this service
                if (ListGateComplexPref.getInstance().gates.size() == 0) {
                    Log.d(TAG, "No Gates Found, Stopping Location Service");
                    stopSelf();
                    return;
                }

                if (mLocationHelper.getLocation() != null) {

                    if (!mCodeBlocker) {

                        CalcGatesDistanceAndETA();

                        SetGateStatus();

                        ActiveGate();

                        UpdateNotification();

                        UpdateIntervalAlgorithm();

                        WriteToLog();

                        DATABroadcast();

                        ListViewBroadcast();

                    }
                }
            }
        };
        mHandlerTask.run();
    }




    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public static void MakeTheCall(Context context) {

        Intent intent = new Intent(Intent.ACTION_CALL);

        intent.setData(Uri.parse("tel:" + ListGateComplexPref.getInstance().getClosestGate().phone));

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "No Phone Call Permission", Toast.LENGTH_LONG).show();
            return;
        }
        context.startActivity(intent);

    }


    private void CalcGatesDistanceAndETA() {

        for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++)
        {
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

        if (ListGateComplexPref.getInstance().getClosestGate().active) {

            //away, set the next update to to ETA / 2
            if (ListGateComplexPref.getInstance().getClosestGate().status == GateStatus.ONWAY) {
                Log.d(TAG, "=====Out Side GPS Open Distance=====");
                nextUpdateInterval = (long) ((ListGateComplexPref.getInstance().getClosestETA() / DEFAULT_ACTIVE_COEFFICIENT));
            }
            //on the way x minutes before the gate, start massive GPS request
            else if (ListGateComplexPref.getInstance().getClosestGate().status == GateStatus.ALMOST) {
                Log.d(TAG, "=====Massive GPS Request=====");
                nextUpdateInterval = UPDATE_INTERVAL;
            }
            //arrived to the gate, deactivate gate
            else {
                OpenSSME();
            }

            SetUpInterval(nextUpdateInterval);
        }
    }

    private void WriteToLog() {
        Log.d(TAG, "====Settings====");
        Log.d(TAG, "GPS Open Distance: " + Settings.getInstance().getGps_distance());
        Log.d(TAG, "Gate Radius Open Distance: " + Settings.getInstance().getOpen_distance());
        Log.d(TAG, "====Location====");
        Log.d(TAG, "Location Updates: " + mLocationHelper.isIsLocationUpdatesOn());
        Log.d(TAG, "=====Gate Details=====");
        Log.d(TAG, "Gate Name: " + ListGateComplexPref.getInstance().getClosestGate().gateName);
        Log.d(TAG, "Distance To Gate: " + Floor(ListGateComplexPref.getInstance().getClosestGate().distance));
        Log.d(TAG, "Next Location Update: " + mGPSUpdateInterval / 1000 + " Seconds");
        Log.d(TAG, "ETA: " + Floor(ListGateComplexPref.getInstance().getClosestGate().ETA));
        Log.d(TAG, "Active: " + ListGateComplexPref.getInstance().getClosestGate().active);
        Log.d(TAG, "Speed: " + mLocationHelper.getSpeed());
    }

    private void SetUpInterval(long interval) {
        if (interval != mGPSUpdateInterval) {
            mGPSUpdateInterval = interval;
            mLocationHelper.ChangeLocationRequest(mGPSUpdateInterval);

        }
    }

    private void OpenSSME() {
        ListGateComplexPref.getInstance().getClosestGate().active = false;
        MakeTheCall(this);
        Log.d(TAG, "=====OpenSSME=====");
        Log.d(TAG, "Make The Call To " + ListGateComplexPref.getInstance().getClosestGate().phone);
        PrefUtils.setCurrentGate(ListGateComplexPref.getInstance(), getApplicationContext());
    }

    private void DATABroadcast() {

        Intent intent = new Intent(Constants.LOCATION_SERVICE_DATA);
        intent.addFlags(Constants.DATA_UPDATE_FLAG);

        intent.putExtra(Constants.DISTANCE, Floor(ListGateComplexPref.getInstance().getClosestGate().distance / 1000) + " km");
        intent.putExtra(Constants.GPS, mLocationHelper.isIsLocationUpdatesOn() + "");
        intent.putExtra(Constants.SPEED, mLocationHelper.getSpeed() == 0 ? "No Movement" : Floor(mLocationHelper.getSpeed()) + " km/h");
        intent.putExtra(Constants.GATE_NAME, ListGateComplexPref.getInstance().getClosestGate().gateName);
        Double eta = (ListGateComplexPref.getInstance().getClosestGate().ETA*60);
        intent.putExtra(Constants.ETA, secondsToString(eta.intValue()));
        intent.putExtra(Constants.GATE_RADIUS, ListGateComplexPref.getInstance().getClosestGate().status.status());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void ListViewBroadcast() {
        Intent intent = new Intent(Constants.DATA_CHANGED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    private double Floor(double value)
    {
        return Math.floor(value*100) / 100;
    }


    private String secondsToString(int pTime) {
        return String.format("%02d:%02d", pTime / 60, pTime % 60);
    }



}