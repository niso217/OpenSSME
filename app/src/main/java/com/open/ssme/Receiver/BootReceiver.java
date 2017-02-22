package com.open.ssme.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.open.ssme.Helpers.ScheduleHelper;

import static com.open.ssme.Utils.Constants.BOOT_COMPLETED;

 /**
 * Created by nirb on 21/02/2017.
 */

public class BootReceiver extends BroadcastReceiver {
    private final String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BOOT_COMPLETED)) {
            ScheduleHelper.getInstance(context).ScheduleStartTime();
            ScheduleHelper.getInstance(context).ScheduleEndTime();
            Log.d(TAG,"Auto Start The Schedule");
        }
    }
}