package com.open.ssme.Receiver;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.open.ssme.Helpers.ScheduleHelper;
import com.open.ssme.Objects.ListGateComplexPref;
import com.open.ssme.Objects.Settings;
import com.open.ssme.Service.OpenSSMEService;

import static com.open.ssme.Utils.Constants.END_SERVICE;
import static com.open.ssme.Utils.Constants.START_SERVICE;

/**
 * Created by nirb on 20/02/2017.
 */

public class AlarmBroadcastReceiver extends BroadcastReceiver {
    private final String TAG = AlarmBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Settings.getInstance().isSchedule()){
            Log.d(TAG,"END_SERVICE");
            ScheduleHelper.getInstance(context).StopOpenSSMEService();

        }
    }

}
