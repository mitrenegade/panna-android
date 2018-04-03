package io.renderapps.balizinha.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.fragment.ChatFragment;
import io.renderapps.balizinha.fragment.EventDetailsFragment;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.service.FirebaseService;
import io.renderapps.balizinha.util.Constants;
import io.renderapps.balizinha.util.GeneralHelpers;

/**
 * Display game users, details and actions (messages, payments)
 */
public class EventDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    // load event from key or from bundle
    public static String EVENT_ID = "eid";
    public static String EVENT_TITLE = "title";
    public static String EVENT_LAUNCH_MODE = "didLaunchFromCalendar"; // user launched event from calendar
    public static String EVENT_STATUS = "event_status"; // user launched event from calendar
    public static String EVENT_PLAYER_STATUS = "player_status"; // user launched event from calendar

    // properties
    private Event event;
    private String eventId;
    private String title;
    private int playerCount = 0;
    private boolean didLaunchFromCalendar;
    private boolean userJoined;
    private boolean eventIsOver;
    private boolean eventUpdate = false;
    private boolean viewPagerAdded = false;

    // firebase
    private ValueEventListener valueEventListener;
    private ValueEventListener playerCountListener;
    private DatabaseReference databaseRef;
    private DatabaseReference paymentRef;
    private FirebaseUser firebaseUser;
    private FirebaseRemoteConfig remoteConfig;


    private ImageView headerImage;
    private AppBarLayout appBarLayout;
    private ViewPager mViewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fab;
    private int[] tabIcons = {R.drawable.ic_details_filled, R.drawable.ic_chat_filled};
    private int[] unselectedTabIcons = {R.drawable.ic_details, R.drawable.ic_chat};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        remoteConfig = FirebaseRemoteConfig.getInstance();

        // fetch bundle from intent
        eventId = getIntent().getStringExtra(EVENT_ID);
        title = getIntent().getStringExtra(EVENT_TITLE);
        didLaunchFromCalendar = getIntent().getBooleanExtra(EVENT_LAUNCH_MODE, false);
        eventIsOver = getIntent().getBooleanExtra(EVENT_STATUS, false);
        userJoined = getIntent().getBooleanExtra(EVENT_PLAYER_STATUS, false);

        // firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();
        paymentRef = databaseRef.child("charges").child("events")
                .child(eventId);
        paymentRef.keepSynced(true);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        appBarLayout = findViewById(R.id.app_bar_layout);
        headerImage = findViewById(R.id.header_img);
        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(title);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));

        // join leave
        fab = findViewById(R.id.joinLeaveButton);
        fab.setOnClickListener(this);
        fab.setEnabled(false);

        // fetch event
        fetchEvent();

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.viewpager);

        if (userJoined) // user is already joined
            fab.setImageResource(R.drawable.ic_leave);

        if (eventIsOver) // event is over
            fab.setVisibility(View.GONE);
        else {
            AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
            appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                @Override
                public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                    if (Math.abs(verticalOffset)-appBarLayout.getTotalScrollRange() == 0)
                        //  Collapsed
                        fab.setVisibility(View.GONE);

                    else
                        //Expanded
                        fab.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    public void fetchEvent(){
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    event = dataSnapshot.getValue(Event.class);
                    event.setEid(eventId);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fab.setEnabled(true);
                            eventUpdate = true;
                            if (event.getPhotoUrl() != null && !event.getPhotoUrl().isEmpty())
                                loadHeaderImage();
                            if (!viewPagerAdded) {
                                eventUpdate = false; // no need to update on first instance
                                setupViewPager();
                            } else {
                                // update event details if currently on that fragment
                                updateEventDetailFragment();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        playerCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int count = 0;
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    if (dataSnapshot.hasChildren()){
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            if (data.getValue(Boolean.class))
                                count++;
                        }
                    }
                    playerCount = count;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        databaseRef.child("eventUsers").child(eventId).addValueEventListener(playerCountListener);
        databaseRef.child("events").child(eventId).addValueEventListener(valueEventListener);
    }

    public void setupViewPager(){
        viewPagerAdded = true;
        ViewPagerAdapter mSectionsPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        EventDetailsFragment eventDetailsFragment = EventDetailsFragment.newInstance(event, userJoined, eventIsOver);
        mSectionsPagerAdapter.addFragment(eventDetailsFragment, "Details");
        mSectionsPagerAdapter.addFragment(ChatFragment.newInstance(eventId, title), "Chat");
        mViewPager.setAdapter(mSectionsPagerAdapter);
        setupTabBar();

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        appBarLayout.setExpanded(true);
                        if (eventUpdate)
                            updateEventDetailFragment();
                        break;
                    case 1:
                        collapseToolbar();
                        enableMessaging();
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    public void setupTabBar(){
        // setup tabs
        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        setupTabIcons();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                final int position = tab.getPosition();
                tabLayout.getTabAt(position).setIcon(tabIcons[position]);
                tabLayout.getTabAt(position).setIcon(tabIcons[position]);

                // hide keyboard
                hideSoftKeyBoard();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                final int position = tab.getPosition();
                tabLayout.getTabAt(position).setIcon(unselectedTabIcons[position]);
                tabLayout.getTabAt(position).setIcon(unselectedTabIcons[position]);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

    }

    public void setupTabIcons(){
        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(unselectedTabIcons[1]);
    }

    public void collapseToolbar(){
        if (appBarLayout != null)
            appBarLayout.setExpanded(false);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.joinLeaveButton){
            if (userJoined)
                onUserLeave();
            else {
                if (firebaseUser.getDisplayName() == null || firebaseUser.getDisplayName().isEmpty()) {
                    GeneralHelpers.showAddNameDialog(this);
                    return;
                }
                if (playerCount >= event.maxPlayers) {
                    Toast.makeText(this, getString(R.string.game_full),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (event.paymentRequired) {
                    hasUserAlreadyPaid();
                } else
                    onUserJoin();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        paymentRef.keepSynced(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseRef != null) {
            if (valueEventListener != null)
                databaseRef.child("events").child(eventId).removeEventListener(valueEventListener);
            if (playerCountListener != null)
                databaseRef.child("eventUsers").child(event.getEid()).addValueEventListener(playerCountListener);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }

    /**************************************************************************************************
     * Custom methods
     *************************************************************************************************/

    public void onUserLeave(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (!event.paymentRequired){
            builder.setMessage(getString(R.string.leave_event));
        } else {
            builder.setTitle(getString(R.string.leave_event_title));
            builder.setMessage(getString(R.string.leave_paid_event));
        }

        builder.setPositiveButton(
                "Yes, I'm sure",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        databaseRef.child("userEvents").child(firebaseUser.getUid())
                                .child(eventId).setValue(false);
                        databaseRef.child("eventUsers").child(eventId)
                                .child(firebaseUser.getUid()).setValue(false);

                        // update UI
                        fab.setImageResource(R.drawable.ic_add);
                        userJoined = false;

                        // disable messaging
                        enableMessaging();

                        // only finish activity if launched from calendar
                        if (didLaunchFromCalendar)
                            finish();
                    }
                });

        builder.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder.create().show();
    }

    public void onUserJoin(){
        databaseRef.child("userEvents").child(firebaseUser.getUid()).child(eventId).setValue(true);
        databaseRef.child("eventUsers").child(eventId).child(firebaseUser.getUid()).setValue(true);

        fab.setImageResource(R.drawable.ic_leave);
        userJoined = true;

        // allow user to now send messages in game chat
        enableMessaging();
        GeneralHelpers.showSuccessfulJoin(this);
    }

    public void updateEventDetailFragment(){
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" +
                R.id.viewpager + ":" + mViewPager.getCurrentItem());
        if (mViewPager.getCurrentItem() == 0 && page != null) {
            eventUpdate = false;
            ((EventDetailsFragment) page).updateEvent(event);
        }
    }

    public void enableMessaging(){
        // update user status to enable/disable messaging
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" +
                R.id.viewpager + ":" + mViewPager.getCurrentItem());
        if (mViewPager.getCurrentItem() == 1 && page != null)
            ((ChatFragment)page).enableMessaging();
    }

    private void hideSoftKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        assert imm != null;
        if(imm.isAcceptingText()) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    public void loadHeaderImage(){
        RequestOptions myOptions = new RequestOptions()
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_soccer_header);
        // load photo
        if (!isDestroyed() && !isFinishing()) {
            Glide.with(this)
                    .asBitmap()
                    .apply(myOptions)
                    .load(event.getPhotoUrl())
                    .into(headerImage);
        }
    }


    // getters
    public boolean getUserStatus(){
        return userJoined;
    }

    /**************************************************************************************************
     * Verify payment
     *************************************************************************************************/

    // has user paid for game already, left and is now joining again
    private void hasUserAlreadyPaid(){
        paymentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean userDidPay = false;
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()){
                    for (DataSnapshot child: dataSnapshot.getChildren()){
                        if (child.child("player_id").getValue(String.class).equals(firebaseUser.getUid())){
                            final String status = child.child("status").getValue(String.class);
                            if (status != null && status.equals("succeeded")){
                                userDidPay = true;
                                onUserJoin();
                                break;
                            }
                        }
                    }
                }
                if (!userDidPay)
                    isPaymentConfigEnabled();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void isPaymentConfigEnabled(){
        final int cacheExpiration = Constants.CACHE_EXPIRATION;
        remoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                            remoteConfig.activateFetched();
                        boolean paymentRequired = remoteConfig.getBoolean(Constants.PAYMENT_CONFIG_KEY);
                        // check if user has added a payment method
                        if (paymentRequired) {
                            checkUserPaymentMethod();
                        } else {
                            showPaymentDialog();
                        }
                    }
                });
    }

    private void checkUserPaymentMethod(){
        final Context mContext = this;
        if (firebaseUser != null) {
            FirebaseDatabase.getInstance().getReference().child("stripe_customers")
                    .child(firebaseUser.getUid()).child("source")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getValue() != null)
                                confirmPaymentDialog();
                            else
                                GeneralHelpers.showPaymentRequiredDialog(mContext);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {}
                    });
        }
    }

    private void confirmPaymentDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.confirm_payment));
        builder.setCancelable(false);

        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_layout_payment, null);
        ((TextView)view.findViewById(R.id.payment_details)).setText("Press Ok to pay $"
                .concat((String.format(Locale.getDefault(), "%.2f", event.getAmount()))).concat(" for this game."));
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        onAddCharge();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        if (!isDestroyed() && !isFinishing())
            builder.create().show();

    }

    private void showPaymentDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.payment_required_title));
        builder.setCancelable(false);

        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_layout_payment, null))
                // Add action buttons
                .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        onUserJoin();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        if (!isDestroyed() && !isFinishing())
            builder.create().show();
    }

    public void onAddCharge(){
        fab.setEnabled(false);
        if (firebaseUser != null) {
            Map<String, Object> chargeChild = new HashMap<>();
            final double price = event.getAmount() * 100;
            final int chargeAmount = (int) price;
            chargeChild.put("amount", chargeAmount);
            chargeChild.put("player_id", firebaseUser.getUid());
            final String chargeKey = paymentRef.push().getKey();
            createPaymentListener(chargeKey);

            // update
            Toast.makeText(this, getString(R.string.processing_payment),
                    Toast.LENGTH_SHORT).show();
            paymentRef.child(chargeKey).updateChildren(chargeChild);
//            FirebaseService.startActionAddCharge(this, eventId, firebaseUser.getUid(),
//                    chargeKey, chargeAmount);
        }
    }

    // listen for successful payments
    private void createPaymentListener(String chargeKey){
        paymentRef.child(chargeKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    final String status = dataSnapshot.child("status").getValue(String.class);
                    if (status != null){
                        if (isDestroyed() || isFinishing())
                            return;
                        if (status.equals("succeeded")){
                            onUserJoin();
                        } else
                            showFailedPayment();
                        fab.setEnabled(true);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showFailedPayment();
                fab.setEnabled(true);
            }
        });
    }

    public void showFailedPayment(){
        Toast.makeText(this, getString(R.string.payment_failed), Toast.LENGTH_LONG).show();
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
//            return mFragmentTitleList.get(position);
            return null;
        }
    }
}
