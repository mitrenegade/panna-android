package io.renderapps.balizinha.ui.league;

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

import butterknife.BindView;
import butterknife.ButterKnife;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.ui.profile.UserProfileActivity;
import io.renderapps.balizinha.util.PhotoHelper;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.ViewHolder> {

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.photo) ImageView photo;
        @BindView(R.id.default_photo) TextView defaultPhoto;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private Context mContext;
    private ArrayList<Player> members;

    public MembersAdapter(Context context, ArrayList<Player> members){
        this.mContext = context;
        this.members = members;
    }

    @NonNull
    @Override
    public MembersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MembersAdapter.ViewHolder holder, int position) {
        final Player player = members.get(position);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mContext != null && mContext instanceof LeagueActivity) {

                    Intent userProfileIntent = new Intent(mContext, UserProfileActivity.class);
                    userProfileIntent.putExtra(UserProfileActivity.USER_ID, player.getUid());

                    mContext.startActivity(userProfileIntent);
                    ((LeagueActivity) mContext).overridePendingTransition(R.anim.anim_slide_in_right,
                            R.anim.anim_slide_out_left);
                }
            }
        });

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
    }

    private void showDefaultImage(ViewHolder viewHolder, Player player){
        viewHolder.defaultPhoto.setVisibility(View.VISIBLE);
        viewHolder.photo.setVisibility(View.GONE);
        viewHolder.defaultPhoto.setText("");

//        PhotoHelper.clearImage(mContext, viewHolder.photo);
        if (player.getName() != null && !player.getName().isEmpty())
            viewHolder.defaultPhoto.setText(String.valueOf(player.getName().charAt(0)));
        else
            viewHolder.defaultPhoto.setText("-");
    }

    @Override
    public int getItemCount() {
        return members.size();
    }
}