package io.renderapps.balizinha.activity;

import android.content.DialogInterface;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.fragment.ChatFragment;
import io.renderapps.balizinha.fragment.EventDetailsFragment;
import io.renderapps.balizinha.model.Action;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    // load event from key or from bundle
    public static String EVENT_ID = "eid";
    public static String EVENT_TITLE = "title";
    public static String EVENT_TYPE = "type";
    public static String EVENT_TIME = "time";
    public static String EVENT_INFO = "info";
    public static String EVENT_CITY = "city";
    public static String EVENT_STATE = "state";
    public static String EVENT_CREATOR = "creator";
    public static String EVENT_CREATED_AT = "created_at";
    public static String EVENT_PAYMENT = "payment_required";
    public static String EVENT_PLAYER_STATUS = "userJoined"; // joined or not
    public static String EVENT_STATUS = "eventIsOver"; // upcoming or past
    public static String EVENT_LAUNCH_MODE = "didLaunchFromCalendar"; // user launched event from calendar

    // properties
    private String eventId;
    private String title;
    private String type;
    private long time;
    private double createdAt;
    private String description;
    private String creator_id;
    private String city;
    private String state;
    private boolean userJoined;
    private boolean eventIsOver;
    private boolean payment_required;
    private boolean didLaunchFromCalendar;

    // firebase
    private DatabaseReference databaseRef;
    private FirebaseUser firebaseUser;


    private CollapsingToolbarLayout collapsingToolbarLayout;
    private ViewPagerAdapter mSectionsPagerAdapter;
    private AppBarLayout appBarLayout;
    private ViewPager mViewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fab;
    private int[] tabIcons = {R.drawable.ic_details_filled, R.drawable.ic_chat_filled};
    private int[] unselectedTabIcons = {R.drawable.ic_details, R.drawable.ic_chat};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // fetch bundle from intent
        eventId = getIntent().getStringExtra(EVENT_ID);
        title = getIntent().getStringExtra(EVENT_TITLE);
        type = getIntent().getStringExtra(EVENT_TYPE);
        description = getIntent().getStringExtra(EVENT_INFO);
        city = getIntent().getStringExtra(EVENT_CITY);
        state = getIntent().getStringExtra(EVENT_STATE);
        creator_id = getIntent().getStringExtra(EVENT_CREATOR);
        userJoined = getIntent().getBooleanExtra(EVENT_PLAYER_STATUS, false);
        eventIsOver = getIntent().getBooleanExtra(EVENT_STATUS, false);
        time = getIntent().getLongExtra(EVENT_TIME, 0);
        createdAt = getIntent().getDoubleExtra(EVENT_CREATED_AT, 0);
        didLaunchFromCalendar = getIntent().getBooleanExtra(EVENT_LAUNCH_MODE, false);

        // firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        appBarLayout = findViewById(R.id.app_bar_layout);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(title);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.viewpager);
        setupViewPager();

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

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    collapseToolbar();
                    enableMessaging();
                } else
                    appBarLayout.setExpanded(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // join leave
        fab = findViewById(R.id.joinLeaveButton);
        fab.setOnClickListener(this);
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

    public void setupViewPager(){
        mSectionsPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        EventDetailsFragment eventDetailsFragment = EventDetailsFragment.newInstance(eventId, title,
                city, state, description, creator_id, type, time, userJoined, eventIsOver, payment_required);
        mSectionsPagerAdapter.addFragment(eventDetailsFragment, "Details");
        mSectionsPagerAdapter.addFragment(ChatFragment.newInstance(eventId), "Chat");
        mViewPager.setAdapter(mSectionsPagerAdapter);
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
                if (payment_required)
                    showPaymentDialog();
                else
                    onUserJoin();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }


    public void onUserLeave(){
        // dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.leave_event));
        builder.setCancelable(false);

        builder.setPositiveButton(
                "Yes, I'm sure",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        databaseRef.child("userEvents").child(firebaseUser.getUid())
                                .child(eventId).setValue(false);
                        databaseRef.child("eventUsers").child(eventId)
                                .child(firebaseUser.getUid()).setValue(false);

                        // create and add event action
                        Action action = new Action(eventId, "", createdAt,
                                Action.ACTION_LEAVE, firebaseUser.getUid());
                        if (firebaseUser.getDisplayName() != null)
                            action.setUsername(firebaseUser.getDisplayName());
                        String actionKey = databaseRef.child("action").push().getKey();
                        databaseRef.child("action").child(actionKey).setValue(action);

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

        // create and add event action
        Action action = new Action(eventId, "", createdAt,
                Action.ACTION_JOIN, firebaseUser.getUid());
        if (firebaseUser.getDisplayName() != null)
            action.setUsername(firebaseUser.getDisplayName());
        String actionKey = databaseRef.child("action").push().getKey();
        databaseRef.child("action").child(actionKey).setValue(action);

        // update UI
        fab.setImageResource(R.drawable.ic_leave);
        userJoined = true;

        // allow user to now send messages in game chat
        enableMessaging();

        showSuccess();
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

        builder.create().show();
    }


    public void enableMessaging(){
        // update user status to enable/disable messaging
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + mViewPager.getCurrentItem());
        if (mViewPager.getCurrentItem() == 1 && page != null)
            ((ChatFragment)page).enableMessaging();
    }

    private void hideSoftKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if(imm.isAcceptingText()) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void showSuccess(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.success_join_title));
        builder.setMessage(getString(R.string.success_join));
        builder.setCancelable(false);
        builder.setNegativeButton(
                "Close",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder.create().show();
    }

    // getters
    public boolean getUserStatus(){
        return userJoined;
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
