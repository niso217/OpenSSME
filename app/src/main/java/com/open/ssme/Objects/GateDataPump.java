package com.open.ssme.Objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import static com.open.ssme.Utils.Constants.STRING_DIVIDER;

public class GateDataPump {
    public static LinkedHashMap<String, List<String>> getData() {
        ListGateComplexPref.getInstance().sort();
        LinkedHashMap<String, List<String>> expandableListDetail = new LinkedHashMap();
        Iterator<Gate> iterator = ListGateComplexPref.getInstance().gates.iterator();
        while (iterator.hasNext()) {
            List<String> details = new ArrayList();
            Gate current = iterator.next();
            details.add(current.phone);
            details.add(current.getETA());
            details.add(current.getDistance());
            expandableListDetail.put(current.gateName + STRING_DIVIDER + current.getImagePath(), details);
        }
        return expandableListDetail;
    }




}
