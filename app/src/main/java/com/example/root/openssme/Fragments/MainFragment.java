package com.example.root.openssme.Fragments;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.root.openssme.OpenSSMEService;
import com.example.root.openssme.R;
import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.SocialNetwork.Settings;
import com.example.root.openssme.Utils.Constants;


/**
 * Created by niso2 on 15/05/2016.
 */
public class MainFragment extends Fragment

 {
     private TextView Gate,ETA,Distance,Radius,GPS,Google,Speed;
     private CountDownTimer mCountDownTimer;
     private long mMillisUntilFinished;
     private OpenSSMEService mLocationService;
     private final String TAG = MainFragment.class.getSimpleName();

     private Intent mServiceIntent;
     private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

         @Override
         public void onReceive(Context context, Intent intent) {

                 switch (intent.getFlags()) {

                     case Constants.DATA_UPDATE_FLAG:
                         //recived location update

                         ETA.setText(intent.getStringExtra(Constants.ETA));
                         Gate.setText(intent.getStringExtra(Constants.GATE_NAME));
                         Distance.setText(intent.getStringExtra(Constants.DISTANCE));
                         Radius.setText(intent.getStringExtra(Constants.GATE_RADIUS));
                         Speed.setText(intent.getStringExtra(Constants.SPEED));
                         GPS.setText(intent.getStringExtra(Constants.GPS));


                         break;

                 }
         }
     };



     @Override
     public void onActivityCreated(@Nullable Bundle savedInstanceState) {


         if (savedInstanceState != null && ListGateComplexPref.getInstance().gates!=null && ListGateComplexPref.getInstance().gates.size()>0) {

//             ETA.setText(Math.floor(ListGateComplexPref.getInstance().gates.get(0).ETA) + " Minutes");
//             Gate.setText(ListGateComplexPref.getInstance().gates.get(0).gateName);
//             Distance.setText(Math.floor(ListGateComplexPref.getInstance().gates.get(0).distance * 0.001 * 100) / 100 + " Km");
//             Radius.setText(ListGateComplexPref.getInstance().gates.get(0).status.status() + "");
         }



         super.onActivityCreated(savedInstanceState);
     }

     @Override
     public void onCreate(@Nullable Bundle savedInstanceState) {
         ScreenSetup();
         super.onCreate(savedInstanceState);
     }

     @Nullable
     @Override
     public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

             View rootFragment = inflater.inflate(R.layout.fragment_main, null);

             Gate = (TextView) (rootFragment).findViewById(R.id.textViewNG);
             ETA = (TextView) (rootFragment).findViewById(R.id.textViewETA);
            Speed = (TextView) (rootFragment).findViewById(R.id.textViewSpeed);

         Distance = (TextView) (rootFragment).findViewById(R.id.textViewDistance);
             Radius = (TextView) (rootFragment).findViewById(R.id.textViewRadius);
             GPS = (TextView) (rootFragment).findViewById(R.id.textViewGPS);


//         if (ListGateComplexPref.getInstance().gates!=null && ListGateComplexPref.getInstance().gates.size()>0) {
//
//             ETA.setText(ListGateComplexPref.getInstance().gates.get(0).ETA+"");
//             Gate.setText(ListGateComplexPref.getInstance().gates.get(0).gateName);
//             Distance.setText(Math.floor(ListGateComplexPref.getInstance().gates.get(0).distance * 0.001 * 100) / 100 + " Km");
//             Radius.setText(ListGateComplexPref.getInstance().gates.get(0).status + "");
//
//
//         }

         LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                 new IntentFilter(Constants.LOCATION_SERVICE_DATA));

             return rootFragment;

     }

     @Override
     public void onResume() {
         //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
         super.onResume();
     }

     @Override
    public void onStart() {
         StartOpenSSMEService();
         super.onStart();
    }

     @Override
     public void onDestroy() {
         if (mMessageReceiver != null) {
             LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
             mMessageReceiver = null;
         }
         super.onDestroy();
     }

     private void ScreenSetup(){
         if (Settings.getInstance().isScreen()) {
             getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         }
         else
             getActivity().getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
     }

    public void StartOpenSSMEService() {
        if (ListGateComplexPref.getInstance().gates!=null && ListGateComplexPref.getInstance().gates.size()>0) {

            mLocationService = new OpenSSMEService();
            mServiceIntent = new Intent(getContext(), mLocationService.getClass());
            if (!isMyServiceRunning(mLocationService.getClass())) {
                getContext().startService(mServiceIntent);
            }
        }
    }

     private boolean isMyServiceRunning(Class<?> serviceClass) {
         ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
         for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
             if (serviceClass.getName().equals(service.service.getClassName())) {
                 Log.d(TAG, "is Connection Service Running: " + true);
                 return true;
             }
         }
         Log.d(TAG, "is Connection Service Running: " + false);
         return false;
     }


 }
