package io.renderapps.balizinha.ui.event;

import android.content.Context;
import android.content.DialogInterface;
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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.service.CloudService;
import io.renderapps.balizinha.service.PlayerService;
import io.renderapps.balizinha.ui.event.chat.ChatFragment;
import io.renderapps.balizinha.util.Constants;
import io.renderapps.balizinha.util.DialogHelper;
import io.renderapps.balizinha.util.PhotoHelper;

import static io.renderapps.balizinha.util.Constants.REF_CHARGES;
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
    public static String EVENT_PLAYER_STATUS = "player_status";

    // properties
    private Event event;
    private String eventId;
    private String title;
    private int playerCount = 0;
    private boolean didLaunchFromCalendar;
    private boolean userJoined;
    private boolean eventUpdate = false;
    private boolean viewPagerAdded = false;

    // firebase
    private ValueEventListener valueEventListener;
    private ValueEventListener playerCountListener;
    private DatabaseReference databaseRef;
    private DatabaseReference paymentRef;
    private FirebaseUser firebaseUser;
    private FirebaseRemoteConfig remoteConfig;

    @BindView(R.id.app_bar_layout) AppBarLayout appBarLayout;
    @BindView(R.id.header_img) ImageView headerImage;
    @BindView(R.id.viewpager) ViewPager mViewPager;
    @BindView(R.id.tabs) TabLayout tabLayout;

    @BindView(R.id.bottom_view) CardView bottomView;
    @BindView(R.id.joinLeaveButton) Button joinLeaveButton;
    @BindView(R.id.status_layout) LinearLayout joinProgress;

    @OnClick(R.id.joinLeaveButton) void onJoinLeaveGame(){
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null)
            onBackPressed();

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
                    hasUserAlreadyPaid();
                else
                    onUserJoin();

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

        // fetch bundle from intent
        eventId = getIntent().getStringExtra(EVENT_ID);
        title = getIntent().getStringExtra(EVENT_TITLE);
        didLaunchFromCalendar = getIntent().getBooleanExtra(EVENT_LAUNCH_MODE, false);
        userJoined = getIntent().getBooleanExtra(EVENT_PLAYER_STATUS, false);
        boolean eventIsOver = getIntent().getBooleanExtra(EVENT_STATUS, false);

        if (eventId == null || eventId.isEmpty()){
            onBackPressed();
            return;
        }

        // firebase
        remoteConfig = FirebaseRemoteConfig.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        paymentRef = databaseRef.child(REF_CHARGES).child("events").child(eventId);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(title);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));

        if (userJoined)
            updateButtonTitle(true);

        if (eventIsOver)
            bottomView.setVisibility(View.GONE);

        fetchEvent();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseRef != null) {
            if (valueEventListener != null)
                databaseRef.child(REF_EVENTS).child(eventId).removeEventListener(valueEventListener);
            if (playerCountListener != null)
                databaseRef.child(REF_EVENT_USERS).child(event.getEid()).addValueEventListener(playerCountListener);
        }

        if (paymentRef != null)
            paymentRef.keepSynced(false);
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

    void showJoinProgress(final boolean showProgress){
        if (!isDestroyed() && !isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // reset progress text
                    ((TextView) joinProgress.findViewById(R.id.status)).setText(R.string.status_joining);

                    if (showProgress){
                        joinProgress.setVisibility(View.VISIBLE);
                        joinLeaveButton.setVisibility(View.INVISIBLE);
                    } else {
                        joinProgress.setVisibility(View.GONE);
                        joinLeaveButton.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    public void showFailedPayment(){
        if (!isDestroyed() && !isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.payment_failed), Toast.LENGTH_LONG).show();
                    showJoinProgress(false);
                    joinLeaveButton.setEnabled(true);
                }
            });
        }
    }


    void updateButtonTitle(Boolean didJoin){
        if (didJoin){
            joinLeaveButton.setText(R.string.leave_game);
            joinLeaveButton.setBackground(ContextCompat.getDrawable(this, R.drawable.background_leave_button));
        } else {
            joinLeaveButton.setText(R.string.join_game);
            joinLeaveButton.setBackground(ContextCompat.getDrawable(this, R.drawable.background_join_button));
        }
    }


    // getters
    public boolean getUserStatus(){
        return userJoined;
    }


    /**************************************************************************************************
     * Firebase
     *************************************************************************************************/

    void fetchEvent(){
        final Context mContext = this;
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
                            joinLeaveButton.setEnabled(true);
                            eventUpdate = true;

                            if (event.getPhotoUrl() != null && !event.getPhotoUrl().isEmpty())
                                PhotoHelper.glideHeader(mContext, headerImage, event.getPhotoUrl(), R.drawable.ic_soccer_header);

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
        updateButtonTitle(false);
        joinLeaveButton.setEnabled(true);
        userJoined = false;

        // disable messaging
        enableMessaging();

        // only finish activity if launched from calendar
        if (didLaunchFromCalendar)
            finish();
    }

    /*
     * Adds user to the event and enables messaging.
     * Can be called from background thread, update UI on main thread.
     */
    void onUserJoin(){
        databaseRef.child(REF_EVENT_USERS).child(eventId).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                int numOfPlayers = 0;
                for (MutableData child: mutableData.getChildren()){
                    if (child != null && child.getValue() != null){
                        if ((Boolean)child.getValue()){
                            numOfPlayers++;
                        }
                    }
                }

                if (numOfPlayers < event.getMaxPlayers()){
                    databaseRef.child(REF_USER_EVENTS).child(firebaseUser.getUid()).child(eventId).setValue(true);
                    databaseRef.child(REF_EVENT_USERS).child(eventId).child(firebaseUser.getUid()).setValue(true);
                    return Transaction.success(mutableData);
                } else {
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean successful, @Nullable DataSnapshot dataSnapshot) {
                if (databaseError != null || !successful){
                    Toast.makeText(getApplicationContext(), "Unable to join game. Max number of players reached.", Toast.LENGTH_LONG).show();
                    showJoinProgress(false);
                    joinLeaveButton.setEnabled(true);
                    return;
                }

                // successful
                successfulJoin();
            }
        });
    }

    void successfulJoin(){
        if (!isDestroyed() && !isFinishing()) {
            final Context mContext = this;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    userJoined = true;

                    showJoinProgress(false);
                    updateButtonTitle(true);
                    joinLeaveButton.setEnabled(true);

                    // allow user to now send messages in game chat
                    enableMessaging();
                    DialogHelper.showSuccessfulJoin((EventDetailsActivity)mContext);
                }
            });
        }
    }



    /**************************************************************************************************
     * Verify payment
     *************************************************************************************************/

    /*
     * Check if user has already paid for the event. If so, add user to event else
     * will prompt for payment (if any).
     */
    void hasUserAlreadyPaid(){
        showJoinProgress(true);
        paymentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()){
                    for (DataSnapshot child: dataSnapshot.getChildren()){

                        final String pid = child.child("player_id").getValue(String.class);
                        final String status = child.child("status").getValue(String.class);

                        if (pid != null && status != null && pid.equals(firebaseUser.getUid()) && status.equals("succeeded")){
                            onUserJoin();
                            return;
                        }
                    }
                }

                // user has not paid
                isPaymentConfigEnabled();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    /*
     * Check if payments for events are enabled/disabled.
     */
    void isPaymentConfigEnabled(){
        final int cacheExpiration = Constants.CACHE_EXPIRATION;
        remoteConfig.fetch(cacheExpiration).addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                    remoteConfig.activateFetched();
                boolean paymentRequired = remoteConfig.getBoolean(Constants.PAYMENT_CONFIG_KEY);

                if (paymentRequired) {
                    checkUserPaymentMethod();
                } else {
                    showPaymentDialog();
                }
            }
        });
    }

    void checkUserPaymentMethod(){
        final Context mContext = this;
        FirebaseDatabase.getInstance().getReference().child("stripe_customers")
                .child(firebaseUser.getUid()).child("source")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                        if (!isDestroyed() && !isFinishing()){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (dataSnapshot.exists() && dataSnapshot.getValue() != null)
                                        confirmPaymentDialog();
                                    else {
                                        showJoinProgress(false);
                                        DialogHelper.showPaymentRequiredDialog((EventDetailsActivity)mContext);
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
    }

    /*
     * Prompt user for payment and add charge.
     */
    void confirmPaymentDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.confirm_payment));
        builder.setCancelable(false);

        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_layout_payment, null);
        ((TextView)view.findViewById(R.id.payment_details)).setText("Press Ok to pay $"
                .concat((String.format(Locale.getDefault(), "%.2f", event.getAmount()))).concat(" for this game."));
        builder.setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        onAddCharge();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showJoinProgress(false);
                        dialog.cancel();
                    }
                });

        if (!isDestroyed() && !isFinishing()){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.create().show();
                }
            });
        }
    }

    private void showPaymentDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.payment_required_title));
        builder.setCancelable(false);

        LayoutInflater inflater = getLayoutInflater();
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
                        showJoinProgress(false);
                        dialog.cancel();
                    }
                });

        if (!isDestroyed() && !isFinishing())
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.create().show();
                }
            });
    }

    void onAddCharge(){
        joinLeaveButton.setEnabled(false);

        // update UI
        if (!isDestroyed() && !isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) joinProgress.findViewById(R.id.status)).setText(R.string.status_processing_payment);
                }
            });
        }

        new CloudService(new CloudService.ProgressListener() {
            @Override
            public void onStringResponse(String response) {
                if (response == null || response.isEmpty()){
                    showFailedPayment();
                    return;
                }

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.has("error")){
                        showFailedPayment();
                        return;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    showFailedPayment();
                    return;
                }

                // successful
                onUserJoin();

            }
        }).holdPaymentForEvent(firebaseUser.getUid(), eventId);
    }
}
