package io.renderapps.balizinha.ui.calendar;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Event;

public class CalendarSection extends Section {

    private Context context;
    private String header;

    private SectionedRecyclerViewAdapter adapter;
    private ArrayList<Event> events;

    private boolean expanded = true;
    private boolean IS_UPCOMING;

    CalendarSection(Context context, boolean upcoming, SectionedRecyclerViewAdapter adapter, ArrayList<Event> events) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.item_section_event)
                .headerResourceId(R.layout.item_league_header)
                .loadingResourceId(R.layout.item_section_loading)
                .build());

        this.context = context;
        this.events = events;
        this.IS_UPCOMING = upcoming;
        this.adapter = adapter;

        if (context != null){
            header = (upcoming) ? context.getString(R.string.upcoming_games) : context.getString(R.string.past_games);
        }
    }

    @Override
    public int getContentItemsTotal() {
        return expanded ? events.size() : 0;
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new CalendarVH(context, IS_UPCOMING, view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        final Event event = events.get(position);
        ((CalendarVH) holder).bind(event);
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new CalendarHeaderVH(view);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
        final CalendarHeaderVH headerHolder = (CalendarHeaderVH) holder;
        headerHolder.header.setText(header);
    }

    class CalendarHeaderVH extends RecyclerView.ViewHolder {

        @BindView(R.id.header)
        TextView header;
        @BindView(R.id.league_indicator)
        ImageView expandCollapseIndicator;

        @OnClick(R.id.league_indicator) void onExpandCollapse(){
            expanded = !expanded;
            expandCollapseIndicator.setImageResource(expanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);

            if (adapter != null)
                adapter.notifyDataSetChanged();
        }

        CalendarHeaderVH(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
