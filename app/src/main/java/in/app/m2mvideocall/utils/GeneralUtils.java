package in.app.m2mvideocall.utils;

import android.content.Context;
import android.content.Intent;

import com.bumptech.glide.Glide;

import androidx.appcompat.widget.AppCompatImageView;
import in.app.m2mvideocall.R;

public class GeneralUtils {

    public static String getUserName(Context context) {
        return SessionManager.getPreferences(context, "user_name");
    }

    public static String getUserUuid(Context context) {
        return SessionManager.getPreferences(context, "user_uuid");
    }

    public static String getUserEmail(Context context) {
        return SessionManager.getPreferences(context, "user_email");
    }

    public static String getCallSession(Context context) {
        return SessionManager.getPreferences(context, "call-session");
    }

    public static String getCallType(Context context) {
        return SessionManager.getPreferences(context, "call-type");
    }

    public static void loadUrl(String url, Context context, AppCompatImageView appCompatImageView) {
        Glide
                .with(context)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.ic_image)
                .into(appCompatImageView);
    }

    public static void shareTextToApps(String text, Context context){
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        String shareBody = "Join me on Chirpz at "+text;
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        context.startActivity(Intent.createChooser(intent, "Share via"));
    }
}
