package io.renderapps.balizinha.ui.calendar;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Event;

import static io.renderapps.balizinha.util.Constants.REF_EVENTS;
import static io.renderapps.balizinha.util.Constants.REF_USER_EVENTS;

public class CalendarFragment extends Fragment{

    private final String UPCOMING_SECTION_TAG = "upcomingTag";
    private final String PAST_SECTION_TAG = "pastTag";

    private ArrayList<Event> upcomingEventList;
    private ArrayList<Event> pastEventList;
    private List<String> mUpcomingEventIds;
    private List<String> mPastEventIds;

    private ChildEventListener childEventListener;

    // views
    @BindView(R.id.calendar_recycler) RecyclerView mRecycler;

    SectionedRecyclerViewAdapter sectionAdapter;
    CalendarSection upcomingSection;
    CalendarSection pastSection;

    // firebase
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mGameReference;

    public CalendarFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.isEmpty())
            return;

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mGameReference = mDatabaseRef.child(REF_USER_EVENTS).child(uid);

        upcomingEventList = new ArrayList<>();
        pastEventList = new ArrayList<>();
        mUpcomingEventIds = new ArrayList<>();
        mPastEventIds = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_calendar, container, false);
        ButterKnife.bind(this, root);

        // toolbar
        Toolbar toolbar = root.findViewById(R.id.calendar_toolbar);
        toolbar.setTitle(R.string.title_calendar);
        if (getActivity() != null)
            ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        setupRecycler();
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        upcomingEventList.clear();
        pastEventList.clear();
        mUpcomingEventIds.clear();
        mPastEventIds.clear();

        upcomingSection.setState(Section.State.LOADING);
        pastSection.setState(Section.State.LOADING);

        if (sectionAdapter != null)
            sectionAdapter.notifyDataSetChanged();

        checkEmptyKeys();
        loadEventKeys();
    }

    public void setupRecycler(){
        if (getActivity() == null) return;

        sectionAdapter = new SectionedRecyclerViewAdapter();
        upcomingSection = new CalendarSection(getActivity(), true, sectionAdapter, upcomingEventList);
        pastSection =  new CalendarSection(getActivity(), false, sectionAdapter, pastEventList);

        upcomingSection.setHasFooter(false);
        pastSection.setHasFooter(false);

        sectionAdapter.addSection(UPCOMING_SECTION_TAG, upcomingSection);
        sectionAdapter.addSection(PAST_SECTION_TAG, pastSection);

        sectionAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                setSectionLoaded();
            }
        });

        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecycler.setAdapter(sectionAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGameReference != null && childEventListener != null)
            mGameReference.removeEventListener(childEventListener);
    }

    /******************************************************************************
     * Firebase
     *****************************************************************************/
    private void loadEventKeys() {
        childEventListener = mGameReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    if ((Boolean) dataSnapshot.getValue())
                            loadEvent(dataSnapshot.getKey());
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    if ((Boolean) dataSnapshot.getValue())
                        loadEvent(dataSnapshot.getKey());
                    else {
                        final String eventKey = dataSnapshot.getKey();
                        // user left game
                        if (mPastEventIds.contains(eventKey)){
                            final int index = mPastEventIds.indexOf(eventKey);
                            if (index > -1) {
                                mPastEventIds.remove(index);
                                pastEventList.remove(index);
                            }
                        } else if (mUpcomingEventIds.contains(eventKey)) {
                            final int index = mUpcomingEventIds.indexOf(eventKey);
                            if (index > -1) {
                                mUpcomingEventIds.remove(index);
                                upcomingEventList.remove(index);
                                updateAdapter(index, UPCOMING_SECTION_TAG, false, true);
                            }
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

    public void loadEvent(final String key) {
        mDatabaseRef.child(REF_EVENTS).child(key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    if (isAdded()) {
                        final Event event = dataSnapshot.getValue(Event.class);
                        if (event == null)
                            return;
                        final String eventKey = dataSnapshot.getKey();
                        event.setEid(eventKey);

                        // do not display if it's not active
                        if (dataSnapshot.hasChild("active")){
                            if (!event.active){
                                final int upcomingIndex = mUpcomingEventIds.indexOf(eventKey);
                                final int pastIndex = mPastEventIds.indexOf(eventKey);
                                if (upcomingIndex > -1){
                                    mUpcomingEventIds.remove(upcomingIndex);
                                    upcomingEventList.remove(upcomingIndex);
                                    updateAdapter(upcomingIndex, UPCOMING_SECTION_TAG, false, true);
                                } else if (pastIndex > -1){
                                    mPastEventIds.remove(pastIndex);
                                    mPastEventIds.remove(pastIndex);
                                    updateAdapter(pastIndex, PAST_SECTION_TAG, false, true);
                                }

                                // set section loaded in case event is only one user has joined
                                // and it is no longer active
                                setSectionLoaded();
                                return;
                            }
                        }

                        final Date eventDate = new Date(event.getEndTime() * 1000);
                        // upcoming events
                        if (new Date().before(eventDate)){
                            final int eventIndex = mUpcomingEventIds.indexOf(eventKey);
                            if (eventIndex > -1) {
                                upcomingEventList.set(eventIndex, event);
                                updateAdapter(eventIndex, UPCOMING_SECTION_TAG, false, false);
                            } else {
                                mUpcomingEventIds.add(eventKey);
                                upcomingEventList.add(event);
                                updateAdapter(upcomingEventList.size() - 1, UPCOMING_SECTION_TAG,
                                        true, false);
                            }
                        } else {
                            // past events
                            final int eventIndex = mPastEventIds.indexOf(eventKey);
                            if (eventIndex > -1) {
                                pastEventList.set(eventIndex, event);
                                updateAdapter(eventIndex, PAST_SECTION_TAG, false, false);
                            } else {
                                mPastEventIds.add(eventKey);
                                pastEventList.add(event);
                                updateAdapter(pastEventList.size() -1, PAST_SECTION_TAG, true, false);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void checkEmptyKeys(){
        mGameReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()){
                    // user has not joined any games
                    setSectionLoaded();
                } else {
                    boolean isEmpty = false;
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        if (child.getValue() != null && (Boolean) child.getValue()) {
                            // user has joined at least one game
                            break;
                        } else {
                            // user has not joined any games
                            isEmpty = true;
                        }
                    }

                    if (isEmpty)
                        setSectionLoaded();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    /******************************************************************************
     * Private methods
     *****************************************************************************/

    void updateAdapter(final int index, final String sectionTag, final boolean isInsertion, final boolean isRemoval){
        if (!isAdded() || getActivity() == null || sectionAdapter == null)
            return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isRemoval){
                    sectionAdapter.notifyItemRemovedFromSection(sectionTag, index);
                } else if (isInsertion){
                    sectionAdapter.notifyItemInsertedInSection(sectionTag, index);
                } else {
                    // an update
                    sectionAdapter.notifyItemChangedInSection(sectionTag, index);
                }
            }
        });
    }

    void setSectionLoaded(){
        if (isAdded() && getActivity() != null){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    upcomingSection.setState(Section.State.LOADED);
                    pastSection.setState(Section.State.LOADED);
                    sectionAdapter.notifyDataSetChanged();
                }
            });
        }
    }
}


