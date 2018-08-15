package io.renderapps.balizinha.ui.event.chat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.ui.event.chat.ActionVH;
import io.renderapps.balizinha.ui.event.chat.ReceivedMessageVH;
import io.renderapps.balizinha.ui.event.chat.SentMessageVH;
import io.renderapps.balizinha.ui.profile.UserProfileActivity;
import io.renderapps.balizinha.model.Action;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.PhotoHelper;

import static io.renderapps.balizinha.model.Action.ACTION_CHAT;
import static io.renderapps.balizinha.model.Action.ACTION_CREATE;
import static io.renderapps.balizinha.model.Action.ACTION_JOIN;
import static io.renderapps.balizinha.model.Action.ACTION_LEAVE;
import static io.renderapps.balizinha.model.Action.ACTION_PAID;
import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;

/**
 * Created by joel
 * on 12/21/17.
 */

public class ActionAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_ACTION = 3;

    private Context mContext;
    private List<Action> messages;
    private String eventTitle;
    private FirebaseUser firebaseUser;

    ActionAdapter(Context context, List<Action> messages, String eventTitle){
        this.mContext = context;
        this.messages = messages;
        this.eventTitle = eventTitle;
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }


    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        final Action action = messages.get(position);
        if (action.getType().equals(ACTION_CHAT)) {
            return (action.getUser().equals(firebaseUser.getUid())) ? VIEW_TYPE_MESSAGE_SENT : VIEW_TYPE_MESSAGE_RECEIVED;
        } else {
            return VIEW_TYPE_ACTION;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageVH(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_recieved, parent, false);
            return new ReceivedMessageVH(mContext, view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_action, parent, false);
            return new ActionVH(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final Action action = messages.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageVH) holder).bind(action.getMessage());
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageVH) holder).bind(action.getUser(), action.getMessage());
                break;
            case VIEW_TYPE_ACTION:
                ((ActionVH) holder).bind(eventTitle, action);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
