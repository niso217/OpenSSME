package com.example.root.openssme;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.root.openssme.Adapter.GateAdapter;
import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.SocialNetwork.Settings;
import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.common.GoogleConnection;


/**
 * Created by niso2 on 15/05/2016.
 */
public class MainFragment extends Fragment implements
        ServiceConnection
 {
     private boolean mServiceBound;
    private LocationService mLocationService;
     private TextView Gate,ETA,Distance,UpdatedBy,Radius,LastUpdate,Google;
     private CountDownTimer mCountDownTimer;
     private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

         @Override
         public void onReceive(Context context, Intent intent) {

                 switch (intent.getFlags()) {

                     case Constants.LOCATION_UPDATE_FLAG:
                         //recived location update
                         boolean connection = intent.getBooleanExtra(Constants.GOOGLE_CONNECTION, true);
                         ETA.setText(Math.floor(ListGateComplexPref.getInstance().gates.get(0).ETA * 0.0166666667 * 100) / 100 + " Minutes");
                         Gate.setText(ListGateComplexPref.getInstance().gates.get(0).gateName);
                         Distance.setText(Math.floor(ListGateComplexPref.getInstance().gates.get(0).distance * 0.001 * 100) / 100 + " Km");
                         Radius.setText(ListGateComplexPref.getInstance().gates.get(0).status + "");
                         Google.setText(connection + "");
                         CountDown(intent.getLongExtra(Constants.NEXT_UPDATE,Constants.API_REFRESH_GO));

                         break;

                 }
         }
     };

     private void CountDown(long seconds){
         if (mCountDownTimer!=null) mCountDownTimer.cancel();

         mCountDownTimer = new CountDownTimer(seconds, 1000) {

             public void onTick(long millisUntilFinished) {
                 LastUpdate.setText("" + millisUntilFinished / 1000);
             }

             public void onFinish() {
             }
         }.start();
     }

     @Override
     public void onActivityCreated(@Nullable Bundle savedInstanceState) {
             LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                     new IntentFilter(Constants.LOCATION_SERVICE));

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
         Distance = (TextView) (rootFragment).findViewById(R.id.textViewDistance);
         Radius = (TextView) (rootFragment).findViewById(R.id.textViewRadius);
         LastUpdate = (TextView) (rootFragment).findViewById(R.id.textViewLastUpdate);
         Google = (TextView) (rootFragment).findViewById(R.id.googleapi);
         (rootFragment).findViewById(R.id.buttonRefresh).setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 unBindService();
                 bindService();
             }
         });

         return rootFragment;
     }

     @Override
    public void onStart() {
        if (!mServiceBound)
        bindService();
         super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mServiceBound) {
            unBindService();
            mServiceBound = false;
        }

    }

     private void ScreenSetup(){
         if (Settings.getInstance().isScreen()) {
             getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         }
         else
             getActivity().getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
     }

    public void bindService() {
        Intent intent = new Intent(getActivity(), LocationService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, this, getActivity().BIND_AUTO_CREATE);
    }

    public void unBindService() {
        getActivity().unbindService(this);
        if (mLocationService != null) {
            mLocationService = null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        LocationService.MyBinder myBinder = (LocationService.MyBinder) binder;
        mLocationService = myBinder.getService();
        mServiceBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (mLocationService == null)
            return;
        mLocationService = null;
        mServiceBound = false;

    }


}
