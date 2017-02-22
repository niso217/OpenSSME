package com.open.ssme.Objects;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Created by root on 02/05/16.
 */
public class ListGateComplexPref {



    public ArrayList<Gate> gates;

    private static ListGateComplexPref ourInstance = new ListGateComplexPref();

    public static ListGateComplexPref getInstance() {
        return ourInstance;
    }

    public ListGateComplexPref(){
        this.gates = new ArrayList<Gate>();

    }

    public Gate getClosestGate()
    {
        return gates.get(0);
    }

    public boolean isGateExist(String phone){
        Iterator<Gate> iterator = gates.iterator();
        while (iterator.hasNext()) {
            Gate current = iterator.next();
            if (current.phone.equals(phone)) return true;
        }
        return false;
    }

    public void ChangeGatePosition(Marker marker){
        for (int i = 0; i < gates.size(); i++) {
            if (gates.get(i).phone.equals(marker.getTitle())){
                gates.get(i).setLocation(marker.getPosition());
                return;
            }
        }
    }


    public void clear(){
        ourInstance = new ListGateComplexPref();
    }

    public void sort(){
        Collections.sort(gates, new Comparator<Gate>() {
            @Override
            public int compare(Gate o1, Gate o2) {
                return o1.distance.compareTo(o2.distance);
            }
        });
    }

    public void copy(ListGateComplexPref listGateComplexPref){

        for (int i = 0; i <listGateComplexPref.gates.size(); i++) {
            gates.add(listGateComplexPref.gates.get(i));
        }
    }

    public double getClosestETA(){
        return gates.get(0).ETA * 60 * 1000;
    }

    public double getClosestDistance(){
        return gates.get(0).distance;
    }


}
