package io.renderapps.balizinha.fragment;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.EventDetailsActivity;
import io.renderapps.balizinha.adapter.ActionAdapter;
import io.renderapps.balizinha.model.Action;
import io.renderapps.balizinha.model.Message;
import io.renderapps.balizinha.model.Player;

import static io.renderapps.balizinha.util.Constants.REF_ACTIONS;
import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;


public class ChatFragment extends Fragment implements View.OnClickListener {

    private Context mContext;
    public static String EVENT_ID = "eid";
    public static String EVENT_TITLE = "title";
    private String eventId;
    private String eventTitle;
    private RecyclerView messagesRecycler;
    private EditText messageField;
    private ImageButton sendButton;
    private Map<String, Player> users;
    private List<Message> messages;
    private List<Action> actionList;
    private ActionAdapter adapter;
    private ProgressBar progressBar;

    // firebase
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseRef;
    private Query query;

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstance(String eid, String title){
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(EVENT_ID, eid);
        args.putString(EVENT_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        if (getArguments() != null) {
            eventId = getArguments().getString(EVENT_ID);
            eventTitle = getArguments().getString(EVENT_TITLE);
        }

        users = new HashMap<>();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        query = databaseRef.child(REF_ACTIONS).orderByChild("event").equalTo(eventId);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_chat, container, false);

        messagesRecycler = rootView.findViewById(R.id.messages_recycler);
        messageField = rootView.findViewById(R.id.messageEditText);
        progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(mContext, R.color.colorPrimary),
                PorterDuff.Mode.SRC_IN);
        sendButton = rootView.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);
        sendButton.setEnabled(false);

        messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (messageField.getText().length() > 0)
                    sendButton.setEnabled(true);
                else
                    sendButton.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // recycler
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setStackFromEnd(true);
//        linearLayoutManager.setReverseLayout(false);
        messagesRecycler.setLayoutManager(linearLayoutManager);

        // adapter
        actionList = new ArrayList<>();
//        messages = new ArrayList<>();
        adapter = new ActionAdapter(getActivity(), actionList, eventTitle);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                progressBar.setVisibility(View.GONE);
                int friendlyMessageCount = adapter.getItemCount();
                int lastVisiblePosition =
                        linearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    messagesRecycler.scrollToPosition(positionStart);
                }
            }
        });
        messagesRecycler.setAdapter(adapter);
        checkEmptyState();
        fetchActions();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        messageField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus){
                    if (adapter.getItemCount() > 0)
                        messagesRecycler.smoothScrollToPosition(adapter.getItemCount() - 1);
                }
            }
        });

        enableMessaging();
    }


    public void checkEmptyState(){
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    public void fetchActions(){
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Action action = dataSnapshot.getValue(Action.class);
                actionList.add(action);
                Collections.sort(actionList, new Comparator<Action>(){
                    public int compare(Action obj1, Action obj2) {
                        // ## Ascending order
                        return Long.valueOf((long)obj1.getCreatedAt()).compareTo((long) obj2.getCreatedAt());
                    }
                });
                updateAdapter();

//                if (action.getType().equals("chat")) {
//                    Message message = new Message(action.getUsername(), action.getUser(), action.getMessage());
//                    if (users.containsKey(action.getUser())){
//                        final Player player = users.get(action.getUser());
//                        createMessage(message, player);
//                    } else {
//                        fetchPlayer(message);
//                    }
//                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

//    public void fetchPlayer(final Message message){
//        databaseRef.child(REF_PLAYERS).child(message.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
//                    final Player player = dataSnapshot.getValue(Player.class);
//                    users.put(message.getUid(), player);
//                    createMessage(message, player);
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//            }
//        });
//    }

//    public void createMessage(Message message, Player player){
//        if (player.getPhotoUrl() != null && !player.getPhotoUrl().isEmpty())
//            message.setPhotoUrl(player.getPhotoUrl());
//        if (player.getName() != null && !player.getName().isEmpty())
//            message.setName(player.getName());
//
//        messages.add(message);
//        updateAdapter();
//    }

    public void updateAdapter(){
        if (isAdded()){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyItemInserted(actionList.size() - 1);
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.sendButton:
                onSendMessage();
        }
    }

    public void onSendMessage(){
        final String message = messageField.getText().toString();
        messageField.setText("");
        final double time = System.currentTimeMillis() / 1000;
        Action action = new Action(eventId, message, time, Action.ACTION_CHAT, firebaseUser.getUid());
        if (firebaseUser.getDisplayName() != null)
            action.setUsername(firebaseUser.getDisplayName());
        String key = databaseRef.child(REF_ACTIONS).push().getKey();
        databaseRef.child(REF_ACTIONS).child(key).setValue(action);
    }

    public void enableMessaging(){
        // enable messaging if current user has joined game
        boolean enable = ((EventDetailsActivity)getActivity()).getUserStatus();
        messageField.setEnabled(enable);
//        sendButton.setEnabled(enable);

        if (!enable)
            messageField.setHint(R.string.messages_hint_disabled);
        else
            messageField.setHint(R.string.messages_hint);
    }
}
