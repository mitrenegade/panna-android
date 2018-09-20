package io.renderapps.balizinha.util;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import io.renderapps.balizinha.ui.account.AccountActivity;
import io.renderapps.balizinha.ui.event.organize.LeagueSelectorActivity;

public class ActivityLauncher {

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
