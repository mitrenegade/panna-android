package io.renderapps.balizinha.ui.event.chat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;


import java.util.HashMap;
import java.util.Map;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.ui.event.EventDetailsActivity;
import io.renderapps.balizinha.ui.profile.UserProfileActivity;
import io.renderapps.balizinha.util.PhotoHelper;

import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;

public class ReceivedMessageVH extends RecyclerView.ViewHolder {

    private Context mContext;
    private Map<String, Player> currentUsers;

    private ImageView photo;
    private TextView defaultPhoto;
    private TextView message;
    private TextView username;

    public ReceivedMessageVH(Context mContext, View itemView) {
        super(itemView);
        this.mContext = mContext;
        currentUsers = new HashMap<>();

        photo = itemView.findViewById(R.id.user_photo);
        defaultPhoto = itemView.findViewById(R.id.default_photo);
        message = itemView.findViewById(R.id.message);
        username = itemView.findViewById(R.id.user_name);
    }

    public void bind(String userId, String message) {
        this.message.setText(message);

        if (currentUsers.containsKey(userId)){
            final Player player = currentUsers.get(userId);
            showMessage(this, player);
        } else
            fetchUser(this, userId);
    }


    private void fetchUser(final ReceivedMessageVH holder, final String uid){
        FirebaseDatabase.getInstance().getReference().child(REF_PLAYERS).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    final Player player = dataSnapshot.getValue(Player.class);
                    if (player == null) return;

                    player.setUid(uid);
                    currentUsers.put(uid, player);
                    showMessage(holder, player);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void showMessage(final ReceivedMessageVH holder, final Player player){
        if (player.getName() != null && !player.getName().isEmpty())
            holder.username.setText(player.getName());

        // photo
        FirebaseStorage.getInstance().getReference()
                .child("images/player").child(player.getUid()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                if (uri != null){
                    holder.defaultPhoto.setVisibility(View.GONE);
                    holder.photo.setVisibility(View.VISIBLE);

                    PhotoHelper.glideImage(mContext, holder.photo, uri.toString(), R.drawable.ic_default_photo);
                } else {
                    showDefaultImage(holder, player);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                showDefaultImage(holder, player);
            }
        });

        // to go user profile on photo click
        holder.photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent userProfileIntent = new Intent(mContext, UserProfileActivity.class);
                userProfileIntent.putExtra(UserProfileActivity.USER_ID, player.getUid());
                mContext.startActivity(userProfileIntent);
                ((EventDetailsActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);

            }
        });
    }

    private void showDefaultImage(ReceivedMessageVH viewHolder, Player player){
        viewHolder.defaultPhoto.setVisibility(View.VISIBLE);
        viewHolder.photo.setVisibility(View.GONE);
        viewHolder.defaultPhoto.setText("");

        if (player.getName() != null && !player.getName().isEmpty())
            viewHolder.defaultPhoto.setText(String.valueOf(player.getName().charAt(0)));
        else
            viewHolder.defaultPhoto.setText("-");
    }


}