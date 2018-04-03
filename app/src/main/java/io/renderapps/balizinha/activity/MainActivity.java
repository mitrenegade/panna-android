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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.view.PaymentMethodsActivity;

import org.json.JSONException;
import org.json.JSONObject;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.fragment.AccountFragment;
import io.renderapps.balizinha.fragment.CalendarFragment;
import io.renderapps.balizinha.fragment.MapFragment;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.service.CloudService;
import io.renderapps.balizinha.service.FirebaseService;
import io.renderapps.balizinha.util.Constants;
import io.renderapps.balizinha.util.GeneralHelpers;

public class MainActivity extends AppCompatActivity {
    public static final String STRIPE_KEY_PROD =
            "pk_live_IziZ9EDk1374oI3rXjEciLBG";
    public static final String STRIPE_KEY_DEV =
            "pk_test_YYNWvzYJi3bTyOJi2SNK3IkE";
    public static final int REQUEST_CODE_SELECT_SOURCE = 55;
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

    // stripe
    private CustomerSession customerSession;

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
                    GeneralHelpers.syncEndpoints(databaseRef, firebaseUser.getUid());
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
        isPaymentConfigEnabled();

        // fetch current user
        fetchCurrentUser();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_SOURCE && resultCode == RESULT_OK) {
            String selectedSource = data.getStringExtra(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT);

            Source source = Source.fromString(selectedSource);
            // Note: it isn't possible for a null or non-card source to be returned.
            if (source != null && Source.CARD.equals(source.getType())) {
                SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
                FirebaseService.startActionSavePayment(this, firebaseUser.getUid(), source.getId(),
                        cardData.getBrand(), cardData.getLast4());

                // update account fragment
                AccountFragment accountFragment = (AccountFragment) getFragmentManager()
                        .findFragmentById(R.id.navigation_account);
                if (accountFragment != null)
                    accountFragment.setAdapter(R.array.accountOptionsWithPayment);
            }
        }
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
        fragmentManager = getFragmentManager();
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

    /**************************************************************************************************
     * Firebase
     *************************************************************************************************/

    public void fetchCurrentUser(){
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    player = dataSnapshot.getValue(Player.class);
                    if (player.getOs() == null || player.getOs().isEmpty()
                            || !player.getOs().equals(getString(R.string.os_android)))
                        databaseRef.child("players").child(firebaseUser.getUid()).child("os")
                                .setValue(getString(R.string.os_android));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        databaseRef.child("players").child(firebaseUser.getUid()).addValueEventListener(valueEventListener);
    }

    public void validateCustomerId(){
        databaseRef.child("stripe_customers")
                .child(firebaseUser.getUid())
                .child("customer_id")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
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
                    public void onCancelled(DatabaseError databaseError) {}
                });
    }

    public Player getCurrentUser(){
        return player;
    }

    private void isPaymentConfigEnabled(){
        final int cacheExpiration = Constants.CACHE_EXPIRATION;
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mFirebaseRemoteConfig.activateFetched();
                        }
                    }
                });
    }
}
