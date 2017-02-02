package com.example.root.openssme.Objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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