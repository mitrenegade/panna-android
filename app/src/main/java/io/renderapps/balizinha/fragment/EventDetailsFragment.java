package io.renderapps.balizinha.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.AttendeesActivity;
import io.renderapps.balizinha.activity.EventDetailsActivity;
import io.renderapps.balizinha.adapter.PlayersAdapter;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.CircleTransform;
import io.renderapps.balizinha.util.GeneralHelpers;

import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;

/**
 * Created by joel
 * on 12/20/17.
 */

public class EventDetailsFragment extends Fragment implements View.OnClickListener {

    // load event from key or from bundle
    public static String EVENT_ID = "eid";
    public static String EVENT_TITLE = "title";
    public static String EVENT_TYPE = "type";
    public static String EVENT_TIME = "time";
    public static String EVENT_INFO = "info";
    public static String EVENT_PLACE = "place";
    public static String EVENT_CITY = "city";
    public static String EVENT_STATE = "state";
    public static String EVENT_CREATOR = "creator";
    public static String EVENT_PAYMENT = "payment_required";
    public static String EVENT_PRICE = "event_price";
    public static String EVENT_PLAYER_STATUS = "player_status"; // joined or not
    public static String EVENT_STATUS = "event_status"; // upcoming or past

    // properties
    private String eventId;
    private String title;
    private String type;
    private double price;
    private long time;
    private String description;
    private String creator_id;
    private String city;
    private String state;
    private String place;
    private boolean payment_required;
    private Context mContext;
    private ArrayList<Player> playerList;
    private List<String> playerListIds;
    private PlayersAdapter playersAdapter;
    private SimpleDateFormat mFormatter = new SimpleDateFormat("EE, MMM dd \u2022 h:mm aa", Locale.getDefault());
    private Calendar calendar;

    // firebase
    private DatabaseReference databaseRef;

    // views
    private RecyclerView playersRecycler;
    private TextView event_title;
    private TextView event_time;
    private TextView event_location;
    private TextView event_address;
    private TextView event_description;
    private TextView event_creator_name;
    private ImageView event_creator_photo;
    private TextView event_payment;
    private ProgressBar progressBar;

    public EventDetailsFragment() {
        // Required empty public constructor
    }

