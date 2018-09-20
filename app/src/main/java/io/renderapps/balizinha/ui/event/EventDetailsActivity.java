package io.renderapps.balizinha.ui.event;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.service.PaymentService;
import io.renderapps.balizinha.service.PlayerService;
import io.renderapps.balizinha.service.ShareService;
import io.renderapps.balizinha.service.EventService;
import io.renderapps.balizinha.service.StorageService;
import io.renderapps.balizinha.ui.event.chat.ChatFragment;
import io.renderapps.balizinha.ui.event.organize.CreateEventActivity;
import io.renderapps.balizinha.util.CommonUtils;
import io.renderapps.balizinha.util.DialogHelper;
import io.renderapps.balizinha.util.PhotoHelper;

import static io.renderapps.balizinha.util.Constants.REF_EVENTS;
import static io.renderapps.balizinha.util.Constants.REF_EVENT_USERS;
import static io.renderapps.balizinha.util.Constants.REF_USER_EVENTS;

/**
 * Display game users, details and actions (messages, payments)
 */
public class EventDetailsActivity extends AppCompatActivity {

    // load event from key or from bundle
    public static String EVENT_ID = "eid";
    public static String EVENT_TITLE = "title";

    // user launched event from calendar
    public static String EVENT_LAUNCH_MODE = "didLaunchFromCalendar";
    public static String EVENT_STATUS = "event_status";

    // properties
    private Event event;
    private String eventId;
    private String title = "";
    private int playerCount = 0;
    private boolean didLaunchFromCalendar = false;
    private boolean userJoined = false;
    private boolean eventUpdate = false;
    private boolean viewPagerAdded = false;

    // firebase
    private ValueEventListener valueEventListener;
    private ValueEventListener playerCountListener;
    private DatabaseReference databaseRef;
    private FirebaseUser firebaseUser;

