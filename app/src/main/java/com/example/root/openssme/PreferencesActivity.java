package com.example.root.openssme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;

import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.common.GoogleConnection;

import java.util.Calendar;

public class PreferencesActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
       {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }


           @Override
           public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

               if (key.equals("map_type")) {
                   Intent intent = new Intent(Constants.LOCATION_SERVICE);
                   intent.addFlags(Constants.CHANGE_MAP);
                   LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
               }
           }
       }
