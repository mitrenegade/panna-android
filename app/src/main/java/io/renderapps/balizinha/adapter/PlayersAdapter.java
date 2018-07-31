package io.renderapps.balizinha.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;

import io.renderapps.balizinha.activity.EventDetailsActivity;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.UserProfileActivity;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.PhotoHelper;

/**
 * Created by joel
 * on 12/15/17.
 */

public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.ViewHolder> {

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView name;

        ViewHolder(View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.player_photo);
            name = itemView.findViewById(R.id.player_name);
        }
    }

    // properties
    private ArrayList<Player> players;
    private Context mContext;

    public PlayersAdapter(Context context, ArrayList<Player> players){
        this.players = players;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player, parent,
                false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Player player = players.get(position);

        holder.name.setText(player.getName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent userProfileIntent = new Intent(mContext, UserProfileActivity.class);
                userProfileIntent.putExtra(UserProfileActivity.USER_ID, player.getUid());
                mContext.startActivity(userProfileIntent);
                ((EventDetailsActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right,
                        R.anim.anim_slide_out_left);
            }
        });

        FirebaseStorage.getInstance().getReference()
                .child("images/player").child(player.getUid()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                if (uri != null){
                    PhotoHelper.glideImage(mContext, holder.photo, uri.toString(), R.drawable.ic_default_photo);
                } else {
                    PhotoHelper.clearImage(mContext, holder.photo);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                PhotoHelper.clearImage(mContext, holder.photo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return players.size();
    }
}
