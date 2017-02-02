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
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.root.openssme.Adapter.CustomExpandableListAdapter;
import com.example.root.openssme.Adapter.GateAdapter;
import com.example.root.openssme.Objects.ExpandableListDataPump;
import com.example.root.openssme.R;
import com.example.root.openssme.Utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GateListFragmentNew extends Fragment implements
        ExpandableListView.OnGroupExpandListener,
        ExpandableListView.OnGroupCollapseListener,
        ExpandableListView.OnChildClickListener {

    private GateAdapter mAdapter;
    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;
    List<String> expandableListTitle;
    HashMap<String, List<String>> expandableListDetail;
    private int lastExpandedPosition = -1;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            //expandableListAdapter

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

        expandableListView = (ExpandableListView) (rootFragment).findViewById(R.id.expandableListView);
        expandableListDetail = ExpandableListDataPump.getData();
        expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
        expandableListAdapter = new CustomExpandableListAdapter(getContext(), expandableListTitle, expandableListDetail,expandableListView);
        expandableListView.setAdapter(expandableListAdapter);
        expandableListView.setOnGroupExpandListener(this);
        expandableListView.setOnGroupCollapseListener(this);
        expandableListView.setOnChildClickListener(this);


        // [END customize_button]
        return rootFragment;
    }



    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

        return false;
    }

    @Override
    public void onGroupCollapse(int groupPosition) {

    }

    @Override
    public void onGroupExpand(int groupPosition) {
        if (lastExpandedPosition != -1
                && groupPosition != lastExpandedPosition) {
            expandableListView.collapseGroup(lastExpandedPosition);
        }
        lastExpandedPosition = groupPosition;
    }
}
