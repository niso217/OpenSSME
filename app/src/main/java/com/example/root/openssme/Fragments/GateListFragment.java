package com.example.root.openssme.Fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.root.openssme.Adapter.GateAdapter;
import com.example.root.openssme.R;
import com.example.root.openssme.Utils.Constants;

public class GateListFragment extends Fragment {

    private GateAdapter mAdapter;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            mAdapter.notifyDataSetChanged();

        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                    new IntentFilter(Constants.DATA_CHANGED));
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootFragment = inflater.inflate(R.layout.fragment_gate_list, null);

        //ListView employeeListView = (ListView) (rootFragment).findViewById(R.id.listView);
       // mAdapter = new GateAdapter(getContext(), employeeListView);
       // employeeListView.setAdapter(mAdapter);


        // [END customize_button]
        return rootFragment;
    }


}
