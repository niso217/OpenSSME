package com.example.root.openssme.Objects;

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
    public String googleETA;
    public String googleDistance;
    public String imagePath;
    public GateStatus status;
    public boolean active;

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getDistance() {

        if (googleDistance == null || googleDistance.equals(""))
            return String.valueOf(Floor(distance / 1000)) + " km";

        return googleDistance;
    }

    public void setGoogleDistance(String googleDistance) {
        this.googleDistance = googleDistance;
    }

    public String getETA() {
        if (googleETA == null || googleETA.equals(""))
        {
            Double eta = ETA*60;
            return ToHoursMinSec(eta.intValue());
        }


        return googleETA;    }

    public void setGoogleETA(String googleETA) {
        this.googleETA = googleETA;
    }

    public Gate(String gateName, String phone, LatLng location, String imagePath) {
        this.gateName = gateName;
        this.phone = phone;
        this.location = location;
        this.Location = new Location(LatLngToLocation(location));
        this.ETA = Double.MAX_VALUE;
        this.distance = Double.MAX_VALUE;
        this.imagePath = imagePath;
        this.status = GateStatus.ALMOST; //close
        this.active = false;
    }

    public Location LatLngToLocation(LatLng latlang) {
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        loc.setLatitude(latlang.latitude);
        loc.setLongitude(latlang.longitude);
        return loc;
    }

    private  String ToHoursMinSec(int input) {
        int hours = input / 3600;
        int minutes = (input % 3600) / 60;
        int seconds = (input % 3600) % 60;
        String result = "";
        if (hours>0)
        {
            result = hours + " hour";
            if (hours >1)
                result = result+"s ";
            result = result + "and "+ minutes +" min";
            if (minutes>1)
                result = result+"s";
        }
        else if (minutes>0){
            result =  minutes +" min";
            if (minutes>1)
                result = result+"s";
        }
        else
        {
            result =  seconds +" sec";
            if (seconds>1)
                result = result+"s";
        }



        return result;
    }

    private static double Floor(double value) {
        return Math.floor(value * 100) / 100;
    }




}
