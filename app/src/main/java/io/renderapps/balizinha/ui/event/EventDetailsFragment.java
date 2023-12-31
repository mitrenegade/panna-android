package io.renderapps.balizinha.ui.event;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.service.FirebaseService;
import io.renderapps.balizinha.ui.event.attendees.AttendeesActivity;
import io.renderapps.balizinha.util.Constants;
import io.renderapps.balizinha.util.PhotoHelper;

import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;

/**
 * Created by joel
 * on 12/20/17.
 */

public class EventDetailsFragment extends Fragment implements OnMapReadyCallback {

    public static String EXTRA_EVENT = "event";

    // properties
    private Event mEvent;
    private DatabaseReference databaseRef;
    private Unbinder unbinder;

    private GoogleMap googleMap;
    private ArrayList<Player> playerList;
    private List<String> playerListIds;
    private PlayersAdapter playersAdapter;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("EE, MMM dd", Locale.getDefault());
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm aa", Locale.getDefault());
    private Calendar calendar;

    // Views
    @BindView(R.id.players_recycler) RecyclerView playersRecycler;
    @BindView(R.id.event_title) TextView event_title;
    @BindView(R.id.event_date) TextView event_date;
    @BindView(R.id.event_time) TextView event_time;
    @BindView(R.id.event_location) TextView event_location;
    @BindView(R.id.event_city) TextView event_city;
    @BindView(R.id.event_description) TextView event_description;
    @BindView(R.id.creator_name) TextView event_creator_name;
    @BindView(R.id.payment_required) TextView event_payment;
    @BindView(R.id.creator_img) ImageView event_creator_photo;
    @BindView(R.id.player_progressbar) ProgressBar progressBar;
    @BindView(R.id.player_count) TextView numOfPlayers;
    @BindView(R.id.btn_attendees) TextView viewAttendees;
    @BindView(R.id.mapView) MapView mapView;

    @BindView(R.id.top_separator) View topSeparator;
    @BindView(R.id.separator_layout) LinearLayout separatorLayout;
    @BindView(R.id.hide_button) ImageButton hideButton;
    @BindView(R.id.left_seperator) View leftSeparator;
    @BindView(R.id.right_separator) View rightSeparator;

    @OnClick(R.id.hide_button) void hideMap(){
        if (mapView.isShown()) {
            hideButton.setImageResource(R.drawable.ic_eye_hidden);
            mapView.setVisibility(View.GONE);
            leftSeparator.setVisibility(View.VISIBLE);
            rightSeparator.setVisibility(View.VISIBLE);
        } else {
            hideButton.setImageResource(R.drawable.ic_eye);
            mapView.setVisibility(View.VISIBLE);
            leftSeparator.setVisibility(View.INVISIBLE);
            rightSeparator.setVisibility(View.INVISIBLE);
        }
    }


