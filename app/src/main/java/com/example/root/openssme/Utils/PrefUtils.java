package com.example.root.openssme.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
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

    public static int getSettings(Context ctx, String key){
        SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(ctx);
        String str = pref.getString(key,"1");
        return Integer.parseInt(str);

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
