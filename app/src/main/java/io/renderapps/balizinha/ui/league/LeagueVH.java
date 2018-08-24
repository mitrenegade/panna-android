package io.renderapps.balizinha.ui.league;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;


import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.League;
import io.renderapps.balizinha.ui.main.MainActivity;
import io.renderapps.balizinha.util.PhotoHelper;

class LeagueVH extends RecyclerView.ViewHolder {
    private Context mContext;

    @BindView(R.id.title) TextView title;
    @BindView(R.id.location) TextView location;
    @BindView(R.id.description) TextView description;
    @BindView(R.id.tags) TextView tags;
    @BindView(R.id.events) TextView numOfEvents;
    @BindView(R.id.members) TextView numOfMembers;
    @BindView(R.id.logo) ImageView logo;

    LeagueVH(Context context, View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);

        this.mContext = context;
    }

    public void bind(final League league, final boolean isOtherLeague){

        // reset
        location.setText("");
        description.setText("");
        tags.setText("");

        logo.setAlpha((float)1.0);
        title.setAlpha((float)1.0);
        location.setAlpha((float)1.0);

        title.setText(league.getName());
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mContext != null && mContext instanceof MainActivity){
                    if (!((MainActivity) mContext).isDestroyed() && !((MainActivity) mContext).isFinishing()){
                        Intent leagueIntent = new Intent(mContext, LeagueActivity.class);
                        leagueIntent.putExtra(LeagueActivity.EXTRA_LEAGUE, league);
                        mContext.startActivity(leagueIntent);
                    }
                }
            }
        });

        if (league.isPrivate() && isOtherLeague) {
            // private league
            setLocation("Private");
            logo.setAlpha((float)0.5);
            title.setAlpha((float)0.5);
            location.setAlpha((float)0.5);
        } else {
            setLocation(league.getCity());
            setDescription(league.getInfo());
            setTags(league.getTags());
        }

        setLogo(league.getId());
        setLeaguePlayers(league.getPlayerCount());
        setLeagueEvents(league.getEventCount());
    }

    private void setLocation(String city){
        if (city == null || city.isEmpty()){
            location.setVisibility(View.GONE);
        } else {
            location.setVisibility(View.VISIBLE);
            location.setText(city);
        }
    }

    private void setDescription(String info){
        if (info == null || info.isEmpty()){
            description.setVisibility(View.GONE);
        } else {
            description.setVisibility(View.VISIBLE);
            description.setText(info);
        }
    }

    private void setTags(ArrayList<String> leagueTags){
        if (leagueTags == null || leagueTags.isEmpty()){
            tags.setVisibility(View.GONE);
            return;
        }

        tags.setVisibility(View.VISIBLE);

        // remove empty tags
        leagueTags.removeAll(Arrays.asList("", null));

        final String tags = TextUtils.join(", ", leagueTags);
        this.tags.setText(tags);
    }

    private void setLogo(String leagueId){
        FirebaseStorage.getInstance().getReference()
                .child("images/league").child(leagueId).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                if (uri != null){
                    PhotoHelper.glideImage(mContext, logo, uri.toString(), R.drawable.ic_league_placeholder);
                } else {
                    PhotoHelper.clearImage(mContext, logo);
                    logo.setImageResource(R.drawable.ic_league_default_photo);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                PhotoHelper.clearImage(mContext, logo);
                logo.setImageResource(R.drawable.ic_league_default_photo);
            }
        });
    }

    private void setLeagueEvents(int numOfEvents){
        this.numOfEvents.setText(String.valueOf(numOfEvents));
    }

    private void setLeaguePlayers(int numOfPlayers){
        this.numOfMembers.setText(String.valueOf(numOfPlayers));
    }
}
