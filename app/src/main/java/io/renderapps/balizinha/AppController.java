package io.renderapps.balizinha;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by joel
 * on 12/12/17.
 */
public class AppController extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
