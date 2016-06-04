package com.example.root.openssme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;

import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.Utils.PrefUtils;

/**
 * Created by niso2 on 01/06/2016.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String TAG = SettingsFragment.class.getSimpleName();


    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        //add xml
        addPreferencesFromResource(R.xml.preferences);


    }

    @Override
    public void onResume() {
        super.onResume();
        //unregister the preferenceChange listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        PrefUtils.getSettings(getContext());
        if (key.equals("map_type")) {
            Intent intent = new Intent(Constants.LOCATION_SERVICE);
            intent.addFlags(Constants.CHANGE_MAP);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
        if (key.equals("open_distance")) {
            Intent intent = new Intent(Constants.LOCATION_SERVICE);
            intent.addFlags(Constants.CHANGE_RADIUS);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }


}
