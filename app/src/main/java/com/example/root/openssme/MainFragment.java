package com.example.root.openssme;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.root.openssme.Adapter.GateAdapter;
import com.example.root.openssme.Utils.Constants;


/**
 * Created by niso2 on 15/05/2016.
 */
public class MainFragment extends Fragment implements
        ServiceConnection
 {
     private boolean mServiceBound;
    private LocationService mLocationService;
     private TextView tv;
     private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

         @Override
         public void onReceive(Context context, Intent intent) {
             switch (intent.getFlags()) {


                 case Constants.LOCATION_UPDATE_FLAG:
                     //recived location update
                     if (intent.hasExtra(Constants.LAST_UPDATE)) {
                         int time = intent.getIntExtra(Constants.LAST_UPDATE,0);
                         tv.setText("Keep Alive: "+time);
                     }
                     break;
             }
         }
     };

     @Override
     public void onActivityCreated(@Nullable Bundle savedInstanceState) {
             LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                     new IntentFilter(Constants.LOCATION_SERVICE));

         super.onActivityCreated(savedInstanceState);
     }

     @Nullable
     @Override
     public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

         View rootFragment = inflater.inflate(R.layout.fragment_main, null);

          tv = (TextView) (rootFragment).findViewById(R.id.textViewDistance);

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
