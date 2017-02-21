package com.open.ssme.Fragments;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SeekBarPreference;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.widget.Toast;

import com.open.ssme.Helpers.ScheduleHelper;
import com.open.ssme.Listener.SettingsChangedListener;
import com.open.ssme.Objects.ListGateComplexPref;
import com.open.ssme.Objects.Settings;
import com.open.ssme.R;
import com.open.ssme.Receiver.AlarmBroadcastReceiver;
import com.open.ssme.Service.OpenSSMEService;
import com.open.ssme.Utils.PermissionsUtil;
import com.open.ssme.Utils.PrefUtils;
import com.open.ssme.Utils.TimePreference;

import java.util.Calendar;
import java.util.List;

import static android.content.Context.ALARM_SERVICE;
import static com.open.ssme.Utils.Constants.END_TIME;
import static com.open.ssme.Utils.Constants.FIRST_RUN;
import static com.open.ssme.Utils.Constants.OPEN_DISTANCE;
import static com.open.ssme.Utils.Constants.PREF_UPDATE_CATEGORY;
import static com.open.ssme.Utils.Constants.SCHEDULE;
import static com.open.ssme.Utils.Constants.SETTINGS_REQ_SMS;
import static com.open.ssme.Utils.Constants.SOCIAL;
import static com.open.ssme.Utils.Constants.START_TIME;
import static com.open.ssme.Utils.Constants.START_SERVICE;
import static com.open.ssme.Utils.Constants.END_SERVICE;


/**
 * Created by niso2 on 01/06/2016.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();
    private PreferenceCategory mPreferenceCategory;
    private SwitchPreferenceCompat mPreferenceSocial, mPreferenceFirstRun, mPreferenceSchedule;
    private SeekBarPreference mSeekBarPreference;
    private TimePreference start_time, end_time;
    public static SettingsChangedListener mSettingsChangedListener;
    private ScheduleHelper mScheduleHelper;


    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        //add xml
        addPreferencesFromResource(R.xml.preferences);

        mScheduleHelper = ScheduleHelper.getInstance(getContext());

        mPreferenceCategory = (PreferenceCategory) findPreference(PREF_UPDATE_CATEGORY);
        mPreferenceSocial = (SwitchPreferenceCompat) findPreference(SOCIAL);
        mPreferenceFirstRun = (SwitchPreferenceCompat) findPreference(FIRST_RUN);
        mPreferenceSchedule = (SwitchPreferenceCompat) findPreference(SCHEDULE);
        mSeekBarPreference = (SeekBarPreference) findPreference(OPEN_DISTANCE);

        mSeekBarPreference.setMin(50);
        mPreferenceFirstRun.setVisible(false);

        start_time = (TimePreference) findPreference(START_TIME);
        end_time = (TimePreference) findPreference(END_TIME);
        start_time.setSummary(Settings.getInstance().getStart_time());
        end_time.setSummary(Settings.getInstance().getEnd_time());

        SetUpPreferenceSchedule();
        SetUpSettingsChangedListener();


    }

    private void SetUpPreferenceSchedule(){
        if (ListGateComplexPref.getInstance().gates.size() == 0) {
            mPreferenceSchedule.setEnabled(false);
            mPreferenceSchedule.setChecked(false);
            mPreferenceSchedule.setSummary(R.string.no_gates_settings);
        } else {
            mPreferenceSchedule.setEnabled(true);
            mPreferenceSchedule.setSummary(R.string.schedule_summary);

        }
    }

    private void SetUpSettingsChangedListener(){
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
                mPreferenceSocial.setEnabled(true);
            } else {
                mPreferenceSocial.setEnabled(false);
                mPreferenceSocial.setSummary(getString(R.string.please_fix));
            }

        } else {
            mPreferenceSocial.setEnabled(true);
            mPreferenceSocial.setSummary(getString(R.string.social_summary));

        }

        PrefUtils.getSettings(getContext());


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {

        // Try if the preference is one of our custom Preferences
        DialogFragment dialogFragment = null;
        if (preference instanceof TimePreference) {
            // Create a new instance of TimePreferenceDialogFragment with the key of the related
            // Preference
            dialogFragment = TimePreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }


        if (dialogFragment != null) {
            // The dialog was created (it was one of our custom Preferences), show the dialog for it
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference" +
                    ".PreferenceFragment.DIALOG");
        } else {
            // Dialog creation could not be handled here. Try with the super method.
            super.onDisplayPreferenceDialog(preference);
        }

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
    public void onDestroy() {
        super.onDestroy();
        //mScheduleHelper.cancelAlarm();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SOCIAL) && sharedPreferences.getBoolean(SOCIAL, false) == true)
            requestPermissions();
        else
            PrefUtils.getSettings(getContext());


        if (mPreferenceSchedule.isChecked()) {

            mScheduleHelper.StopOpenSSMEService();

            if (key.equals(START_TIME)) {
                start_time.setSummary(Settings.getInstance().getStart_time());
            }
            if (key.equals(END_TIME)) {
                end_time.setSummary(Settings.getInstance().getEnd_time());
            }


            if (Settings.getInstance().getStart() != 0 && Settings.getInstance().getEnd() != 0) {
                mScheduleHelper.ScheduleStartTime();
                mScheduleHelper.ScheduleEndTime();
                Toast.makeText(getContext(),Settings.getInstance().getStart_time() +" - "+
                                Settings.getInstance().getEnd_time(),
                        Toast.LENGTH_SHORT).show();
            }

        } else {
            mScheduleHelper.cancelAlarm();
            mScheduleHelper.StartOpenSSMEService();

        }

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
        else
            PrefUtils.getSettings(getContext());

    }


}
