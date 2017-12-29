package io.renderapps.balizinha.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.fragment.AccountFragment;
import io.renderapps.balizinha.fragment.CalendarFragment;
import io.renderapps.balizinha.fragment.MapFragment;
import io.renderapps.balizinha.model.Player;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private BottomNavigationView navigation;
    private Player player;

    // firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseUser firebaseUser;
    private ValueEventListener valueEventListener;
    private DatabaseReference databaseRef;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment fragment;
            Class fragmentClass;

            switch (item.getItemId()) {
                case R.id.navigation_account:
                    fragmentClass = AccountFragment.class;
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

        // auth listener
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
        fetchCurrentUser();
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
        fragmentManager = getFragmentManager();
        fragmentManager.addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    public void onBackStackChanged() {
                        // Update your UI here.
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
        String fragTag = fragmentAfterBackPress.getTag();
        String[] fullPath = fragTag.split("\\.");
        String currentFrag = fullPath[fullPath.length - 1]; //get fragment name full path

        int id;
        switch (currentFrag) {
            case "AccountFragment":
                id = R.id.navigation_account;
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

    public void fetchCurrentUser(){
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null)
                    player = dataSnapshot.getValue(Player.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseRef.child("players").child(firebaseUser.getUid()).addValueEventListener(valueEventListener);
    }

    public Player getCurrentUser(){
        return player;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseRef != null && firebaseUser != null && valueEventListener != null)
            databaseRef.child("players").child(firebaseUser.getUid()).removeEventListener(valueEventListener);
    }
}