    public static EventDetailsFragment newInstance(Event event, boolean player_status,
                                                   boolean event_status) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putString(EVENT_ID, event.getEid());
        args.putString(EVENT_TITLE, event.getName());
        args.putString(EVENT_CITY, event.getCity());
        args.putString(EVENT_PLACE, event.getPlace());
        args.putString(EVENT_INFO, event.getInfo());
        args.putString(EVENT_CREATOR, event.getOwner());
        args.putLong(EVENT_TIME, event.getStartTime());
        args.putString(EVENT_TYPE, event.getType());
        args.putString(EVENT_STATE, event.getState());
        args.putBoolean(EVENT_PLAYER_STATUS, player_status);
        args.putBoolean(EVENT_STATUS, event_status);
        args.putBoolean(EVENT_PAYMENT, event.paymentRequired);
        args.putDouble(EVENT_PRICE, event.getAmount());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(EVENT_ID);
            creator_id = getArguments().getString(EVENT_CREATOR);
            title = getArguments().getString(EVENT_TITLE);
            city = getArguments().getString(EVENT_CITY);
            state = getArguments().getString(EVENT_STATE);
            place = getArguments().getString(EVENT_PLACE);
            description = getArguments().getString(EVENT_INFO);
            type = getArguments().getString(EVENT_TYPE);
            time = getArguments().getLong(EVENT_TIME);
            payment_required = getArguments().getBoolean(EVENT_PAYMENT);
            price = getArguments().getDouble(EVENT_PRICE);
        }

        // firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_event_details, container, false);

        // views
        playersRecycler = rootView.findViewById(R.id.players_recycler);
        event_title = rootView.findViewById(R.id.event_title);
        event_time = rootView.findViewById(R.id.event_time);
        event_description = rootView.findViewById(R.id.event_description);
        event_location = rootView.findViewById(R.id.event_location);
        event_address = rootView.findViewById(R.id.event_address);
        event_creator_name = rootView.findViewById(R.id.creator_name);
        event_creator_photo = rootView.findViewById(R.id.creator_img);
        event_payment = rootView.findViewById(R.id.payment_required);
        progressBar = rootView.findViewById(R.id.player_progressbar);
        progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(mContext, R.color.colorPrimary),
                PorterDuff.Mode.SRC_IN);


        // on-click
        rootView.findViewById(R.id.btn_attendees).setOnClickListener(this);
        // setup
        calendar = Calendar.getInstance();
        playerList = new ArrayList<>();
        playerListIds = new ArrayList<>();

        setupPlayersRecycler();
        setupEventDetails();
        fetchCreator();
        fetchPlayers();

        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public void setupPlayersRecycler(){
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        playersRecycler.setLayoutManager(layoutManager);
        playersRecycler.hasFixedSize();
        // adapter
        playersAdapter = new PlayersAdapter(mContext, playerList);
        playersAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                progressBar.setVisibility(View.GONE);
            }
        });
        playersRecycler.setAdapter(playersAdapter);
    }

    public void updateEvent(Event event){
        description = event.getInfo();
        title = event.name;
        place = event.getPlace();
        city = event.getCity();
        state = event.getState();
        payment_required = event.paymentRequired;
        time = event.getStartTime();
        creator_id = event.getOwner();
        type = event.getType();
        price = event.getAmount();
        setupEventDetails();
    }

    public void setupEventDetails(){
        // event
        if (description != null)
            if (description.isEmpty())
                event_description.setText(getString(R.string.none));
            else
                event_description.setText(description);

        if (title != null)
            event_title.setText(title.concat(" ").concat("(").concat(type)
                    .concat(")"));

        // location
        if (place != null && !place.isEmpty()){
            event_location.setText(place);
        } else {
            event_location.setVisibility(View.GONE);
        }

        String address = city;
        if (state != null)
            address = address.concat(", ").concat(state);
        event_address.setText(address);

        // payment
        if (payment_required) {
            event_payment.setText(String.format(Locale.getDefault(),"%.2f", price));
            event_payment.setVisibility(View.VISIBLE);
        }

        // Date
        formatTime(time);
        String time = mFormatter.format(calendar.getTime());
        event_time.setText(time);
    }


    void formatTime(double sec){
        calendar.clear();
        calendar.setTimeInMillis((long)sec * 1000);
        int unroundedMinutes = calendar.get(Calendar.MINUTE);
        int mod = unroundedMinutes % 15;
        calendar.add(Calendar.MINUTE, mod < 8 ? -mod : (15-mod));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    public void fetchCreator(){
        databaseRef.child(REF_PLAYERS).child(creator_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    final Player creator = dataSnapshot.getValue(Player.class);
                    if (isAdded()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                event_creator_name.setText(creator.getName());
                                if (creator.getPhotoUrl() != null && !creator.getPhotoUrl().isEmpty())
                                    GeneralHelpers.glideImage(mContext, event_creator_photo,
                                            creator.getPhotoUrl(), R.drawable.ic_default_photo);
                                creator.setPid(creator_id);
                                playerListIds.add(creator_id);
                                playerList.add(creator);
                                playersAdapter.notifyItemInserted(playerList.size() - 1);
                            }
                        });
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    public void fetchPlayers(){
        databaseRef.child("eventUsers").child(eventId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot child, String s) {
                if (child.exists() && child.getValue() != null)
                            if (child.getValue(Boolean.class))
                                if (!child.getKey().equals(creator_id))
                                    addPlayer(child.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot child, String s) {
                if (child.exists() && child.getValue() != null)
                    if (child.getValue(Boolean.class)) {
                        if (!child.getKey().equals(creator_id)) {
                            addPlayer(child.getKey());
                        }
                    }else {
                    // user left
                        final int index = playerListIds.indexOf(child.getKey());
                        if (index > -1){
                            playerList.remove(index);
                            playerListIds.remove(index);
                            if (isAdded()){
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        playersAdapter.notifyItemRemoved(index);
                                    }
                                });
                            }
                        }
                    }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    public void addPlayer(final String pid){
        databaseRef.child(REF_PLAYERS).child(pid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Player player = dataSnapshot.getValue(Player.class);
                player.setPid(pid);
                playerListIds.add(pid);
                playerList.add(player);
                if (isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playersAdapter.notifyItemInserted(playerList.size() - 1);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_attendees:
                Intent intent = new Intent(mContext, AttendeesActivity.class);
                if (playerList != null && !playerList.isEmpty()){
                    intent.putParcelableArrayListExtra(AttendeesActivity.EXTRA_PLAYERS,
                            playerList);
                }
                startActivity(intent);
                ((EventDetailsActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right,
                        R.anim.anim_slide_out_left);
                break;
        }
    }
}
