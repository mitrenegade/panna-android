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

import java.util.List;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.AttendeesActivity;
import io.renderapps.balizinha.activity.UserProfileActivity;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.PhotoHelper;

/**
 * Simple adapter to show list view of attending players
 */

public class AttendeesAdapter extends RecyclerView.Adapter<AttendeesAdapter.ViewHolder> {

    private List<Player> users;
    private Context mContext;

    public AttendeesAdapter(Context context, List<Player> users){
        this.mContext = context;
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listview_player,
                parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Player player = users.get(position);
        holder.name.setText(player.getName());

        if (player.getCity() != null && !player.getCity().isEmpty()) {
            holder.city.setVisibility(View.VISIBLE);
            holder.city.setText(player.getCity());
        } else {
            holder.city.setVisibility(View.GONE);
        }

        // on-click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent userProfileIntent = new Intent(mContext, UserProfileActivity.class);
                userProfileIntent.putExtra(UserProfileActivity.USER_ID, player.getUid());
                mContext.startActivity(userProfileIntent);
                ((AttendeesActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right,
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
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        private ImageView photo;
        private TextView name;
        private TextView city;

        ViewHolder(View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.list_icon);
            name = itemView.findViewById(R.id.list_primary_text);
            city = itemView.findViewById(R.id.list_secondary_text);
        }
    }
}