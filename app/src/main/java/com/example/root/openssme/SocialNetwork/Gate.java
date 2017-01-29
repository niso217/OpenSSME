package com.example.root.openssme.SocialNetwork;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.root.openssme.R;
import com.example.root.openssme.Utils.Constants;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by nir on 02/05/16.
 */
public class Gate {

    public String gateName;
    public String phone;
    public LatLng location;
    public Double ETA;
    public Double distance;
    public String imagePath;
    public boolean status;
    public boolean active;


    public Gate(String gateName, String phone, LatLng location,String imagePath) {
        this.gateName = gateName;
        this.phone = phone;
        this.location = location;
        this.ETA = Double.MAX_VALUE;
        this.distance = Double.MAX_VALUE;
        this.imagePath = imagePath;
        this.status = false; //close
        this.active = false;
    }




}
