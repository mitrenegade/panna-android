package io.renderapps.balizinha.ui.league;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.model.League;
import io.renderapps.balizinha.service.CloudService;
import io.renderapps.balizinha.util.PhotoHelper;

public class LeagueAdapter extends RecyclerView.Adapter<LeagueAdapter.ViewHolder> {

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.title) TextView title;
        @BindView(R.id.location) TextView location;
        @BindView(R.id.description) TextView description;
        @BindView(R.id.tags) TextView tags;
        @BindView(R.id.events) TextView numOfEvents;
        @BindView(R.id.members) TextView numOfMembers;

        @BindView(R.id.logo) ImageView logo;


        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    // properties
    private Context mContext;
    private ArrayList<League> leagues;
    private Moshi moshi;

    LeagueAdapter(Context context, ArrayList<League> leagues){
        this.mContext = context;
        this.leagues = leagues;
        this.moshi = new Moshi.Builder().build();

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_league, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final League league = leagues.get(position);
        holder.title.setText(league.getName());
        holder.location.setText(league.getCity());
        holder.description.setText(league.getInfo());

        String tags = "";
        if (league.getTags() != null) {
            for (String tag : league.getTags()) {
                tags = tags.concat(tag).concat(", ");
            }
        }

        holder.tags.setText(tags);

        if (league.getPhotoUrl() != null && !league.getPhotoUrl().isEmpty()) {
            PhotoHelper.glideImage(mContext, holder.logo, league.getPhotoUrl(), R.drawable.ic_league_placeholder);
        } else {
            if (mContext != null) {
                PhotoHelper.clearImage(mContext, holder.logo);
                PhotoHelper.glideDrawable(mContext, holder.logo, ContextCompat.getDrawable(mContext, R.drawable.ic_logo));
            }
        }

        playersForLeague(league.getId(), holder.numOfMembers);
        eventsForLeague(league.getId(), holder.numOfEvents);

    }

    private void playersForLeague(final String leagueId, final TextView counter){
        new CloudService(new CloudService.ProgressListener() {
            @Override
            public void onStringResponse(String response) {
                if (response == null || response.isEmpty()){
                    counter.setText("0");
                    return;
                }

                int numOfMembers = 0;
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject resultsObj = jsonObject.getJSONObject("result");
                    Iterator<String> ids = resultsObj.keys();

                    while (ids.hasNext()){
                        final String userId = ids.next();
                        final String status = resultsObj.getString(userId);
                        if (!status.equals("none"))
                            numOfMembers++;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                counter.setText(String.valueOf(numOfMembers));
            }
        }).getLeaguePlayers(leagueId);
    }

    private void eventsForLeague(final String leagueId, final TextView counter){
        new CloudService(new CloudService.ProgressListener() {
            @Override
            public void onStringResponse(String response) {
                if (response == null || response.isEmpty()){
                    counter.setText("0");
                    return;
                }

                int numOfEvents = 0;
                try {

                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject resultsObj = jsonObject.getJSONObject("result");

                    Iterator<String> eventIds = resultsObj.keys();
                    JsonAdapter<Event> jsonAdapter = moshi.adapter(Event.class);

                    while (eventIds.hasNext()){

                        final String eventId = eventIds.next();
                        final JSONObject eventJson = resultsObj.getJSONObject(eventId);

                        // event inactive
                        if (eventJson.has("active") && !eventJson.getBoolean("active"))
                            continue;

                        Event event = jsonAdapter.fromJson(jsonObject.toString());
                        if (event != null)
                            numOfEvents++;
                    }

                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }

                counter.setText(String.valueOf(numOfEvents));
            }
        }).getLeagueEvents(leagueId);
    }

    @Override
    public int getItemCount() {
        return leagues.size();
    }
}