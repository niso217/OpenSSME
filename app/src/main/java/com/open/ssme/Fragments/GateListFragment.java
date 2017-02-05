package com.open.ssme.Fragments;


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
import android.widget.ExpandableListView;

import com.open.ssme.Adapter.CustomGateAdapter;
import com.open.ssme.Objects.GateDataPump;
import com.open.ssme.R;
import com.open.ssme.Utils.Constants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class GateListFragment extends Fragment implements
        ExpandableListView.OnGroupExpandListener,
        ExpandableListView.OnGroupCollapseListener,
        ExpandableListView.OnChildClickListener {

    ExpandableListView expandableListView;
    CustomGateAdapter expandableListAdapter;
    List<String> expandableListTitle;
    LinkedHashMap<String, List<String>> expandableListDetail;
    private int lastExpandedPosition = -1;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            expandableListAdapter.notifyChange();

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        UnRegisterReceiver();
    }

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
        expandableListDetail = GateDataPump.getData();
        expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
        expandableListAdapter = new CustomGateAdapter(getContext(), expandableListTitle, expandableListDetail,expandableListView);
        expandableListView.setAdapter(expandableListAdapter);
        expandableListView.setOnGroupExpandListener(this);
        expandableListView.setOnGroupCollapseListener(this);
        expandableListView.setOnChildClickListener(this);

        expandableListView.setEmptyView((rootFragment).findViewById(R.id.emptyElement));

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

    private void UnRegisterReceiver() {
        if (mMessageReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
            mMessageReceiver = null;
        }
    }
}
