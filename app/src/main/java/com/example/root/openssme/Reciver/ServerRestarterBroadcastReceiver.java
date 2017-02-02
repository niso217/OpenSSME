package com.example.root.openssme.Reciver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.root.openssme.Service.OpenSSMEService;

/**
 * Created by fabio on 24/01/2016.
 */
public class ServerRestarterBroadcastReceiver extends BroadcastReceiver {
    private final String TAG = ServerRestarterBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Service Stops!!!!!");
        context.startService(new Intent(context, OpenSSMEService.class));
    }

}
