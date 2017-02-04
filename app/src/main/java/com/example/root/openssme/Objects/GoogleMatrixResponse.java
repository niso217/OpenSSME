package com.example.root.openssme.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by niso on 03/02/2017.
 */

public class GoogleMatrixResponse {

    private String status;
    private String[] destination_addresses;
    private String[] origin_addresses;
    private Item[] rows;



    public List<String> getDistance()
    {
        List<String> result = new ArrayList<>();
        for (int i = 0; i <rows[0].elements.length ; i++) {
            String distance = rows[0].elements[i].distance.text;
            result.add(distance);
        }
        return result;
    }

    public List<String> getDuration()
    {
        List<String> result = new ArrayList<>();
        for (int i = 0; i <rows[0].elements.length ; i++) {
            String duration = rows[0].elements[i].duration.text;
            result.add(duration);
        }
        return result;
    }

    class Item {
        private Element[] elements;

    }

    class Element{
        Duration duration;
        Duration distance;
        String status;
    }

    class Duration{
        String text;
        String value;
    }

}