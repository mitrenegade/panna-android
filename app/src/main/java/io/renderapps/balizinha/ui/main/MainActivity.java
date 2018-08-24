package io.renderapps.balizinha.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.stripe.android.PaymentConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.service.notification.NotificationService;
import io.renderapps.balizinha.ui.calendar.CalendarFragment;
import io.renderapps.balizinha.ui.league.LeagueFragment;
import io.renderapps.balizinha.ui.profile.SetupProfileActivity;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.service.CloudService;
import io.renderapps.balizinha.ui.login.LoginActivity;
import io.renderapps.balizinha.util.CommonUtils;
import io.renderapps.balizinha.util.Constants;

public class MainActivity extends AppCompatActivity {
    protected final String STRIPE_KEY_PROD =
            "pk_live_IziZ9EDk1374oI3rXjEciLBG";
    protected final String STRIPE_KEY_DEV =
            "pk_test_YYNWvzYJi3bTyOJi2SNK3IkE";

    // Remote Config keys
    private static final String UPDATE_KEY = "newestVersionAndroid";

    private AlertDialog mAlertDialog;
    private FragmentManager fragmentManager;
    private BottomNavigationView navigation;
    private Player player;

    // firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseUser firebaseUser;
    private ValueEventListener valueEventListener;
    private DatabaseReference databaseRef;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment fragment;
            Class fragmentClass;

