package io.renderapps.balizinha.ui.league;

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
import io.renderapps.balizinha.model.League;

public class LeagueSection extends Section {

    private Context context;
    private String header;

    private SectionedRecyclerViewAdapter adapter;
    private ArrayList<League> leagues;

    private boolean expanded = true;

    LeagueSection(Context context, SectionedRecyclerViewAdapter adapter, String header, ArrayList<League> leagues) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.item_league)
                .headerResourceId(R.layout.item_league_header)
                .loadingResourceId(R.layout.item_section_loading)
                .build());

        this.context = context;
        this.header = header;
        this.leagues = leagues;
        this.adapter = adapter;
    }

    @Override
    public int getContentItemsTotal() {
        return expanded ? leagues.size() : 0;
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new LeagueVH(context, view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        final League league = leagues.get(position);
        ((LeagueVH) holder).bind(league);
    }


    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new LeagueHeaderVH(view);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
        final LeagueHeaderVH headerHolder = (LeagueHeaderVH) holder;
        headerHolder.header.setText(header);
    }

    class LeagueHeaderVH extends RecyclerView.ViewHolder {

        @BindView(R.id.header) TextView header;
        @BindView(R.id.league_indicator) ImageView expandCollapseIndicator;

        @OnClick(R.id.league_indicator) void onExpandCollapse(){
            expanded = !expanded;
            expandCollapseIndicator.setImageResource(expanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);

            if (adapter != null)
                adapter.notifyDataSetChanged();
        }

        LeagueHeaderVH(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
