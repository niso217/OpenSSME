package com.open.ssme.Fragments;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.open.ssme.R;
import com.open.ssme.Utils.PrefUtils;

import static com.open.ssme.Utils.Constants.PREF_FIRST_RUN;
import static com.open.ssme.Utils.Constants.PREF_UPDATE_CATEGORY;

/**
 * Created by niso2 on 01/06/2016.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String TAG = SettingsFragment.class.getSimpleName();


    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        //add xml
        addPreferencesFromResource(R.xml.preferences);

        PreferenceCategory category = (PreferenceCategory) findPreference(PREF_UPDATE_CATEGORY);
        Preference logout = findPreference(PREF_FIRST_RUN);
        category.removePreference(logout);


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
    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);

    }

}
