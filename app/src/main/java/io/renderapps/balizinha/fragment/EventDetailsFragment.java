package io.renderapps.balizinha.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import io.renderapps.balizinha.adapter.PlayersAdapter;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.CircleTransform;

/**
 * Created by joel
 * on 12/20/17.
 */

public class EventDetailsFragment extends Fragment {

    // load event from key or from bundle
    public static String EVENT_ID = "eid";
    public static String EVENT_TITLE = "title";
    public static String EVENT_TYPE = "type";
    public static String EVENT_TIME = "time";
    public static String EVENT_INFO = "info";
    public static String EVENT_CITY = "city";
    public static String EVENT_STATE = "state";
    public static String EVENT_CREATOR = "creator";
    public static String EVENT_PAYMENT = "payment_required";
    public static String EVENT_PLAYER_STATUS = "player_status"; // joined or not
    public static String EVENT_STATUS = "event_status"; // upcoming or past

    // properties
    private String eventId;
    private String title;
    private String type;
    private long time;
    private String description;
    private String creator_id;
    private String city;
    private String state;
    private boolean payment_required;
    private Context mContext;
    private List<Player> playerList;
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
    private TextView event_description;
    private TextView event_creator_name;
    private ImageView event_creator_photo;
    private ImageView event_payment;
    private ProgressBar progressBar;

    public EventDetailsFragment() {
        // Required empty public constructor
    }

    public static EventDetailsFragment newInstance(String eid, String title, String city, String state, String info,
                                                   String creator, String type, long time, boolean player_status,
                                                   boolean event_status, boolean payment_required) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putString(EVENT_ID, eid);
        args.putString(EVENT_TITLE, title);
        args.putString(EVENT_CITY, city);
        args.putString(EVENT_INFO, info);
        args.putString(EVENT_CREATOR, creator);
        args.putLong(EVENT_TIME, time);
        args.putString(EVENT_TYPE, type);
        args.putString(EVENT_STATE, state);
        args.putBoolean(EVENT_PLAYER_STATUS, player_status);
        args.putBoolean(EVENT_STATUS, event_status);
        args.putBoolean(EVENT_PAYMENT, payment_required);
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
            description = getArguments().getString(EVENT_INFO);
            type = getArguments().getString(EVENT_TYPE);
            time = getArguments().getLong(EVENT_TIME);
            payment_required = getArguments().getBoolean(EVENT_PAYMENT);
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
        event_creator_name = rootView.findViewById(R.id.creator_name);
        event_creator_photo = rootView.findViewById(R.id.creator_img);
        event_payment = rootView.findViewById(R.id.payment_required);
        progressBar = rootView.findViewById(R.id.player_progressbar);

        // setup
        calendar = Calendar.getInstance();
        playerList = new ArrayList<>();
        playerListIds = new ArrayList<>();
        setupPlayersRecycler();
        setupEvent();

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

    private void setupEvent(){
        // event
        if (description != null)
            if (description.isEmpty())
                event_description.setText(getString(R.string.none));
            else
                event_description.setText(description);

        if (title != null)
            event_title.setText(title.concat(" ").concat("(").concat(type)
                .concat(")"));
        String address = city;
        if (state != null)
            address = address.concat(", ").concat(state);
        event_location.setText(address);

        // payment
        if (payment_required)
            event_payment.setVisibility(View.VISIBLE);

        // Date
        formatTime(time);
        String time = mFormatter.format(calendar.getTime());
        event_time.setText(time);

        // players
        fetchCreator();
        fetchPlayers();
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
        databaseRef.child("players").child(creator_id).addListenerForSingleValueEvent(new ValueEventListener() {
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
                                    loadImage(event_creator_photo, creator.getPhotoUrl());
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
        databaseRef.child("players").child(pid).addListenerForSingleValueEvent(new ValueEventListener() {
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

    public void loadImage(ImageView iv, String photoUrl){
        if (isAdded()){
            RequestOptions myOptions = new RequestOptions()
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transform(new CircleTransform(getActivity()))
                    .placeholder(R.drawable.ic_default_photo);
            // load photo
            Glide.with(this)
                    .asBitmap()
                    .apply(myOptions)
                    .load(photoUrl)
                    .into(iv);
        }
    }
}
