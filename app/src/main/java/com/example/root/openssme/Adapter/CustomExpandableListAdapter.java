package com.example.root.openssme.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import com.example.root.openssme.Listener.SwipeDismissListViewTouchListener;
import com.example.root.openssme.Objects.ExpandableListDataPump;
import com.example.root.openssme.Objects.ListGateComplexPref;
import com.example.root.openssme.R;
import com.example.root.openssme.Service.OpenSSMEService;
import com.example.root.openssme.Utils.PrefUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static com.example.root.openssme.Utils.Constants.STRING_DIVIDER;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {

    private final String TAG = CustomExpandableListAdapter.class.getSimpleName();

    private Context context;
    private List<String> expandableListTitle;
    private LinkedHashMap<String, List<String>> expandableListDetail;
    private ExpandableListView mExpandableListView;

    public CustomExpandableListAdapter(Context context, List<String> expandableListTitle,
                                       LinkedHashMap<String, List<String>> expandableListDetail,ExpandableListView listView) {
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
            convertView = layoutInflater.inflate(R.layout.list_item, null);
        }
        TextView expandedListTextView = (TextView) convertView.findViewById(R.id.expandedListItem);
        ImageView expandedListImageView = (ImageView) convertView.findViewById(R.id.imageView);

        switch(expandedListPosition)
        {
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
        String [] data_array = data.split(STRING_DIVIDER);
        String title = data_array[0];
        String image_path = data_array[1];



        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_group, null);

            mExpandableListView.setOnTouchListener(new SwipeDismissListViewTouchListener(
                    mExpandableListView,
                    new SwipeDismissListViewTouchListener.DismissCallbacks() {
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
                                Log.d(TAG,"onDismiss " + position);
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setMessage("Do you want to remove?");
                                builder.setCancelable(false);
                                builder.setPositiveButton("Yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                DeletePosition(position);
                                            }
                                        });
                                builder.setNegativeButton("No",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                                AlertDialog alertDialog = builder.create();
                                alertDialog.show();
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

        if (position==0 || position < ListGateComplexPref.getInstance().gates.size()) {
            OpenSSMEService.mCodeBlocker = true;
            ListGateComplexPref.getInstance().gates.remove(position);
            ListGateComplexPref.getInstance().sort();
            PrefUtils.setCurrentGate(ListGateComplexPref.getInstance(), context);
            OpenSSMEService.mCodeBlocker = false;

        }
        notifyChange();
    }

    public void notifyChange(){
        expandableListDetail = ExpandableListDataPump.getData();
        expandableListTitle = new ArrayList(expandableListDetail.keySet());
        notifyDataSetChanged();
    }
}