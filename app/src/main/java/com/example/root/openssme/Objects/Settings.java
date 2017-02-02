package com.example.root.openssme.Objects;

/**
 * Created by niso2 on 01/06/2016.
 */
public class Settings {

    private int gps_distance;
    private int open_distance;
    private int map_type;
    private int service_provider;
    private String profile;
    private boolean follow_me;
    private boolean screen;
    private boolean first_run;

    public boolean isFirst_run() {
        return first_run;
    }

    public void setFirst_run(boolean first_run) {
        this.first_run = first_run;
    }

    public boolean isScreen() {
        return screen;
    }

    public void setScreen(boolean screen) {
        this.screen = screen;
    }

    public boolean isFollow_me() {
        return follow_me;
    }

    public void setFollow_me(boolean follow_me) {
        this.follow_me = follow_me;
    }

    public int getService_provider() {
        return service_provider;
    }

    public void setService_provider(int service) {
        this.service_provider = service;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getProfile() {
        return profile;
    }

    public void setGps_distance(int gps_distance) {
        this.gps_distance = gps_distance;
    }

    public void setOpen_distance(int open_distance) {
        this.open_distance = open_distance;
    }

    public void setMap_type(int map_type) {
        this.map_type = map_type;
    }

    public int getGps_distance() {
        return gps_distance;
    }

    public int getOpen_distance() {
        return open_distance;
    }

    public int getMap_type() {
        return map_type;
    }

    private static Settings ourInstance = new Settings();
    

    public static Settings getInstance() {

        return ourInstance;
    }

    private Settings() {

    }
}