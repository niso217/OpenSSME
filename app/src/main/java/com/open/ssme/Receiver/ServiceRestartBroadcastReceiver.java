package com.open.ssme.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.open.ssme.Service.OpenSSMEService;

/**
 * Created by fabio on 24/01/2016.
 */
public class ServiceRestartBroadcastReceiver extends BroadcastReceiver {
    private final String TAG = ServiceRestartBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Service Stops!!!!!");
        context.startService(new Intent(context, OpenSSMEService.class));
    }

}