            switch (item.getItemId()) {
                case R.id.navigation_league:
                    fragmentClass = LeagueFragment.class;
                    break;
                case R.id.navigation_map:
                    fragmentClass = MapFragment.class;
                    break;
                case R.id.navigation_calendar:
                    fragmentClass = CalendarFragment.class;
                    break;
                default:
                    fragmentClass = MapFragment.class;
            }
            try {
                fragment = (Fragment) fragmentClass.newInstance();
                replaceFragment(fragment);
                item.setChecked(true);
                setTitle(item.getTitle());
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PaymentConfiguration.init(STRIPE_KEY_PROD);

        // auth listener
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        firebaseUser = mAuth.getCurrentUser();
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser == null) {
                    // user authentication no longer valid
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                    overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                } else {
                    CommonUtils.syncEndpoints(databaseRef, firebaseUser.getUid());
                    validateCustomerId();
                }
            }
        };

        // setup bottom navigation
        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        loadMapFragment();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authListener);

        // is payment enabled
        initRemoteConfig();

        // fetch current user
        fetchCurrentUser();

        NotificationService.storeFcmToken();
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() == 1) {
            // exit activity / app
            finish();
        } else if (fragmentManager.getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authListener != null) {
            mAuth.removeAuthStateListener(authListener);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // allow fragments to handle permissions
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseRef != null && firebaseUser != null && valueEventListener != null)
            databaseRef.child("players").child(firebaseUser.getUid()).removeEventListener(valueEventListener);
    }

    /**************************************************************************************************
     * Bottom navigation view control and back stack navigation management
     *************************************************************************************************/

    public void replaceFragment (Fragment fragment){
        String backStateName =  fragment.getClass().getName();
        boolean fragmentPopped = fragmentManager.popBackStackImmediate (backStateName, 0);
        // fragment not in back stack - create
        if (!fragmentPopped && fragmentManager.findFragmentByTag(backStateName) == null){
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.navigation_content, fragment, backStateName);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(backStateName);
            ft.commit();
        }
    }

    public void loadMapFragment(){
        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    public void onBackStackChanged() {
                        // Update UI
                        updateUI();
                    }
                });
        try {
            Fragment home = MapFragment.class.newInstance();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.navigation_content, home, home.getClass().getName());
            ft.addToBackStack( home.getClass().getName());
            ft.commit();
            navigation.setSelectedItemId(R.id.navigation_map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        Fragment fragmentAfterBackPress = getCurrentFragment();
        if (fragmentAfterBackPress == null) return;

        String fragTag = fragmentAfterBackPress.getTag();
        String[] fullPath = fragTag.split("\\.");
        String currentFrag = fullPath[fullPath.length - 1]; //get fragment name full path

        int id;
        switch (currentFrag) {
            case "LeagueFragment":
                id = R.id.navigation_league;
                break;
            case "MapFragment":
                id = R.id.navigation_map;
                break;
            case "CalendarFragment":
                id = R.id.navigation_calendar;
                break;
            default:
                id = R.id.navigation_map;
        }

        MenuItem menuItem = navigation.getMenu().findItem(id);
        if (!menuItem.isChecked()) {
            menuItem.setChecked(true);
            menuItem.setTitle(menuItem.getTitle());
        }
    }

    private Fragment getCurrentFragment() {
        if (fragmentManager.getBackStackEntryCount() != 0) {
            String fragmentTag = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName();
            return fragmentManager.findFragmentByTag(fragmentTag);
        }
        return null;
    }

    /**************************************************************************************************
     * Firebase
     *************************************************************************************************/

    public void fetchCurrentUser(){
        final Context mContext = this;
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    player = dataSnapshot.getValue(Player.class);

                    // set device os
                    if (player.getOs() == null || player.getOs().isEmpty()
                            || !player.getOs().equals(getString(R.string.os_android))) {
                        databaseRef.child("players").child(firebaseUser.getUid()).child("os")
                                .setValue(getString(R.string.os_android));
                    }

                    // set app version
                    if (player.getVersion() == null || player.getVersion().isEmpty()
                            || !player.getVersion().equals(Constants.APP_VERSION)){
                        databaseRef.child("players").child(firebaseUser.getUid()).child("version")
                                .setValue(Constants.APP_VERSION);
                    }
                } else {
                    // player did not register successfully with facebook
                    for (UserInfo info : firebaseUser.getProviderData()) {
                        if (info.getProviderId().equals("facebook.com")) {
                            LoginManager.getInstance().logOut();
                            // send user to login and re-auth with Facebook
                            mAuth.signOut();
                            startActivity(new Intent(mContext, LoginActivity.class));
                            finish();
                        } else {
                            // redirect user to registration
                            startActivity(new Intent(mContext, SetupProfileActivity.class));
                            finish();
                        }
                    }
                    overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        databaseRef.child("players").child(firebaseUser.getUid()).addValueEventListener(valueEventListener);
    }

    public void validateCustomerId(){
        if (firebaseUser == null)
            return;
        databaseRef.child("stripe_customers")
                .child(firebaseUser.getUid())
                .child("customer_id")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists() || dataSnapshot.getValue() == null){
                            new CloudService(new CloudService.ProgressListener() {
                                @Override
                                public void onStringResponse(String string) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(string);
                                        final String id = jsonObject.getString("customer_id");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).validateStripeCustomer(firebaseUser.getUid(), firebaseUser.getEmail());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
    }

    public Player getCurrentUser(){
        return player;
    }

    private void initRemoteConfig(){
        int cacheExpiration = Constants.CACHE_EXPIRATION; // 2 hrs
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    mFirebaseRemoteConfig.activateFetched();
                }
                showUpdateAvailable();
            }
        });
    }

    private void showUpdateAvailable(){
        final SharedPreferences sharedPref =
                getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        boolean showUpdate = sharedPref.getBoolean(Constants.PREF_SHOW_UPDATES_KEY, true);
        long elapsedTime = sharedPref.getLong(Constants.PREF_ELAPSED_TIME, 0);

        // user has checked to never see updates again
        if (!showUpdate)
            return;

        // show updates in only 12hr intervals
        long differenceInMillis = System.currentTimeMillis() - elapsedTime;
        long differenceInHours = TimeUnit.MILLISECONDS.toHours(differenceInMillis);
        if (differenceInHours < 12)
            return;

        // get update version if available
        final String updateVersion = mFirebaseRemoteConfig.getString(UPDATE_KEY);
        if (updateVersion == null || updateVersion.isEmpty())
            return;

        // no need to show update if user has latest version
        if (updateVersion.equals(Constants.APP_VERSION))
            return;

        final String updateMsg = "There is a newer version " + updateVersion
                + " of Balizinha available in the Play Store.";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Available");
        builder.setCancelable(false);

        // set view
        View v = getLayoutInflater().inflate(R.layout.dialog_app_update, null);
        builder.setView(v);
        final CheckBox hideUpdates = v.findViewById(R.id.update_checkbox);
        ((TextView)v.findViewById(R.id.update_message)).setText(updateMsg);
        // response
        builder.setPositiveButton(
                "Open in Play Store",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        openPlayStore();
                        editor.putLong(Constants.PREF_ELAPSED_TIME, new Date().getTime());
                        editor.commit();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(
                "Later",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        if (hideUpdates.isChecked()){
                            // write to user preferences
                            editor.putBoolean(Constants.PREF_SHOW_UPDATES_KEY, false);
                            editor.commit();
                            dialog.dismiss();
                        } else {
                            editor.putLong(Constants.PREF_ELAPSED_TIME, new Date().getTime());
                            editor.commit();
                            dialog.dismiss();
                        }
                    }
                });

        if (isDestroyed() || isFinishing())
            return;
        builder.create().show();
    }

    void openPlayStore(){
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException e) {
            // open in default browser if play store app not installed
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public void showProcessingPayment(){
        if (isDestroyed() || isFinishing()) return;

        final Context mContext = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setCancelable(false);

                LayoutInflater inflater = getLayoutInflater();
                builder.setView(inflater.inflate(R.layout.dialog_processing_payment, null));
                mAlertDialog = builder.create();
                mAlertDialog.show();
            }
        });
    }

    public void hideProcessingPayment(){
        if (mAlertDialog != null){
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
    }
}
