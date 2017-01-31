package com.example.root.openssme.SocialNetwork;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;

import com.example.root.openssme.R;
import com.example.root.openssme.Utils.Constants;
import com.google.android.gms.maps.model.LatLng;
import com.example.root.openssme.Utils.Constants.GateStatus;

/**
 * Created by nir on 02/05/16.
 */
public class Gate {

    public String gateName;
    public String phone;
    public LatLng location;
    public Location Location;
    public Double ETA;
    public Double distance;
    public String imagePath;
    public GateStatus status;
    public boolean active;


    public Gate(String gateName, String phone, LatLng location,String imagePath) {
        this.gateName = gateName;
        this.phone = phone;
        this.location = location;
        this.Location = new Location(LatLngToLocation(location));
        this.ETA = Double.MAX_VALUE;
        this.distance = Double.MAX_VALUE;
        this.imagePath = imagePath;
        this.status = GateStatus.ALMOST; //close
        this.active = true;
    }

    public Location LatLngToLocation(LatLng latlang) {
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        loc.setLatitude(latlang.latitude);
        loc.setLongitude(latlang.longitude);
        return loc;
    }




}
