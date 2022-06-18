package in.app.m2mvideocall.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    public static final String SP_BOOST_STATE = "SP_BOOST_STATE";
    public static final String SP_BOOST_EXPIRE_TIME = "SP_BOOST_EXPIRE_TIME";
    public static final String SP_BOOST_COUNT_IN_DAY = "SP_BOOST_COUNT_IN_DAY";
    public static final String SP_IS_BACKWARD_COMPAT = "SP_IS_BACKWARD_COMPAT";

    public static void setPreferences(Context context, String key, String value){
        SharedPreferences.Editor editor = context.getSharedPreferences("hornok", Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getPreferences(Context context, String key){
        SharedPreferences sharedPreferences = context.getSharedPreferences("hornok", Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, "0");
    }

    public static void setLongPreferences(Context context, String key, long value){
        SharedPreferences.Editor editor = context.getSharedPreferences("hornok", Context.MODE_PRIVATE).edit();
        editor.putLong(key, value);
        editor.commit();
    }

    /*Get long values from shared preferences*/
    public static long getLongPreferences(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences("hornok",	Context.MODE_PRIVATE);
        long position = prefs.getLong(key, 0);
        return position;
    }

    /*Set int values in shared preferences*/
    public static void setIntPreferences(Context context, String key, int value){
        SharedPreferences.Editor editor = context.getSharedPreferences("hornok", Context.MODE_PRIVATE).edit();
        editor.putInt(key, value);
        editor.commit();
    }

    /*Get int values from shared preferences*/
    public static int getIntPreferences(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences("hornok",Context.MODE_PRIVATE);
        int position = prefs.getInt(key, 0);
        return position;
    }
}
