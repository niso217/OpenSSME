package com.example.root.openssme.Objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ExpandableListDataPump {
    public static HashMap<String, List<String>> getData() {
        ListGateComplexPref.getInstance().sort();
        HashMap<String, List<String>> expandableListDetail = new HashMap();
        Iterator<Gate> iterator = ListGateComplexPref.getInstance().gates.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            Gate current = iterator.next();
            List<String> details = new ArrayList();
            details.add(current.phone);
            details.add(current.ETA.toString());
            details.add(current.distance.toString());
            details.add(current.status.status());
            expandableListDetail.put(index++ +"", details);
        }
        return expandableListDetail;
    }
}
