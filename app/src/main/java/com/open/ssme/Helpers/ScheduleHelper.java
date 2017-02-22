package com.open.ssme.Helpers;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.open.ssme.Objects.ListGateComplexPref;
import com.open.ssme.Objects.Settings;
import com.open.ssme.Receiver.AlarmBroadcastReceiver;
import com.open.ssme.Service.OpenSSMEService;

import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by nirb on 21/02/2017.
 */
public class ScheduleHelper {

    public static Context mContext;
    private OpenSSMEService mOpenSSMEService;
    private static AlarmManager mAlarmManager;
    private static PendingIntent mPendingIntent_start,mPendingIntent_end;
    private static final String TAG = ScheduleHelper.class.getSimpleName();


    private static ScheduleHelper ourInstance = new ScheduleHelper();

    public static ScheduleHelper getInstance(Context context) {
        if (mContext == null) {
            mContext = context;
            mAlarmManager = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
            mPendingIntent_start = PendingIntent.getService(mContext,0, new Intent(mContext, OpenSSMEService.class), 0);
            mPendingIntent_end = PendingIntent.getBroadcast(mContext,0, new Intent(mContext, AlarmBroadcastReceiver.class), 0);
        }

        return ourInstance;
    }

    private ScheduleHelper() {
    }


    public void ScheduleStartTime() {
        // reset previous pending intent
        mAlarmManager.cancel(mPendingIntent_start);
        int [] time = Settings.getInstance().getInteger_Start_time();
        int hours = time[0];
        int mins = time[1];
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, mins);
        calendar.set(Calendar.SECOND, 0);

        // if the scheduler date is passed, move scheduler time to tomorrow
        if (System.currentTimeMillis() > calendar.getTimeInMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        setAlarm(calendar,mPendingIntent_start);
    }

    private void setAlarm(Calendar calendar, PendingIntent intent){
        mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, intent);
    }

    public void ScheduleEndTime() {
        // reset previous pending intent
        mAlarmManager.cancel(mPendingIntent_end);

        int [] time = Settings.getInstance().getInteger_End_time();
        int hours = time[0];
        int mins = time[1];
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, mins);
        calendar.set(Calendar.SECOND, 0);

        // if the scheduler date is passed, move scheduler time to tomorrow
        if (System.currentTimeMillis() > calendar.getTimeInMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        setAlarm(calendar,mPendingIntent_end);
    }


    public void cancelAlarm(){
        // Cancel alarms
        try {
            mAlarmManager.cancel(mPendingIntent_end);
            mAlarmManager.cancel(mPendingIntent_start);
        } catch (Exception e) {
            Log.e(TAG, "AlarmManager update was not canceled. " + e.toString());
        }
    }


    public void StopOpenSSMEService() {
        if (ListGateComplexPref.getInstance().gates != null && ListGateComplexPref.getInstance().gates.size() > 0) {

            mOpenSSMEService = new OpenSSMEService();
            Intent ServiceIntent = new Intent(mContext, mOpenSSMEService.getClass());
            if (isMyServiceRunning(mOpenSSMEService.getClass())) {
                mContext.stopService(ServiceIntent);
            }
        }
    }
    public void StartOpenSSMEService() {
        if (ListGateComplexPref.getInstance().gates != null && ListGateComplexPref.getInstance().gates.size() > 0) {

            mOpenSSMEService = new OpenSSMEService();
            Intent ServiceIntent = new Intent(mContext, mOpenSSMEService.getClass());
            if (!isMyServiceRunning(mOpenSSMEService.getClass())) {
                mContext.startService(ServiceIntent);
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d(TAG, "is OpenSSME Service Running: " + true);
                return true;
            }
        }
        Log.d(TAG, "is OpenSSME Service Running: " + false);
        return false;
    }
}
