package io.renderapps.balizinha.ui.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.renderapps.balizinha.R;

import io.renderapps.balizinha.model.EventJsonAdapter;
import io.renderapps.balizinha.service.CloudService;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.util.ActivityLauncher;
import io.renderapps.balizinha.util.CommonUtils;
import io.renderapps.balizinha.util.Constants;

public class MapFragment extends Fragment {

    // properties
    private RecyclerView.Adapter mAdapter;
    private List<Event> currentEvents;
    private List<String> currentEventsIds;
    private List<String> userEvents;

    // firebase
    private FirebaseUser firebaseUser;
    private FirebaseRemoteConfig remoteConfig;
    private DatabaseReference databaseRef;
    private DatabaseReference eventsRef;
    private ChildEventListener childEventListener;
    private Query eventQuery;

    // views
    @BindView(R.id.event_recycler) RecyclerView recyclerView;
    @BindView(R.id.empty_view) LinearLayout emptyView;
    @BindView(R.id.progress_view) FrameLayout progressView;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_map, container, false);
        ButterKnife.bind(this, root);
        setHasOptionsMenu(true);

        // toolbar
        Toolbar toolbar = root.findViewById(R.id.map_toolbar);
        toolbar.setTitle(R.string.toolbar_title_map);

        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

            final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null){
                actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        // properties
        currentEventsIds = new ArrayList<>();
        currentEvents = new ArrayList<>();
        userEvents = new ArrayList<>();

        // firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();
        eventsRef = databaseRef.child("events");

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef.child("userEvents").child(firebaseUser.getUid()).keepSynced(true);

        setupRecycler();
        fetchUserEvents();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseRef != null && childEventListener != null)
            databaseRef.child("userEvents").child(firebaseUser.getUid()).removeEventListener(childEventListener);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_map, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                if (getActivity() != null){
                    if (getActivity() instanceof MainActivity)
                        ActivityLauncher.launchAccount((MainActivity) getActivity());
                }
                break;
            case R.id.action_create:
                if (getActivity() instanceof MainActivity)
                    ActivityLauncher.createGame((MainActivity) getActivity());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setupRecycler(){
        if (!isValidContext()) return;

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.hasFixedSize();

        mAdapter = new MapAdapter(getActivity(), currentEvents);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                // hide empty and progress view when first item is loaded into adapter
                emptyView.setVisibility(View.GONE);
                progressView.setVisibility(View.GONE);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                if (mAdapter.getItemCount() == 0)
                    emptyView.setVisibility(View.VISIBLE);
            }
        });

        recyclerView.setAdapter(mAdapter);
    }


    public void removeEvent(String eventKey){
        final int index = currentEventsIds.indexOf(eventKey);
        if (index > -1){
            if (isAdded() && isValidContext()) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentEventsIds.remove(index);
                        currentEvents.remove(index);
                        mAdapter.notifyItemRemoved(index);
                    }
                });
            }
        }
    }


    /******************************************************************************
     * Firebase
     *****************************************************************************/

    public void fetchUserEvents(){
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null)
                    if ((Boolean) dataSnapshot.getValue()) {
                        userEvents.add(dataSnapshot.getKey());
                        if (currentEventsIds.contains(dataSnapshot.getKey())){
                            removeEvent(dataSnapshot.getKey());
                        }
                    }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() == null) return;

                final String eventKey = dataSnapshot.getKey();
                final int index = userEvents.indexOf(eventKey);
                final boolean isJoined = (Boolean) dataSnapshot.getValue();

                if (!isJoined){
                    // user left game
                    if (index > -1) {
                        userEvents.remove(eventKey);
                        addEvent(eventKey);
                    }
                } else{
                    // user joined game
                    if (index == -1) {
                        userEvents.add(eventKey);
                        removeEvent(eventKey);
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        databaseRef.child("userEvents").child(firebaseUser.getUid()).addChildEventListener(childEventListener);
        databaseRef.child("userEvents").child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // will fetch events after all user events have been fired
                loadEvents();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }


    private void loadEvents(){
        if (remoteConfig == null) remoteConfig = FirebaseRemoteConfig.getInstance();
        if (!isValidContext()) return;

        remoteConfig.fetch(Constants.REMOTE_CACHE_EXPIRATION).addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                    remoteConfig.activateFetched();

                if (remoteConfig.getBoolean(Constants.CONFIG_AVAILABLE_EVENTS)){
                    fetchAvailableEvents();
                } else {
                    checkEmptyState();

                    // query all games that have not reached endTime
                    final double start = new Date().getTime() / 1000.0;
                    eventQuery = eventsRef.orderByChild("endTime").startAt(start);
                    fetchCurrentEvents();
                }
            }
        });
    }

    private void fetchAvailableEvents(){
        currentEventsIds.clear();
        currentEvents.clear();

        Moshi moshi = new Moshi.Builder()
                .add(new EventJsonAdapter())
                .build();

        final JsonAdapter<Event> jsonAdapter = moshi.adapter(Event.class);
        new CloudService(response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONObject resultsObj = jsonObject.getJSONObject("results");
                Iterator<String> ids = resultsObj.keys();

                // no available games
                if (!ids.hasNext()){
                    showEmptyView();
                    return;
                }

                final Date date = new Date();
                while (ids.hasNext()) {
                    String eventId = ids.next();
                    Event event;
                    try {
                        event = jsonAdapter.fromJson(resultsObj.getString(eventId));

                        if (event == null) continue;
                        if (!event.active) continue;
                        if (userEvents.contains(eventId)) continue;

                        if (CommonUtils.isGameOver(CommonUtils.secondsToMillis(event.endTime),
                                date))
                            continue;

                        event.setEid(eventId);
                        currentEventsIds.add(eventId);
                        currentEvents.add(event);
                        updateAdapter(currentEvents.size() - 1, true);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (currentEvents.isEmpty()) showEmptyView();
                else progressView.setVisibility(View.GONE);

            } catch (JSONException e) {
                e.printStackTrace();
                showEmptyView();
            }
        }).getAvailableEvents(firebaseUser.getUid());
    }

    private void checkEmptyState(){
        eventQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // initial data loaded
                if (currentEvents.size() == 0)
                    showEmptyView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void fetchCurrentEvents(){
        eventQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    final Event event = dataSnapshot.getValue(Event.class);
                    if (event == null)
                        return;

                    // user already in game
                    if (userEvents.contains(dataSnapshot.getKey()))
                        return;

                    // do not display if it's not active
                    if (dataSnapshot.hasChild("active")){
                        if (!event.active){
                            return;
                        }
                    }

                    // check if event is today / upcoming
                    long endTime = event.getEndTime() * 1000;
                    Date endDate = new Date(endTime);
                    if (new Date().before(endDate)) {
                        if (!userEvents.contains(dataSnapshot.getKey())) {
                            // game is upcoming, add listener to it
                            addEvent(dataSnapshot.getKey());
                        }
                    }
                }

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    final Event event = dataSnapshot.getValue(Event.class);
                    if (event == null)
                        return;

                    // remove event from listing if no longer active
                    if (dataSnapshot.hasChild("active")){
                        if (!event.active){
                            removeEvent(dataSnapshot.getKey());
                            return;
                        }
                    }

                    // check if event is today / upcoming
//                    Date eDate = new Date(dataSnapshot.child("startTime").getValue(Long.class) * 1000);
                    long endTime = event.getEndTime() * 1000;
                    Date endDate = new Date(endTime);
                    if (new Date().before(endDate)) {
                        if (!userEvents.contains(dataSnapshot.getKey())) {
                            // game is upcoming, add listener to it
                            addEvent(dataSnapshot.getKey());
                        }
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

    public void addEvent(final String eid){
        eventsRef.child(eid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final Event event = dataSnapshot.getValue(Event.class);
                if (event == null) return;

                event.setEid(dataSnapshot.getKey());
                final int index = currentEventsIds.indexOf(dataSnapshot.getKey());

                if (index > -1){
                    // update
                    currentEvents.set(index, event);
                    updateAdapter(index, false);
                } else {
                    // appending
                    currentEventsIds.add(dataSnapshot.getKey());
                    currentEvents.add(event);
                    updateAdapter(currentEvents.size() - 1, true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }


    public void updateAdapter(final int index, final boolean isItemInserted){
        if (isAdded() && getActivity()!= null && !getActivity().isDestroyed() && !getActivity().isFinishing()){
            getActivity().runOnUiThread(() -> {
                if (isItemInserted)
                    mAdapter.notifyItemInserted(index);
                else
                    mAdapter.notifyItemChanged(index);
            });
        }
    }

    void showEmptyView(){
        if (isAdded() && getActivity() != null){
            getActivity().runOnUiThread(() -> {
                progressView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            });
        }
    }

    private boolean isValidContext(){
        return getActivity() instanceof MainActivity && CommonUtils.isValidContext((MainActivity)getActivity());
    }
}
