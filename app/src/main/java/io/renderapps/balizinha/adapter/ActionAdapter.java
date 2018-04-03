package io.renderapps.balizinha.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.EventDetailsActivity;
import io.renderapps.balizinha.activity.UserProfileActivity;
import io.renderapps.balizinha.model.Action;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.CircleTransform;
import io.renderapps.balizinha.util.GeneralHelpers;

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

public class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.ViewHolder> {

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView message;
        TextView username;
        TextView action;

        ViewHolder(View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.user_photo);
            message = itemView.findViewById(R.id.message);
            username = itemView.findViewById(R.id.user_name);
            action = itemView.findViewById(R.id.action);
        }
    }

    private Context mContext;
    private List<Action> messages;
    private Map<String, Player> currentUsers;
    private String eventTitle;
    private SimpleDateFormat mFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private FirebaseUser firebaseUser;
    private DatabaseReference databaseRef;

    public ActionAdapter(Context context, List<Action> messages, String eventTitle){
        this.mContext = context;
        this.messages = messages;
        this.eventTitle = eventTitle;
        this.currentUsers = new HashMap<>();
        this.firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        this.databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Action action = messages.get(position);
        final Date date = new Date((long)action.getCreatedAt() * 1000);
        switch (action.getType()){
            case ACTION_CREATE:
                if (action.getUser().equals(firebaseUser.getUid()))
                    holder.action.setText("You created ".concat(eventTitle).concat(" on ").concat(mFormatter.format(date)));
                else {
                    if (action.getUsername() != null)
                        holder.action.setText(action.getUsername().concat(" created ").concat(eventTitle)
                                .concat(" on ").concat(mFormatter.format(date)));
                    else
                        holder.action.setText(eventTitle.concat(" created on ").concat(mFormatter.format(date)));
                }
                showAction(holder);
                break;
            case ACTION_JOIN:
                if (action.getUser().equals(firebaseUser.getUid()))
                    holder.action.setText("You joined ".concat(eventTitle));
                else {
                    if (action.getUsername() != null)
                        holder.action.setText(action.getUsername().concat(" joined ").concat(eventTitle));
                    else
                     holder.action.setText("A new user has joined ".concat(eventTitle));
                }
                showAction(holder);
                break;
            case ACTION_PAID:
                if (action.getUser().equals(firebaseUser.getUid()))
                    holder.action.setText("You paid for ".concat(eventTitle));
                else {
                    if (action.getUsername() != null)
                        holder.action.setText(action.getUsername().concat(" paid for ").concat(eventTitle));
                    else
                        holder.action.setText("A new user has paid for ".concat(eventTitle));
                }
                showAction(holder);
                break;
            case ACTION_LEAVE:
                if (action.getUser().equals(firebaseUser.getUid()))
                    holder.action.setText("You left ".concat(eventTitle));
                else {
                    if (action.getUsername() != null)
                        holder.action.setText(action.getUsername().concat(" has left ").concat(eventTitle));
                    else
                        holder.action.setText("A user has left ".concat(eventTitle));
                }
                showAction(holder);
                break;
            case ACTION_CHAT:
                holder.action.setVisibility(View.GONE);
                holder.photo.setVisibility(View.VISIBLE);
                holder.username.setVisibility(View.VISIBLE);
                holder.message.setVisibility(View.VISIBLE);

                // set current message
                holder.message.setText(action.getMessage());
                if (action.getUsername() != null)
                    holder.username.setText(action.getUsername());

                // layout message
                if (action.getUser().equals(firebaseUser.getUid())){
                    holder.photo.setVisibility(View.GONE);
                    holder.username.setVisibility(View.GONE);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)holder.message.getLayoutParams();
                    params.gravity = Gravity.END;
                    holder.message.setLayoutParams(params);
                } else {
                    holder.photo.setVisibility(View.VISIBLE);
                    holder.username.setVisibility(View.VISIBLE);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)holder.message.getLayoutParams();
                    params.gravity = Gravity.START;
                    holder.message.setLayoutParams(params);
                }

                // fetch user for each msg
                if (currentUsers.containsKey(action.getUser())){
                    final Player player = currentUsers.get(action.getUser());
                   showMessage(holder, player);
                } else
                    fetchUser(holder, action.getUser());
                break;
        }
    }

    private void showAction(ViewHolder holder){
        holder.photo.setVisibility(View.GONE);
        holder.username.setVisibility(View.GONE);
        holder.message.setVisibility(View.GONE);
        holder.action.setVisibility(View.VISIBLE);
    }

    private void showMessage(ViewHolder holder, final Player player){
        if (player.getName() != null && !player.getName().isEmpty())
            holder.username.setText(player.getName());
        if (player.getPhotoUrl() != null && !player.getPhotoUrl().isEmpty())
            loadImage(holder.photo, player.getPhotoUrl());

        // to go user profile on photo click
        holder.photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent userProfileIntent = new Intent(mContext, UserProfileActivity.class);
                userProfileIntent.putExtra(UserProfileActivity.USER_ID, player.getPid());
                mContext.startActivity(userProfileIntent);
                ((EventDetailsActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);

            }
        });
    }

    private void fetchUser(final ViewHolder holder, final String uid){
        databaseRef.child(REF_PLAYERS).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    final Player player = dataSnapshot.getValue(Player.class);
                    player.setPid(uid);
                    currentUsers.put(uid, player);
                    showMessage(holder, player);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private void loadImage(ImageView iv, String photoUrl){
        GeneralHelpers.glideImage(mContext, iv, photoUrl, R.drawable.ic_default_photo);

//        RequestOptions myOptions = new RequestOptions()
//                .fitCenter()
//                .diskCacheStrategy(DiskCacheStrategy.ALL)
//                .transform(new CircleTransform(mContext))
//                .placeholder(R.drawable.ic_default_photo);
//        // load photo
//        if (mContext != null && !((AppCompatActivity)mContext).isFinishing()) {
//            Glide.with(mContext)
//                    .asBitmap()
//                    .apply(myOptions)
//                    .load(photoUrl)
//                    .into(iv);
//        }
    }
}
