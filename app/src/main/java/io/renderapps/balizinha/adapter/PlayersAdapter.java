package io.renderapps.balizinha.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import io.renderapps.balizinha.activity.ChatActivity;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.UserProfileActivity;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.CircleTransform;

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
    private List<Player> players;
    private Context mContext;

    public PlayersAdapter(Context context, List<Player> players){
        this.players = players;
        this.mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Player player = players.get(position);

        holder.name.setText(player.getName());
        if (player.getPhotoUrl() != null && !player.getPhotoUrl().isEmpty()){
            loadImage(holder.photo, player.getPhotoUrl());
        } else
            holder.photo.setImageResource(R.drawable.ic_default_photo);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent userProfileIntent = new Intent(mContext, UserProfileActivity.class);
                userProfileIntent.putExtra(UserProfileActivity.USER_ID, player.getPid());
                mContext.startActivity(userProfileIntent);
                ((ChatActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);

            }
        });
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    private void loadImage(ImageView iv, String photoUrl){
            RequestOptions myOptions = new RequestOptions()
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transform(new CircleTransform(mContext))
                    .placeholder(R.drawable.ic_default_photo);
            // load photo
            Glide.with(mContext)
                    .asBitmap()
                    .apply(myOptions)
                    .load(photoUrl)
                    .into(iv);
    }
}
