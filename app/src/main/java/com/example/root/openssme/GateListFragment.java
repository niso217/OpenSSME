package com.example.root.openssme;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Movie;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import com.android.volley.RequestQueue;
import com.example.root.openssme.Adapter.GateAdapter;
import com.example.root.openssme.SocialNetwork.Gate;
import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.Utils.Constants;
import com.google.android.gms.common.SignInButton;

import java.util.ArrayList;

public class GateListFragment extends Fragment {

    private GateAdapter mAdapter;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getFlags()) {


                case Constants.LOCATION_UPDATE_FLAG:
                    //recived location update
                    if (intent.hasExtra(Constants.DISTANCE)) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState==null){
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                    new IntentFilter(Constants.LOCATION_SERVICE));
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootFragment = inflater.inflate(R.layout.fragment_gate_list, null);

        mAdapter = new GateAdapter(getContext());
        ListView employeeListView = (ListView) (rootFragment).findViewById(R.id.listView);
        employeeListView.setAdapter(mAdapter);



        // [END customize_button]
        return rootFragment;
    }
}
