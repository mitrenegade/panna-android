package io.renderapps.balizinha.ui.main;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Event;

/**
 *
 * Displays upcoming events
 */

public class MapAdapter extends RecyclerView.Adapter<MapViewHolder> {

    // properties
    private Context mContext;
    private List<Event> events;
    private List<Event> originalEvents;
    private List<String> eventIds;
    private boolean showingCluster = false;

    public MapAdapter(Context context, List<Event> events, List<String> eventIds){
        this.mContext = context;
        this.events = events;
        this.eventIds = eventIds;

        originalEvents = new ArrayList<>();
        originalEvents.addAll(events);
    }

    @NonNull
    @Override
    public MapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_map, parent, false);
        return new MapViewHolder(view, mContext);
    }

    @Override
    public void onBindViewHolder(@NonNull final MapViewHolder holder, final int position) {
        final Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void showClusterEvents(List<Event> clusterEvents){
        if (isShowingCluster())
            addOriginalEvents();

        originalEvents.clear();
        originalEvents.addAll(events);

        events.clear();
        events.addAll(clusterEvents);

        showingCluster = true;
        notifyDataSetChanged();
    }

    // ensures that originalEvents always contains all events and not events from last cluster
    private void addOriginalEvents(){
        events.clear();
        events.addAll(originalEvents);
    }

    public void clearClusterEvents(){
        if (showingCluster) {

            events.clear();
            events.addAll(originalEvents);

            showingCluster = false;
            notifyDataSetChanged();
        }
    }

    public void removeEvent(Event event){
        final int eventIndex = originalEvents.indexOf(event);
        if (eventIndex > -1){
            originalEvents.remove(eventIndex);
            eventIds.remove(eventIndex);
        }

        final int index = events.indexOf(event);
        if (index > -1){
            events.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void addEvent(Event event){
        originalEvents.add(event);
        eventIds.add(event.eid);
        if (!isShowingCluster()) {
            events.add(event);
            notifyItemInserted(events.size() - 1);
        }
    }

    public boolean isShowingCluster() {
        return showingCluster;
    }
}
