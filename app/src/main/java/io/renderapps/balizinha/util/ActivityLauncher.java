package io.renderapps.balizinha.util;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.ui.account.AccountActivity;
import io.renderapps.balizinha.ui.event.organize.LeagueSelectorActivity;
import io.renderapps.balizinha.ui.login.LoginActivity;

public class ActivityLauncher {

    public static void launchLogin(AppCompatActivity activity){
        if(CommonUtils.isValidContext(activity)){
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            activity.finish();
            activity.overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
        }
    }

    public static void launchAccount(AppCompatActivity activity){
        if(CommonUtils.isValidContext(activity)){
            activity.startActivity(new Intent(activity, AccountActivity.class));
        }
    }

    public static void createGame(AppCompatActivity activity){
        if(CommonUtils.isValidContext(activity)){
            activity.startActivity(new Intent(activity, LeagueSelectorActivity.class));
        }
    }
}
