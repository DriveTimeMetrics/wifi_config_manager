package org.hpsaturn.autowifi.System;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Antonio Vanegas @hpsaturn on 10/20/15.
 */
public class Storage {

    private static final String PREF_CUSTOM_MESSAGE = "custom_message";
    private static final String PREF_START_SERVICE = "first_game";

    public static void setCustomMessage(Context ctx, String message) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_CUSTOM_MESSAGE, message);
        editor.commit();
    }

    public static String getCustomMessage(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString(PREF_CUSTOM_MESSAGE, null);
    }

    public static boolean isStartService(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(PREF_START_SERVICE, false);
    }

    public static void setStartService(Context ctx, boolean isStartService) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_START_SERVICE, isStartService);
        editor.commit();
    }


    public static void clearSettings(Context ctx) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }

}