    @OnClick(R.id.btn_attendees) void viewAttendees(){
        if (!isAdded() || getActivity() == null) return;

        Intent intent = new Intent(getActivity(), AttendeesActivity.class);
        if (playerList != null && !playerList.isEmpty())
            intent.putParcelableArrayListExtra(AttendeesActivity.EXTRA_PLAYERS,
                    playerList);

        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.anim_slide_in_right,
                R.anim.anim_slide_out_left);
    }


    public static EventDetailsFragment newInstance(Event event) {
        EventDetailsFragment fragment = new EventDetailsFragment();

        Bundle args = new Bundle();
        args.putParcelable(EXTRA_EVENT, event);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            mEvent = getArguments().getParcelable(EXTRA_EVENT);

        databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_event_details, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        mapView.onCreate(savedInstanceState);

        // setup
        calendar = Calendar.getInstance();
        playerList = new ArrayList<>();
        playerListIds = new ArrayList<>();

        setupPlayersRecycler();
        showMap();

        updateEvent(mEvent);
        fetchOrganizer();
        fetchPlayers();

        return rootView;
    }

    public void setupPlayersRecycler(){
        if (getActivity() == null)
            return;

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        playersRecycler.setLayoutManager(layoutManager);

        // adapter
        playersAdapter = new PlayersAdapter(getActivity(), playerList);
        playersAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                viewAttendees.setEnabled(true);
                progressBar.setVisibility(View.GONE);

            }
        });

        playersRecycler.setAdapter(playersAdapter);
    }

    public void updateEvent(Event event){
        mEvent = event;

        // title
        if (mEvent.getName() != null)
            event_title.setText(mEvent.getName());
        if (mEvent.getType() != null)
            event_title.setText(event_title.getText().toString().concat(" ").concat("(").concat(mEvent.getType())
                .concat(")"));

        // description
        final String description = (mEvent.getInfo() != null && !mEvent.getInfo().isEmpty()) ?
                mEvent.getInfo() : getString(R.string.none);
        event_description.setText(description);

        // place
        if (mEvent.getPlace() != null && !mEvent.getPlace().isEmpty())
            event_location.setText(mEvent.getPlace());
        else
            event_location.setVisibility(View.GONE);

        // location
        String location = "";
        if (mEvent.getCity() != null)
            location = mEvent.getCity();

        if (mEvent.getState() != null)
            location = location.concat(", ").concat(mEvent.getState());
        event_city.setText(location);

        // payment
        if (mEvent.paymentRequired) {
            event_payment.setText(String.format(Locale.getDefault(),"%.2f", mEvent.getAmount()));
            event_payment.setVisibility(View.VISIBLE);
        } else {
            event_payment.setText("Free");
        }

        // date, time
        formatTime(mEvent.getStartTime());
        String date = dateFormatter.format(calendar.getTime());
        String time = timeFormatter.format(calendar.getTime());
        event_date.setText(date);
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

    public void fetchOrganizer(){
        final String organizerId = mEvent.getOwner();
        loadOrganizerPhoto();

        databaseRef.child(REF_PLAYERS).child(organizerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    final Player creator = dataSnapshot.getValue(Player.class);

                    if (isAdded()) {
                        if (getActivity() != null && !getActivity().isDestroyed() && !getActivity().isFinishing()) {
                            getActivity().runOnUiThread(() -> {
                                if (creator == null) return;

                                if (creator.getName() != null)
                                    event_creator_name.setText("Organizer: ".concat(creator.getName()));
                            });
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    void loadOrganizerPhoto(){
        final Context mContext = getActivity();
        FirebaseStorage.getInstance().getReference()
                .child("images/player").child(mEvent.getOwner()).getDownloadUrl().addOnSuccessListener(uri -> {
                    if (uri != null){
                        PhotoHelper.glideImage(mContext, event_creator_photo, uri.toString(), R.drawable.ic_default_photo);
                    }
                }).addOnFailureListener(exception -> { });
    }

    public void fetchPlayers(){
        final String eventId = mEvent.getEid();
        databaseRef.child("eventUsers").child(eventId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot child, String s) {
                if (child.exists() && child.getValue() != null){
                    final String userId = child.getKey();
                    final boolean didJoin = (boolean) child.getValue();
                    if (didJoin && userId != null){
                        addPlayer(userId);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot child, String s) {
                if (child.exists() && child.getValue() != null){
                    final String userId = child.getKey();
                    final boolean didJoin = (boolean) child.getValue();
                    if (userId == null) return;

                    if (didJoin){
                        // add player
                        addPlayer(userId);
                    } else {
                        // user left
                        removePlayer(userId);
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void addPlayer(@NonNull final String uid){
        databaseRef.child(REF_PLAYERS).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || dataSnapshot.getValue() == null)
                    return;

                Player player = dataSnapshot.getValue(Player.class);
                if (player != null) {
                    player.setUid(uid);
                    playerListIds.add(uid);
                    playerList.add(player);

                    if (isAdded()) {
                        if (getActivity() != null && !getActivity().isDestroyed() && !getActivity().isFinishing()) {
                            getActivity().runOnUiThread(() -> playersAdapter.notifyItemInserted(playerList.size() - 1));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void removePlayer(@NonNull String uid){
        final int index = playerListIds.indexOf(uid);
        if (index > -1){
            playerList.remove(index);
            playerListIds.remove(index);
            if (isAdded()) {
                if (getActivity() != null && !getActivity().isDestroyed() && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> playersAdapter.notifyItemRemoved(index));
                }
            }
        }
    }

    public void updatePlayerCount(int playerCount){
        // hide players progressbar if there are no players
        if (playerCount == 0 && progressBar.isShown())
            progressBar.setVisibility(View.GONE);

        numOfPlayers.setText(String.valueOf(playerCount).concat( " attending"));
    }

    private void showMap(){
        if (isAdded() && getActivity() != null) {
            ((EventDetailsActivity)getActivity()).mCompositeDisposable.add(FirebaseService.Companion.getRemoteConfig()
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(remoteConfig -> {
                        // get update version if available
                        final boolean canShowMaps = remoteConfig.getBoolean(Constants.CONFIG_MAPS);
                        if (canShowMaps){
                            topSeparator.setVisibility(View.GONE);
                            separatorLayout.setVisibility(View.VISIBLE);
                            mapView.setVisibility(View.VISIBLE);

                            mapView.getMapAsync(this);
                        }
                    }, error -> {
                    }));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        googleMap.getUiSettings().setMapToolbarEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);
        
        goToLocationZoom(mEvent.lat, mEvent.lon, 16);
    }

    private void goToLocationZoom(double lat, double lng, float zoom) {
        if (googleMap == null) return;

        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);

        final String title = (mEvent.name == null || mEvent.name.isEmpty()) ? "" : mEvent.name;
        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng)).title(mEvent.name).title(title));
        googleMap.moveCamera(update);
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (playersRecycler != null)
            playersRecycler.setAdapter(null);
        unbinder.unbind();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
