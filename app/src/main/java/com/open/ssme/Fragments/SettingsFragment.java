package com.open.ssme.Fragments;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.ShareActionProvider;

import com.open.ssme.Listener.SettingsChangedListener;
import com.open.ssme.R;
import com.open.ssme.Utils.PermissionsUtil;
import com.open.ssme.Utils.PrefUtils;

import java.util.List;

import static com.open.ssme.Utils.Constants.PREF_UPDATE_CATEGORY;
import static com.open.ssme.Utils.Constants.SETTINGS_REQ_SMS;
import static com.open.ssme.Utils.Constants.SOCIAL;

/**
 * Created by niso2 on 01/06/2016.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();
    private PreferenceCategory mPreferenceCategory;
    private SwitchPreferenceCompat mPreferenceSocial;
    public static SettingsChangedListener mSettingsChangedListener;


    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        //add xml
        addPreferencesFromResource(R.xml.preferences);

        mPreferenceCategory = (PreferenceCategory) findPreference(PREF_UPDATE_CATEGORY);
        mPreferenceSocial = (SwitchPreferenceCompat) findPreference(SOCIAL);

        mSettingsChangedListener = new SettingsChangedListener() {
            @Override
            public void SMSChanged() {
                SetUpSMS();

            }
        };

    }

    private void SetUpSMS() {
        boolean SMS = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        if (!SMS) {
            mPreferenceSocial.setChecked(false);

            if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_SMS)) {
                mPreferenceSocial.setEnabled(false);
                mPreferenceSocial.setSummary(getString(R.string.please_fix));
            } else
                mPreferenceSocial.setEnabled(true);
        } else {
            mPreferenceSocial.setEnabled(true);
            mPreferenceSocial.setSummary(getString(R.string.social_summary));

        }

        PrefUtils.setSettings(getContext(), mPreferenceSocial.isChecked());


    }

    @Override
    public void onResume() {
        super.onResume();
        //unregister the preferenceChange listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        SetUpSMS();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SOCIAL) && sharedPreferences.getBoolean(SOCIAL, false) == true)
            requestPermissions();
        else
            PrefUtils.getSettings(getContext());

    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);

    }

    private void requestPermissions() {
        List<String> unGranted = PermissionsUtil.getInstance(getActivity()).checkPermissions(Manifest.permission.READ_SMS);
        if (unGranted.size() != 0)
            PermissionsUtil.getInstance(getActivity()).requestPermissions(unGranted, SETTINGS_REQ_SMS);
    }


}
