package com.example.root.openssme;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.SocialNetwork.SocialNetworkHelper;
import com.example.root.openssme.SocialNetwork.User;
import com.example.root.openssme.Utils.PrefUtils;


public class MyApplication extends Application{

    private static final String TAG = "MyApplication";
    private SocialNetworkHelper mSocialNetworkHelper;

    private static MyApplication mInstance;
    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;
        mSocialNetworkHelper = new SocialNetworkHelper(this);

        //initialize user from share pref saved user params
        User mUser = PrefUtils.getCurrentUser(getApplicationContext());
        if (mUser != null) {
            Log.d(TAG,"new sign in");
            User.getInstance().copy(mUser);
        }

        //initialize user from share pref saved user params
        ListGateComplexPref mListGateComplexPref = PrefUtils.getCurrentGate(getApplicationContext());
        if (mListGateComplexPref != null) {
            Log.d(TAG,"Retrived Array of Gate");
            ListGateComplexPref.getInstance().copy(mListGateComplexPref);
        }


    }


    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    public static SocialNetworkHelper getSocialNetworkHelper() {
        return getInstance().mSocialNetworkHelper;
    }


    }




