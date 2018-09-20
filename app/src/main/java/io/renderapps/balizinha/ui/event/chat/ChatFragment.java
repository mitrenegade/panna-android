package io.renderapps.balizinha.ui.event.chat;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Action;
import io.renderapps.balizinha.ui.event.EventDetailsActivity;
import io.renderapps.balizinha.util.KeyboardUtils;

import static io.renderapps.balizinha.util.Constants.REF_ACTIONS;


public class ChatFragment extends Fragment {

    public static String EVENT_ID = "eid";
    public static String EVENT_TITLE = "title";
    private String eventId;
    private String eventTitle;
    private List<Action> actionList;
    private ActionAdapter adapter;
    private KeyboardUtils keyboardUtils;


    // views
    @BindView(R.id.messages_recycler) RecyclerView messagesRecycler;
    @BindView(R.id.messageEditText) EditText messageField;
    @BindView(R.id.sendButton) ImageButton sendButton;
    @BindView(R.id.progressBar) ProgressBar progressBar;

    @OnClick(R.id.sendButton) void onSendMessage(){
        final String message = messageField.getText().toString();
        messageField.setText("");

        final double time = System.currentTimeMillis() / 1000;
        Action action = new Action(eventId, message, time, Action.ACTION_CHAT, firebaseUser.getUid());

        if (firebaseUser.getDisplayName() != null)
            action.setUsername(firebaseUser.getDisplayName());

        String key = databaseRef.child(REF_ACTIONS).push().getKey();
        if (key != null)
            databaseRef.child(REF_ACTIONS).child(key).setValue(action);
    }

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
        if (getArguments() != null) {
            eventId = getArguments().getString(EVENT_ID);
            eventTitle = getArguments().getString(EVENT_TITLE);
        }

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        query = databaseRef.child(REF_ACTIONS).orderByChild("event").equalTo(eventId);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_chat, container, false);
        ButterKnife.bind(this, rootView);

        setupKeyboard(rootView);
        setupRecycler();
        checkEmptyState();
        fetchActions();

        return rootView;
    }

    void setupKeyboard(View root){

        // allows EditText to remain above keyboard while status bar is translucent
        // track bug for fix: https://issuetracker.google.com/issues/36986276
        if (getActivity() != null)
            keyboardUtils = new KeyboardUtils(getActivity(), root);

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

        messageField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus){
                    if (adapter.getItemCount() > 0)
                        messagesRecycler.smoothScrollToPosition(adapter.getItemCount() - 1);
                }
            }
        });

        sendButton.setEnabled(false);
        enableMessaging();
    }

    void setupRecycler(){
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setStackFromEnd(true);
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
    }

    public void checkEmptyState(){
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void fetchActions(){
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                Action action = dataSnapshot.getValue(Action.class);
                actionList.add(action);
                Collections.sort(actionList, new Comparator<Action>(){
                    public int compare(Action obj1, Action obj2) {
                        // ## Ascending order
                        return Long.compare((long) obj1.getCreatedAt(), (long) obj2.getCreatedAt());
                    }
                });

                updateAdapter();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void updateAdapter(){
        if (isAdded() && getActivity() != null){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyItemInserted(actionList.size() - 1);
                }
            });
        }
    }

    public void enableMessaging(){
        // enable messaging if current user has joined game
        if (getActivity() != null){
            boolean enable = ((EventDetailsActivity)getActivity()).getUserStatus();
            messageField.setEnabled(enable);

            if (!enable)
                messageField.setHint(R.string.messages_hint_disabled);
            else
                messageField.setHint(R.string.messages_hint);
        }
    }
}
