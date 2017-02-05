package com.open.ssme.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.open.ssme.Listener.SwipeToDeleteListener;
import com.open.ssme.Objects.GateDataPump;
import com.open.ssme.Objects.ListGateComplexPref;
import com.open.ssme.R;
import com.open.ssme.Service.OpenSSMEService;
import com.open.ssme.Utils.PrefUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.open.ssme.Utils.Constants.STRING_DIVIDER;

public class CustomGateAdapter extends BaseExpandableListAdapter {

    private final String TAG = CustomGateAdapter.class.getSimpleName();

    private Context context;
    private List<String> expandableListTitle;
    private LinkedHashMap<String, List<String>> expandableListDetail;
    private ExpandableListView mExpandableListView;

    public CustomGateAdapter(Context context, List<String> expandableListTitle,
                             LinkedHashMap<String, List<String>> expandableListDetail, ExpandableListView listView) {
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
        this.mExpandableListView = listView;
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                .get(expandedListPosition);
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final String expandedListText = (String) getChild(listPosition, expandedListPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.gate_list_item, null);
        }
        TextView expandedListTextView = (TextView) convertView.findViewById(R.id.expandedListItem);
        ImageView expandedListImageView = (ImageView) convertView.findViewById(R.id.imageView);

        switch (expandedListPosition) {
            case 0:
                expandedListImageView.setImageResource(R.drawable.ic_call_black_48dp);
                break;
            case 1:
                expandedListImageView.setImageResource(R.drawable.ic_query_builder_black_48dp);
                break;
            case 2:
                expandedListImageView.setImageResource(R.drawable.ic_near_me_black_48dp);
                break;
        }

        expandedListTextView.setText(expandedListText);

        convertView.setEnabled(false);

        return convertView;
    }

    @Override
    public int getChildrenCount(int listPosition) {
        return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                .size();
    }

    @Override
    public Object getGroup(int listPosition) {
        return this.expandableListTitle.get(listPosition);
    }

    @Override
    public int getGroupCount() {
        return this.expandableListTitle.size();
    }

    @Override
    public long getGroupId(int listPosition) {
        return listPosition;
    }

    @Override
    public View getGroupView(final int listPosition, final boolean isExpanded,
                             View convertView, ViewGroup parent) {

        String data = (String) getGroup(listPosition);
        String[] data_array = data.split(STRING_DIVIDER);
        String title = data_array[0];
        String image_path = data_array[1];


        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.gate_list_group, null);

            mExpandableListView.setOnTouchListener(new SwipeToDeleteListener(
                    mExpandableListView,
                    new SwipeToDeleteListener.DismissCallbacks() {
                        @Override
                        public boolean canDismiss(int position) {
                            long packedPosition = mExpandableListView.getExpandableListPosition(position);
                            int itemType = ExpandableListView.getPackedPositionType(packedPosition);
                            /*  if group item clicked */
                            if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                                return true;
                            }
                            return false;
                        }

                        @Override
                        public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                            for (final int position : reverseSortedPositions) {
                                Log.d(TAG, "onDismiss " + position);
                                new AlertDialog.Builder(context)
                                        .setIcon(android.R.drawable.ic_menu_delete)
                                        .setTitle(context.getResources().getString(R.string.delete))
                                        .setMessage(context.getResources().getString(R.string.do_delete) +" " +
                                                ListGateComplexPref.getInstance().gates.get(position).gateName)
                                        .setPositiveButton(context.getResources().getString(R.string.remove), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                DeletePosition(position);

                                            }
                                        })
                                        .setNegativeButton(context.getResources().getString(R.string.cancel), null)
                                        .show();
                            }
                        }
                    }));


        }
        TextView listTitleTextView = (TextView) convertView.findViewById(R.id.listTitle);
        listTitleTextView.setTypeface(null, Typeface.BOLD);
        listTitleTextView.setText(title);


        ImageView image = (ImageView) convertView.findViewById(R.id.imageView);
        Picasso.with(context)
                .load(new File(image_path))
                .resize(200, 200)
                .centerCrop()
                .into(image);


        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }

    private void DeletePosition(int position) {

        if (position == 0 || position < ListGateComplexPref.getInstance().gates.size()) {
            OpenSSMEService.mCodeBlocker = true;
            ListGateComplexPref.getInstance().gates.remove(position);
            ListGateComplexPref.getInstance().sort();
            PrefUtils.setCurrentGate(ListGateComplexPref.getInstance(), context);
            OpenSSMEService.mCodeBlocker = false;

        }
        notifyChange();
    }

    public void notifyChange() {
        expandableListDetail = GateDataPump.getData();
        expandableListTitle = new ArrayList(expandableListDetail.keySet());
        notifyDataSetChanged();
    }
}