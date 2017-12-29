package io.renderapps.balizinha.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.renderapps.balizinha.activity.ChatActivity;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.MainActivity;
import io.renderapps.balizinha.model.Action;
import io.renderapps.balizinha.model.Event;

public class CalendarFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    // properties
    private final String UPCOMING_SECTION_TAG = "upcomingTag";
    private final String PAST_SECTION_TAG = "pastTag";
    private Context mContext;

    List<Event> upcomingEventList;
    List<Event> pastEventList;
    private List<String> mUpcomingEventIds;
    private List<String> mPastEventIds;
    private Calendar calendar;

    private SimpleDateFormat mFormatter = new SimpleDateFormat("EE, MMM dd @ h:mm aa", Locale.getDefault());
    Date todaysDate = new Date();

    // views
    SectionedRecyclerViewAdapter sectionAdapter;
    ExpandableContactsSection upcomingSection;
    ExpandableContactsSection pastSection;

    // firebase
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mGameReference;
    private FirebaseUser mFirebaseUser;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseAuth auth;

    public CalendarFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init
        auth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mFirebaseUser = firebaseAuth.getCurrentUser();
            }
        };
        if (mFirebaseUser != null)
            mGameReference = mDatabaseRef.child("userEvents").child(mFirebaseUser.getUid());
        calendar = Calendar.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_calendar, container, false);

        // toolbar
        Toolbar toolbar = root.findViewById(R.id.calendar_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.title_calendar);

        sectionAdapter = new SectionedRecyclerViewAdapter();
        upcomingSection = new ExpandableContactsSection(ExpandableContactsSection.UPCOMING);
        pastSection = new ExpandableContactsSection(ExpandableContactsSection.PAST);

        upcomingSection.setHasFooter(false);
        pastSection.setHasFooter(false);
        sectionAdapter.addSection(UPCOMING_SECTION_TAG, upcomingSection);
        sectionAdapter.addSection(PAST_SECTION_TAG, pastSection);

        RecyclerView recyclerView = root.findViewById(R.id.calendar_reycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(sectionAdapter);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        upcomingEventList = new ArrayList<>();
        pastEventList = new ArrayList<>();
        mUpcomingEventIds = new ArrayList<>();
        mPastEventIds = new ArrayList<>();
        upcomingSection.setState(Section.State.LOADING);
        pastSection.setState(Section.State.LOADING);
        sectionAdapter.notifyDataSetChanged();

        sectionAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                upcomingSection.setState(Section.State.LOADED);
                pastSection.setState(Section.State.LOADED);
                sectionAdapter.notifyDataSetChanged();
            }
        });
        upcomingSection.setList(upcomingEventList);
        pastSection.setList(pastEventList);
        checkEmptyKeys();
        loadEventKeys();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(authStateListener != null){
            auth.removeAuthStateListener(authStateListener);
        }
    }

    /******************************************************************************
     * Initialize map
     *****************************************************************************/
    private void loadEventKeys() {
        mGameReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    if (dataSnapshot.getValue(Boolean.class)) // true
                            loadEvent(dataSnapshot.getKey());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    if (dataSnapshot.getValue(Boolean.class)) // true
                        loadEvent(dataSnapshot.getKey());
                    else {
                        final String eventKey = dataSnapshot.getKey();
                        // user left game
                        if (mPastEventIds.contains(eventKey)){
                            final int index = mPastEventIds.indexOf(eventKey);
                            if (index > -1) {
                                mPastEventIds.remove(index);
                                pastEventList.remove(index);
                                if (isAdded()) {
                                    sectionAdapter.notifyItemRemovedFromSection(PAST_SECTION_TAG, index);
                                }
                            }
                        } else if (mUpcomingEventIds.contains(eventKey)) {
                            final int index = mUpcomingEventIds.indexOf(eventKey);
                            if (index > -1) {
                                mUpcomingEventIds.remove(index);
                                upcomingEventList.remove(index);
                                if (isAdded()) {
                                    sectionAdapter.notifyItemRemovedFromSection(UPCOMING_SECTION_TAG, index);
                                }
                            }
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

    public void loadEvent(final String key) {
        mDatabaseRef.child("events").child(key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    if (isAdded()) {
                        final Event event = dataSnapshot.getValue(Event.class);
                        event.setEid(dataSnapshot.getKey());
                        final Date eventDate = new Date(event.getCreatedAt() * 1000);
                        final String eventKey = dataSnapshot.getKey();

                        // upcoming events
                        if (eventDate.after(todaysDate) || eventDate.equals(todaysDate)){
                            final int eventIndex = mUpcomingEventIds.indexOf(eventKey);
                            if (eventIndex > -1) {
                                upcomingEventList.set(eventIndex, event);
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        sectionAdapter.notifyItemChangedInSection(UPCOMING_SECTION_TAG, eventIndex);
                                    }
                                });

                            } else {
                                mUpcomingEventIds.add(eventKey);
                                upcomingEventList.add(event);
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        sectionAdapter.notifyItemInsertedInSection
                                                (UPCOMING_SECTION_TAG, upcomingEventList.size() - 1);
                                    }
                                });
                            }
                        } else {
                            // past events
                            final int eventIndex = mPastEventIds.indexOf(eventKey);
                            if (eventIndex > -1) {
                                pastEventList.set(eventIndex, event);
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        sectionAdapter.notifyItemChangedInSection(PAST_SECTION_TAG, eventIndex);
                                    }
                                });

                            } else {
                                mPastEventIds.add(eventKey);
                                pastEventList.add(event);
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        sectionAdapter.notifyItemInsertedInSection
                                                (PAST_SECTION_TAG, pastEventList.size() - 1);
                                    }
                                });
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    public void checkEmptyKeys(){
        mGameReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()){
                    if (isAdded())
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                upcomingSection.setState(Section.State.LOADED);
                                pastSection.setState(Section.State.LOADED);
                                sectionAdapter.notifyDataSetChanged();
                            }
                        });
                } else {
                    boolean isEmpty = false;
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        if (child.getValue(Boolean.class))
                            break;
                        else
                            isEmpty = true;
                    }
                    if (isEmpty && isAdded())
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

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void onUserLeave(final Event event){
        // dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.leave_event));
        builder.setCancelable(false);

        builder.setPositiveButton(
                "Yes, I'm sure",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mDatabaseRef.child("userEvents").child(mFirebaseUser.getUid())
                                .child(event.getEid()).setValue(false);
                        mDatabaseRef.child("eventUsers").child(event.getEid())
                                .child(mFirebaseUser.getUid()).setValue(false);

                        // create and add event action
                        Action action = new Action(event.getEid(), "", event.getCreatedAt(),
                                Action.ACTION_LEAVE, mFirebaseUser.getUid());
                        if (mFirebaseUser.getDisplayName() != null)
                            action.setUsername(mFirebaseUser.getDisplayName());
                        String actionKey = mDatabaseRef.child("action").push().getKey();
                        mDatabaseRef.child("action").child(actionKey).setValue(action);

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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {}

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    private class ExpandableContactsSection extends Section {
        final static int PAST = 0;
        final static int UPCOMING = 1;
        final int mHeader;

        String title;
        List<Event> list;
        boolean expanded = true;

        ExpandableContactsSection(int header) {
            super(new SectionParameters.Builder(R.layout.item_section_event)
                    .headerResourceId(R.layout.item_section_header)
                    .loadingResourceId(R.layout.item_section_loading)
                    .build());

            this.mHeader = header;
            this.list = Collections.emptyList();

            switch (mHeader) {
                case PAST:
                    this.title = getString(R.string.past_events);
                    break;
                case UPCOMING:
                    this.title = getString(R.string.upcoming_events);
                    break;
            }
        }

        public int getmHeader() {
            return mHeader;
        }

        void setList(List<Event> list) {
            this.list = list;
        }

        @Override
        public int getContentItemsTotal() {
            return expanded ? list.size() : 0;
        }

        @Override
        public RecyclerView.ViewHolder getItemViewHolder(View view) {
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
            final ItemViewHolder itemHolder = (ItemViewHolder) holder;
            final Event event = list.get(position);

            // payment
            if (event.paymentRequired)
                ((ItemViewHolder) holder).paymentView.setVisibility(View.VISIBLE);
            else
                ((ItemViewHolder) holder).paymentView.setVisibility(View.GONE);

            if (!event.getOwner().equals(mFirebaseUser.getUid())) {
                if (mHeader == UPCOMING) {
                    itemHolder.editButton.setVisibility(View.VISIBLE);
                    itemHolder.editButton.setText(getString(R.string.delete));
                    itemHolder.editButton.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.white));
                    itemHolder.editButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            onUserLeave(event);
                        }
                    });
                }
            } else
                itemHolder.editButton.setVisibility(View.GONE);

            itemHolder.title.setText(event.getName().concat(" ").concat("(").concat(event.getType())
                    .concat(")"));
            String address = event.getCity();
            if (event.getState() != null)
                address = address.concat(", ").concat(event.getState());
            itemHolder.address.setText(address);

            // Date
            formatTime(event.getStartTime());
            String time = mFormatter.format(calendar.getTime());
            itemHolder.time.setText(time);

            itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    intent.putExtra(ChatActivity.EVENT_ID, event.getEid());
                    intent.putExtra(ChatActivity.EVENT_TITLE, event.getName());
                    intent.putExtra(ChatActivity.EVENT_TYPE, event.getType());
                    intent.putExtra(ChatActivity.EVENT_INFO, event.getInfo());
                    intent.putExtra(ChatActivity.EVENT_CITY, event.getCity());
                    intent.putExtra(ChatActivity.EVENT_STATE, event.getState());
                    intent.putExtra(ChatActivity.EVENT_CREATOR, event.getOwner());
                    intent.putExtra(ChatActivity.EVENT_CREATED_AT, event.getCreatedAt());
                    intent.putExtra(ChatActivity.EVENT_TIME, event.getStartTime());
                    intent.putExtra(ChatActivity.EVENT_PAYMENT, event.paymentRequired);
                    intent.putExtra(ChatActivity.EVENT_LAUNCH_MODE, true);

                    // user already joined game if accessing from Calendar
                    intent.putExtra(ChatActivity.EVENT_PLAYER_STATUS, true);
                    if (mHeader == PAST)
                        intent.putExtra(ChatActivity.EVENT_STATUS, true);
                    else
                        intent.putExtra(ChatActivity.EVENT_STATUS, false);
                    startActivity(intent);
                }
            });

            // set current players in game
            mDatabaseRef.child("eventUsers").child(event.getEid()).addListenerForSingleValueEvent(new ValueEventListener() {
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
                        itemHolder.playerCount.setText(String.valueOf(count));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @Override
        public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
            return new HeaderViewHolder(view);
        }

        @Override
        public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
            final HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.tvTitle.setText(title);
            headerHolder.rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    expanded = !expanded;
                    headerHolder.imgArrow.setImageResource(
                            expanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
                    sectionAdapter.notifyDataSetChanged();
                }
            });
        }

        void formatTime(double sec){
            calendar.clear();
            calendar.setTimeInMillis((long) sec * 1000);
            int unroundedMinutes = calendar.get(Calendar.MINUTE);
            int mod = unroundedMinutes % 15;
            calendar.add(Calendar.MINUTE, mod < 8 ? -mod : (15-mod));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final View rootView;
        private final TextView tvTitle;
        private final ImageView imgArrow;

        HeaderViewHolder(View view) {
            super(view);
            rootView = view;
            tvTitle = view.findViewById(R.id.header_title);
            imgArrow = view.findViewById(R.id.arrow);
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        private final ImageView paymentView;
        private final TextView title;
        private final TextView time;
        private final TextView address;
        private final TextView playerCount;
        private final Button editButton;

        ItemViewHolder(View view) {
            super(view);

            paymentView = view.findViewById(R.id.payment_required);
            title = view.findViewById(R.id.title);
            time = view.findViewById(R.id.time);
            address = view.findViewById(R.id.address);
            playerCount = view.findViewById(R.id.player_count);
            editButton = view.findViewById(R.id.edit_delete_button);
        }
    }
}