    @BindView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.app_bar_layout) AppBarLayout appBarLayout;
    @BindView(R.id.progress_view) FrameLayout progressView;

    @BindView(R.id.header_img) ImageView headerImage;
    @BindView(R.id.viewpager) ViewPager mViewPager;
    @BindView(R.id.tabs) TabLayout tabLayout;

    @BindView(R.id.bottom_view) CardView bottomView;
    @BindView(R.id.joinLeaveButton) Button joinLeaveButton;
    @BindView(R.id.status_layout) LinearLayout joinProgress;
    @BindView(R.id.user_status_progress) ProgressBar userStatusProgress;

    @OnClick(R.id.joinLeaveButton) void onJoinLeaveGame(){
        if (firebaseUser == null) {
            DialogHelper.showLoginDialog(this);
            return;
        }

        if (firebaseUser.getUid().equals(event.getOrganizer())){
            Intent intent = new Intent(this, CreateEventActivity.class);
            intent.putExtra(CreateEventActivity.EXTRA_EVENT, event);
            startActivity(intent);
            overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
            return;
        }

        if (userJoined) {
            showLeaveDialog();
            return;
        }

        if (playerCount >= event.maxPlayers) {
            Toast.makeText(this, getString(R.string.game_full),
                    Toast.LENGTH_LONG).show();
            return;
        }


        final EventDetailsActivity activity = this;
        PlayerService.getPlayer(firebaseUser.getUid(), new PlayerService.PlayerCallback() {
            @Override
            public void onSuccess(@Nullable Player player) {
                if (player == null){
                    Toast.makeText(getApplicationContext(), "Please log in to continue.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (player.getName() == null || player.getName().isEmpty()){
                    DialogHelper.showAddNameDialog(activity);
                    return;
                }

                //  valid name
                if (event.paymentRequired)
                    PaymentService.Companion.hasUserAlreadyPaid(EventDetailsActivity.this, event, firebaseUser.getUid());
                else
                    PaymentService.Companion.onUserJoin(EventDetailsActivity.this, event);

            }
        });
    }

    private int[] tabIcons = {R.drawable.ic_details_filled, R.drawable.ic_chat_filled};
    private int[] unselectedTabIcons = {R.drawable.ic_details, R.drawable.ic_chat};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        ButterKnife.bind(this);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));


        if (getIntent().hasExtra(EVENT_ID)){
            // fetch bundle from intent
            eventId = getIntent().getStringExtra(EVENT_ID);
            title = getIntent().getStringExtra(EVENT_TITLE);

            didLaunchFromCalendar = getIntent().getBooleanExtra(EVENT_LAUNCH_MODE, false);
            boolean eventIsOver = getIntent().getBooleanExtra(EVENT_STATUS, false);

            if (eventIsOver)
                bottomView.setVisibility(View.GONE);

            init(eventId);
            return;
        }

        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent()).addOnCompleteListener(new OnCompleteListener<PendingDynamicLinkData>() {
            @Override
            public void onComplete(@NonNull Task<PendingDynamicLinkData> task) {
                if (task.isSuccessful()){
                    if (task.getResult() != null && task.getResult().getLink() != null){
                        eventId = task.getResult().getLink().getLastPathSegment();

                        if (FirebaseAuth.getInstance().getCurrentUser() == null){
                            Toast.makeText(getApplicationContext(), "Login in to view event.", Toast.LENGTH_LONG).show();
                            CommonUtils.launchLogin(EventDetailsActivity.this);
                            return;
                        }

                        init(eventId);
                        return;
                    }
                }

                // error
                onBackPressed();
            }
        });
    }

    private void init(final String eventId){
        databaseRef = FirebaseDatabase.getInstance().getReference();
        fetchEvent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_event, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        if (item.getItemId() == R.id.action_share)
            ShareService.showShareDialog(this, eventId);
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseRef != null && eventId != null) {
            if (valueEventListener != null)
                databaseRef.child(REF_EVENTS).child(eventId).removeEventListener(valueEventListener);
            if (playerCountListener != null)
                databaseRef.child(REF_EVENT_USERS).child(eventId).addValueEventListener(playerCountListener);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }

    /**************************************************************************************************
     * Setup Layout
     *************************************************************************************************/

    public void setupViewPager(){
        viewPagerAdded = true;
        EventPagerAdapter mSectionsPagerAdapter = new EventPagerAdapter(getSupportFragmentManager());
        EventDetailsFragment eventDetailsFragment = EventDetailsFragment.newInstance(event);
        mSectionsPagerAdapter.addFragment(eventDetailsFragment, "Details");
        mSectionsPagerAdapter.addFragment(ChatFragment.newInstance(eventId, title), "Chat");

        mViewPager.setAdapter(mSectionsPagerAdapter);
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

        setupTabBar();
        updatePlayerCount();
    }

    public void setupTabBar(){
        // setup tabs
        tabLayout.setupWithViewPager(mViewPager);
        setupTabIcons();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                final int position = tab.getPosition();
                if (tabLayout.getTabAt(position) == null) return;

                tabLayout.getTabAt(position).setIcon(tabIcons[position]);
                tabLayout.getTabAt(position).setIcon(tabIcons[position]);

                if (position == 1)
                    bottomView.setVisibility(View.GONE);
                else {
                    bottomView.setVisibility(View.VISIBLE);
                    updatePlayerCount();
                }

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

    public void updateEventDetailFragment(){
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" +
                R.id.viewpager + ":" + mViewPager.getCurrentItem());
        if (mViewPager.getCurrentItem() == 0 && page != null) {
            eventUpdate = false;
            ((EventDetailsFragment) page).updateEvent(event);
        }
    }

    void updatePlayerCount(){
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" +
                R.id.viewpager + ":" + mViewPager.getCurrentItem());
        if (mViewPager.getCurrentItem() == 0 && page != null) {
            ((EventDetailsFragment) page).updatePlayerCount(playerCount);
        }
    }

    public void enableMessaging(){
        // update user status to enable/disable messaging
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" +
                R.id.viewpager + ":" + mViewPager.getCurrentItem());
        if (mViewPager.getCurrentItem() == 1 && page != null)
            ((ChatFragment)page).enableMessaging();
    }

    void hideSoftKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        assert imm != null;
        if(imm.isAcceptingText() && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    void updateButtonTitle(){
        if (userJoined){
            joinLeaveButton.setText(R.string.leave_game);
            joinLeaveButton.setBackground(ContextCompat.getDrawable(this, R.drawable.background_leave_button));
        } else {
            joinLeaveButton.setText(R.string.join_game);
            joinLeaveButton.setBackground(ContextCompat.getDrawable(this, R.drawable.background_join_button));
        }
    }

    void showEditButton(){
        joinLeaveButton.setText(R.string.edit_game);
        joinLeaveButton.setBackground(ContextCompat.getDrawable(this, R.drawable.background_join_button));
    }

    void showButton(){
        if (!isDestroyed() && !isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateButtonTitle();
                    userStatusProgress.setVisibility(View.GONE);
                    joinLeaveButton.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    void loadFragments(){
        if (!viewPagerAdded) {
            eventUpdate = false; // no need to update on first instance
            setupViewPager();
        } else {
            // update event details if currently on that fragment
            updateEventDetailFragment();
        }
    }


    // getters
    public boolean getUserStatus(){
        return userJoined;
    }


    /**************************************************************************************************
     * Firebase
     *************************************************************************************************/

    void loadHeader(){
        StorageService.Companion.getEventImage(eventId, new StorageService.StorageCallback() {
            @Override
            public void onSuccess(@Nullable Uri uri) {
                if (uri != null){
                    PhotoHelper.glideHeader(EventDetailsActivity.this, headerImage, uri.toString(), R.drawable.background_league_header);
                } else if (event != null && event.league != null && !event.league.isEmpty()){
                    StorageService.Companion.getLeagueHeader(event.league, new StorageService.StorageCallback() {
                        @Override
                        public void onSuccess(@Nullable Uri uri) {
                            if (uri != null) {
                                PhotoHelper.glideHeader(EventDetailsActivity.this, headerImage, uri.toString(), R.drawable.background_league_header);
                            }
                        }
                    });
                }
            }
        });
    }

    void fetchUserStatus(){
        if (isOrganizer()) {
            userStatusProgress.setVisibility(View.GONE);
            joinLeaveButton.setVisibility(View.VISIBLE);

            showEditButton();
            return;
        }

        databaseRef.child(REF_USER_EVENTS).child(firebaseUser.getUid())
                .child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    if ((boolean)dataSnapshot.getValue()){
                        userJoined = true;
                    }
                }

                if (!isDestroyed() && !isFinishing()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonTitle();
                            userStatusProgress.setVisibility(View.GONE);
                            joinLeaveButton.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showButton();
            }
        });
    }

    void fetchEvent(){
        joinLeaveButton.setEnabled(false);
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    event = dataSnapshot.getValue(Event.class);

                    if (event == null){
                        Toast.makeText(getApplicationContext(), "This game no longer exists.", Toast.LENGTH_LONG).show();
                        onBackPressed();
                        return;
                    }

                    event.setEid(eventId);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadHeader();

                            progressView.setVisibility(View.GONE);
                            bottomView.setVisibility(View.VISIBLE);

                            joinLeaveButton.setEnabled(true);
                            eventUpdate = true;

                            if (event.name != null) {
                                title = event.name;
                                collapsingToolbarLayout.setTitle(title);
                            }

                            if (EventService.isEventOver(event.endTime))
                                bottomView.setVisibility(View.GONE);

                            fetchUserStatus();
                            loadFragments();
                        }
                    });
                    return;
                }

                // event no longer exists
                Toast.makeText(getApplicationContext(), "Event no longer exists.", Toast.LENGTH_LONG).show();
                onBackPressed();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        playerCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    if (dataSnapshot.hasChildren()){
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            if (data.getValue() != null)
                                if ((Boolean) data.getValue())
                                    count++;
                        }
                    }

                    playerCount = count;
                    if (!isDestroyed() && !isFinishing()){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updatePlayerCount();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        databaseRef.child(REF_EVENT_USERS).child(eventId).addValueEventListener(playerCountListener);
        databaseRef.child(REF_EVENTS).child(eventId).addValueEventListener(valueEventListener);
    }

    public boolean isOrganizer(){
        if (firebaseUser == null)
            firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        return (firebaseUser != null && firebaseUser.getUid().equals(event.organizer));
    }


    /**************************************************************************************************
     * Action Handlers
     *************************************************************************************************/

    void showLeaveDialog(){
        final Context mContext = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
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
                        onUserLeave();
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

    /*
     * Removes user from the event and disables messaging.
     */
    void onUserLeave(){
        databaseRef.child(REF_USER_EVENTS).child(firebaseUser.getUid())
                .child(eventId).setValue(false);
        databaseRef.child(REF_EVENT_USERS).child(eventId)
                .child(firebaseUser.getUid()).setValue(false);

        // update UI
        userJoined = false;
        updateButtonTitle();
        joinLeaveButton.setEnabled(true);

        // disable messaging
        enableMessaging();

        // only finish activity if launched from calendar
        if (didLaunchFromCalendar)
            finish();
    }

    public void showSuccessfulJoin(){
        if (!isDestroyed() && !isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    userJoined = true;
                    updateButtonTitle();
                    joinLeaveButton.setEnabled(true);

                    // allow user to now send messages in game chat
                    enableMessaging();
                }
            });
        }
    }
}
