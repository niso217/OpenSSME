package com.open.ssme.Utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import com.open.ssme.Objects.ListGateComplexPref;
import com.open.ssme.Objects.Settings;
import com.open.ssme.Objects.User;

import static com.open.ssme.Utils.Constants.CURRENT_USER_VALUE;
import static com.open.ssme.Utils.Constants.DEFAULT_LOCATION_UPDATE;
import static com.open.ssme.Utils.Constants.DEFAULT_MAP_TYPE;
import static com.open.ssme.Utils.Constants.DEFAULT_OPEN_DISTANCE;
import static com.open.ssme.Utils.Constants.GATE_LIST;
import static com.open.ssme.Utils.Constants.GATE_PREFS;
import static com.open.ssme.Utils.Constants.USER_PREFS;

/**
 * Created by nir on 16/03/2016.
 */

public class PrefUtils {

    public static void setCurrentUser(User currentUser, Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, USER_PREFS, 0);
        complexPreferences.putObject(CURRENT_USER_VALUE, currentUser);
        complexPreferences.commit();
    }

    public static User getCurrentUser(Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, USER_PREFS, 0);
        User currentUser = complexPreferences.getObject(CURRENT_USER_VALUE, User.class);
        return currentUser;
    }


    public static void clearCurrentUser(Context ctx) {
        User.getInstance().clear();
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, USER_PREFS, 0);
        complexPreferences.clearObject();
        complexPreferences.commit();
    }

    public static void getSettings(Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String gps_distance = pref.getString(Constants.START_LOCATAION_UPDATE, DEFAULT_LOCATION_UPDATE);
        int open_distance = pref.getInt(Constants.OPEN_DISTANCE, DEFAULT_OPEN_DISTANCE);
        String map_type = pref.getString(Constants.MAP_TYPE, DEFAULT_MAP_TYPE);
        String profile = pref.getString(Constants.MAP_TYPE, DEFAULT_MAP_TYPE);
        boolean terminate = pref.getBoolean(Constants.TERMINATE, false);
        boolean screen = pref.getBoolean(Constants.SCREEN, false);
        boolean first_run = pref.getBoolean(Constants.FIRST_RUN, true);
        boolean social = pref.getBoolean(Constants.SOCIAL, false);
        boolean sound = pref.getBoolean(Constants.SOUND, false);
        boolean wifi = pref.getBoolean(Constants.WIFI, true);
        int start_time = pref.getInt(Constants.START_TIME, 0);
        int end_time = pref.getInt(Constants.END_TIME, 0);
        boolean schedule = pref.getBoolean(Constants.SCHEDULE, false);


        Settings.getInstance().setGps_distance(Integer.parseInt(gps_distance));
        Settings.getInstance().setOpen_distance(open_distance);
        Settings.getInstance().setMap_type(Integer.parseInt(map_type));
        Settings.getInstance().setProfile(profile);
        Settings.getInstance().setTerminate(terminate);
        Settings.getInstance().setScreen(screen);
        Settings.getInstance().setFirst_run(first_run);
        Settings.getInstance().setSocial(social);
        Settings.getInstance().setSound(sound);
        Settings.getInstance().setWifi(wifi);
        Settings.getInstance().setStart_time(start_time);
        Settings.getInstance().setEnd_time(end_time);
        Settings.getInstance().setSchedule(schedule);



    }

    public static void setFirstRun(Context ctx) {
        SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(ctx);
        pref.edit().putBoolean(Constants.FIRST_RUN, false).commit();
        getSettings(ctx);

    }

    public static void setTime(Context ctx, String key, int time) {
        SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(ctx);
        pref.edit().putInt(key, time).commit();
        getSettings(ctx);
    }

    public static void setCurrentGate(ListGateComplexPref currentGateList, Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, GATE_PREFS, 0);
        complexPreferences.putObject(GATE_LIST, currentGateList);
        complexPreferences.commit();
    }

    public static ListGateComplexPref getCurrentGate(Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, GATE_PREFS, 0);
        ListGateComplexPref gatelist = complexPreferences.getObject(GATE_LIST, ListGateComplexPref.class);
        return gatelist;
    }


}
