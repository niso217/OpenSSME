package com.example.root.openssme.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.root.openssme.OpenSSMEService;
import com.example.root.openssme.R;
import com.example.root.openssme.SocialNetwork.Gate;
import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.Utils.PrefUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by nir on 20/02/2016.
 */
public class GateAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private Context context;
    private ArrayList<Gate> gates;
    private OpenSSMEService mLocationService;

    public GateAdapter(Context context) {

        ListGateComplexPref.getInstance().sort();

        this.gates = ListGateComplexPref.getInstance().gates;

        inflater = LayoutInflater.from(context);

        this.context = context;


    }

    public void UpdateBaseAdaper(ArrayList<Gate> gates){
        this.gates = gates;
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return gates.size();
    }

    @Override
    public Object getItem(int position) {
        return gates.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        Gate gate = (Gate) getItem(position);
        if (view == null) {
            view = inflater.inflate(R.layout.gate_list, null);

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (position < ListGateComplexPref.getInstance().gates.size())
                    {
                        OpenSSMEService.mCodeBlocker = true;
                        ListGateComplexPref.getInstance().gates.remove(position);
                        ListGateComplexPref.getInstance().sort();
                        PrefUtils.setCurrentGate(ListGateComplexPref.getInstance(),context);
                        notifyDataSetChanged();
                        OpenSSMEService.mCodeBlocker = false;


                    }

                    return false;
                }

            });
        }


        File photo = new File(gates.get(position).imagePath);


        TextView name = (TextView) view.findViewById(R.id.name);
        name.setText(gate.gateName);
        TextView phone = (TextView) view.findViewById(R.id.phone);
        phone.setText(gate.phone);
        TextView distance = (TextView) view.findViewById(R.id.distance);
        distance.setText(Math.floor(gate.distance * 0.001 * 100) / 100 + " Km");
        TextView ETA = (TextView) view.findViewById(R.id.ETA);
        ETA.setText(Math.floor(gate.ETA  *100) /100 + " Minutes");

        ImageView movieurl = (ImageView) view.findViewById(R.id.imageView);
            Picasso.with(context)
                    .load(photo)
                    .resize(200, 200)
                    .centerCrop()
                    .into(movieurl);



        return view;
    }


}
