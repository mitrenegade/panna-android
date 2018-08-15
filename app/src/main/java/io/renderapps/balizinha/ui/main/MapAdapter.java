package io.renderapps.balizinha.ui.main;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    MapAdapter(Context context, List<Event> events){
        this.mContext = context;
        this.events = events;
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
}
