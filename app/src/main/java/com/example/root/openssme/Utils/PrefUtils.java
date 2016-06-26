package com.example.root.openssme.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.SocialNetwork.Settings;
import com.example.root.openssme.SocialNetwork.User;

/**
 * Created by nir on 16/03/2016.
 */

public class PrefUtils {

    public static void setCurrentUser(User currentUser, Context ctx){
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "user_prefs", 0);
        complexPreferences.putObject("current_user_value", currentUser);
        complexPreferences.commit();
    }

    public static User getCurrentUser(Context ctx){
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "user_prefs", 0);
        User currentUser = complexPreferences.getObject("current_user_value", User.class);
        return currentUser;
    }


    public static void clearCurrentUser(Context ctx){
        User.getInstance().clear();
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "user_prefs", 0);
        complexPreferences.clearObject();
        complexPreferences.commit();
    }

    public static void getSettings(Context ctx){
        SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(ctx);
        String gps_distance = pref.getString(Constants.START_LOCATAION_UPDATE,"3");
        String open_distance = pref.getString(Constants.OPEN_DISTANCE,"150");
        String map_type = pref.getString(Constants.MAP_TYPE,"1");
        String profile = pref.getString(Constants.MAP_TYPE,"1");
        String service_provider = pref.getString(Constants.SERVICE_PROVIDER,"1");
        boolean follow = pref.getBoolean(Constants.FOLLOW_ME,false);
        boolean screen = pref.getBoolean(Constants.SCREEN,false);
        boolean first_run = pref.getBoolean(Constants.FIRST_RUN,true);



        Settings.getInstance().setGps_distance(Integer.parseInt(gps_distance));
        Settings.getInstance().setOpen_distance(Integer.parseInt(open_distance));
        Settings.getInstance().setMap_type(Integer.parseInt(map_type));
        Settings.getInstance().setService_provider(Integer.parseInt(service_provider));
        Settings.getInstance().setProfile(profile);
        Settings.getInstance().setFollow_me(follow);
        Settings.getInstance().setScreen(screen);
        Settings.getInstance().setFirst_run(first_run);


    }

    public static void setSettings(Context ctx) {
        SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(ctx);
        pref.edit().putBoolean(Constants.FIRST_RUN, false).commit();

    }

        public static void setCurrentGate(ListGateComplexPref currentGateList, Context ctx){
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "gate_prefs", 0);
        complexPreferences.putObject("gate_list", currentGateList);
        complexPreferences.commit();
    }

    public static ListGateComplexPref getCurrentGate(Context ctx){
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "gate_prefs", 0);
        ListGateComplexPref gatelist = complexPreferences.getObject("gate_list", ListGateComplexPref.class);
        return gatelist;
    }





}
