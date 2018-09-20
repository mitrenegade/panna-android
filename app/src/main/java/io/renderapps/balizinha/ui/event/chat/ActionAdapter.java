package io.renderapps.balizinha.ui.event.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Action;


import static io.renderapps.balizinha.model.Action.ACTION_CHAT;


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
