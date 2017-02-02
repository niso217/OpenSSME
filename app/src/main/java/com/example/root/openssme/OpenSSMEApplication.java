package com.example.root.openssme;

import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.example.root.openssme.Service.OpenSSMEService;
import com.example.root.openssme.Objects.ListGateComplexPref;
import com.example.root.openssme.Helpers.SocialNetworkHelper;
import com.example.root.openssme.Objects.User;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.Common.GoogleConnection;

import java.io.File;
import java.io.IOException;


public class OpenSSMEApplication extends Application{

    private static final String TAG = "OpenSSMEApplication";
    private SocialNetworkHelper mSocialNetworkHelper;
    private Runnable mHandlerTask;



    private static OpenSSMEApplication mInstance;
    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        mHandlerTask = new Runnable() {
            @Override
            public void run() {
                ReadLogs();

                mSocialNetworkHelper = new SocialNetworkHelper(getApplicationContext());

                //get the current shared prefernce settings and store to settings object
                PrefUtils.getSettings(getApplicationContext());

                //initialize user from share pref saved user params
                User mUser = PrefUtils.getCurrentUser(getApplicationContext());
                if (mUser != null) {
                    Log.d(TAG,"new sign in");
                    User.getInstance().copy(mUser);
                }
                if (!GoogleConnection.getInstance(getApplicationContext()).getGoogleApiClient().isConnected()){
                    GoogleConnection.getInstance(getApplicationContext()).getGoogleApiClient().connect();
                }
                //initialize user from share pref saved user params
                ListGateComplexPref mListGateComplexPref = PrefUtils.getCurrentGate(getApplicationContext());
                if (mListGateComplexPref != null) {
                    Log.d(TAG,"Retrived Array of Gate");
                    ListGateComplexPref.getInstance().copy(mListGateComplexPref);
                }
            }
        };
        mHandlerTask.run();




    }

    private void ReadLogs()
    {
        if ( isExternalStorageWritable() ) {

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/OpenSSME" );
            File logDirectory = new File( appDirectory + "/log" );
            File logFile = new File( logDirectory, "logcat" + System.currentTimeMillis() + ".txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec( "logcat -c");
                process = Runtime.getRuntime().exec( "logcat -f " + logFile + " *:S LocationService2:D MainActivity:D");
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } else if ( isExternalStorageReadable() ) {
            // only readable
        } else {
            // not accessible
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }


    public static synchronized OpenSSMEApplication getInstance() {
        return mInstance;
    }

    public static SocialNetworkHelper getSocialNetworkHelper() {
        return getInstance().mSocialNetworkHelper;
    }

    public  void stopService()
    {
        stopService(new Intent(getBaseContext(), OpenSSMEService.class));


    }


    }




