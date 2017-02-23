package com.open.ssme.Fragments;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TimePicker;

import com.open.ssme.R;
import com.open.ssme.Utils.PrefUtils;
import com.open.ssme.Utils.TimePreference;

import static com.open.ssme.Utils.Constants.END_TIME;
import static com.open.ssme.Utils.Constants.START_TIME;

/**
 * The Dialog for the {@link TimePreference}.
 *
 * @author Jakob Ulbrich
 */
public class TimePreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    /**
     * The TimePicker widget
     */
    private TimePicker mStartTime;
    private TimePicker mEndTime;


    /**
     * Creates a new Instance of the TimePreferenceDialogFragment and stores the key of the
     * related Preference
     *
     * @param key The key of the related Preference
     * @return A new Instance of the TimePreferenceDialogFragment
     */
    public static TimePreferenceDialogFragmentCompat newInstance(String key) {
        final TimePreferenceDialogFragmentCompat
                fragment = new TimePreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mStartTime = (TimePicker) view.findViewById(R.id.start_time);
        mEndTime = (TimePicker) view.findViewById(R.id.end_time);


        // Exception: There is no TimePicker with the id 'edit' in the dialog.
        if (mStartTime == null || mEndTime==null) {
            throw new IllegalStateException("Dialog view must contain a TimePicker with id 'edit'");
        }

        // Get the time from the related Preference
        String minutesAfterMidnight = null;


        DialogPreference preference = getPreference();
        if (preference instanceof TimePreference) {
            minutesAfterMidnight = ((TimePreference) preference).getTime();
        }

        if (minutesAfterMidnight!=null && minutesAfterMidnight.contains(":")){
            int numbers [] = toInteger(minutesAfterMidnight);
            int start_hours = numbers[0] / 60;
            int start_minutes = numbers[0] % 60;
            boolean is24hour = DateFormat.is24HourFormat(getContext());

            mStartTime.setIs24HourView(is24hour);
            mStartTime.setCurrentHour(start_hours);
            mStartTime.setCurrentMinute(start_minutes);


            int end_hours = numbers[1] / 60;
            int end_minutes = numbers[1] % 60;

            mEndTime.setIs24HourView(is24hour);
            mEndTime.setCurrentHour(end_hours);
            mEndTime.setCurrentMinute(end_minutes);
        }
        else if (minutesAfterMidnight!=null){
            int value = Integer.valueOf(minutesAfterMidnight);
            int start_hours = value / 60;
            int start_minutes = value % 60;
            boolean is24hour = DateFormat.is24HourFormat(getContext());

            mStartTime.setIs24HourView(is24hour);
            mStartTime.setCurrentHour(start_hours);
            mStartTime.setCurrentMinute(start_minutes);


            int end_hours = start_hours;
            int end_minutes = start_minutes;

            mEndTime.setIs24HourView(is24hour);
            mEndTime.setCurrentHour(end_hours);
            mEndTime.setCurrentMinute(end_minutes);
        }

    }

    private int [] toInteger(String str){
        String[] stringArray = str.split(":");
        int[] intArray = new int[stringArray.length];
        for (int i = 0; i < stringArray.length; i++) {
            String numberAsString = stringArray[i];
            intArray[i] = Integer.parseInt(numberAsString);
        }
        return intArray;
    }

    /**
     * Called when the Dialog is closed.
     *
     * @param positiveResult Whether the Dialog was accepted or canceled.
     */
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            // Get the current values from the TimePicker
            int start_hours;
            int start_minutes;
            int end_hours;
            int end_minutes;
            if (Build.VERSION.SDK_INT >= 23) {
                start_hours = mStartTime.getHour();
                start_minutes = mStartTime.getMinute();
                end_hours = mEndTime.getHour();
                end_minutes = mEndTime.getMinute();
            } else {
                start_hours = mStartTime.getCurrentHour();
                start_minutes = mStartTime.getCurrentMinute();
                end_hours = mEndTime.getCurrentHour();
                end_minutes = mEndTime.getCurrentMinute();
            }

            // Generate value to save
            int start_minutesAfterMidnight = (start_hours * 60) + start_minutes;
            int end_minutesAfterMidnight = (end_hours * 60) + end_minutes;

            String time = start_minutesAfterMidnight +":"+end_minutesAfterMidnight;

            // Save the value
            DialogPreference preference = getPreference();
            if (preference instanceof TimePreference) {
                TimePreference timePreference = ((TimePreference) preference);
                // This allows the client to ignore the user value.
                if (timePreference.callChangeListener(time)) {
                    // Save the value
                    PrefUtils.setTime(getContext(),START_TIME,start_minutesAfterMidnight);
                    PrefUtils.setTime(getContext(),END_TIME,end_minutesAfterMidnight);
                    timePreference.setTime(time);



                }
            }
        }
    }
}
