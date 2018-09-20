package io.renderapps.balizinha.util;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;

import com.google.firebase.database.DatabaseReference;

import java.util.Date;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.ui.login.LoginActivity;

import static io.renderapps.balizinha.util.Constants.REF_USER_EVENTS;

/**
 * General purpose helpers
 */

public class CommonUtils {

    public static void syncEndpoints(DatabaseReference ref, String uid){
        ref.child(Constants.REF_PLAYERS).child(uid).keepSynced(true);
        ref.child(REF_USER_EVENTS).child(uid).keepSynced(true);
        ref.child("stripe_customers").child(uid).keepSynced(true);
    }

    public static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static void launchLogin(AppCompatActivity activity){
        if (!isValidContext(activity)) return;

        activity.startActivity(new Intent(activity, LoginActivity.class));
        activity.finish();
    }

    public static void showSnackbar(View root, String message){
        final Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG);
        snackbar.setActionTextColor(ContextCompat.getColor(root.getContext(), R.color.colorPrimary));
        snackbar.show();
    }

    public static boolean isValidContext(AppCompatActivity activity){
        return !(activity.isDestroyed() || activity.isFinishing());
    }

    public static long secondsToMillis(long sec){
        return sec * 1000;
    }

    public static boolean isGameOver(long millis, Date currentDate){
        final Date endDate = new Date(millis);
        return currentDate.after(endDate);
    }
}
