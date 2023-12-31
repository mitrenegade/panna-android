package io.renderapps.balizinha.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.stripe.android.PaymentConfiguration;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.renderapps.balizinha.BuildConfig;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.service.FirebaseService;
import io.renderapps.balizinha.service.ShareService;
import io.renderapps.balizinha.service.notification.NotificationService;
import io.renderapps.balizinha.ui.calendar.CalendarFragment;
import io.renderapps.balizinha.ui.league.LeagueFragment;
import io.renderapps.balizinha.ui.profile.SetupProfileActivity;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.ui.login.LoginActivity;
import io.renderapps.balizinha.util.ActivityLauncher;
import io.renderapps.balizinha.util.CommonUtils;
import io.renderapps.balizinha.util.Constants;
import io.renderapps.balizinha.util.DialogHelper;

import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;

public class MainActivity extends AppCompatActivity {
    private Handler mHandler;
    private Runnable updateRunnable;

    private FragmentManager fragmentManager;
    private BottomNavigationView navigation;
    public CompositeDisposable mCompositeDisposable;

    // firebase
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseUser firebaseUser;
    private ValueEventListener valueEventListener;
    private DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
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
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PaymentConfiguration.init(BuildConfig.STRIPE_KEY);
        mCompositeDisposable = new CompositeDisposable();
        mHandler = new Handler();

        firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(getApplicationContext(), "Please log in to continue.", Toast.LENGTH_LONG).show();
            ActivityLauncher.launchLogin(this);
            return;
        }


        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent()).addOnCompleteListener(task -> ShareService.parseUrl(this, task));

        // auth listener
        authListener = firebaseAuth -> {
            firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser == null) {
                // user authentication no longer valid
                Toast.makeText(getApplicationContext(), "Please log in to continue.", Toast.LENGTH_LONG).show();
                ActivityLauncher.launchLogin(this);
            } else {
                CommonUtils.syncEndpoints(databaseRef, firebaseUser.getUid());
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
            // exit app
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
        if (mHandler != null && updateRunnable != null)
            mHandler.removeCallbacks(updateRunnable);
        super.onDestroy();
        if (databaseRef != null && firebaseUser != null && valueEventListener != null)
            databaseRef.child("players").child(firebaseUser.getUid()).removeEventListener(valueEventListener);
        if (mCompositeDisposable != null)
            mCompositeDisposable.dispose();
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
        // Update UI
        fragmentManager.addOnBackStackChangedListener(
                this::updateUI);
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
        if (fragTag == null || fragTag.isEmpty()) return;
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
                    Player player = dataSnapshot.getValue(Player.class);

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

        databaseRef.child(REF_PLAYERS).child(firebaseUser.getUid()).addValueEventListener(valueEventListener);
    }

    private void initRemoteConfig(){
        mCompositeDisposable.add(FirebaseService.Companion.getRemoteConfig()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(remoteConfig -> {
                    // get update version if available
                    final String updateVersion = remoteConfig.getString(Constants.CONFIG_NEWEST_VERSION);
                    if (updateVersion == null || updateVersion.isEmpty())
                        return;
                    updateRunnable = () -> {
                        DialogHelper.showUpdateAvailable(MainActivity.this, updateVersion);
                    };
                    mHandler.postDelayed(updateRunnable, 10000);
                }, error -> {}));
    }
}
