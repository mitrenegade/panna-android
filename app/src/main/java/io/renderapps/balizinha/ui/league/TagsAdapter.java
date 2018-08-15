package io.renderapps.balizinha.ui.league;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.renderapps.balizinha.R;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.ViewHolder> {

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tag) TextView tag;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private LeagueActivity activity;
    private ArrayList<String> tags;
    public TagsAdapter(LeagueActivity activity, ArrayList<String> tags){
        this.activity = activity;
        this.tags = tags;
    }

    @NonNull
    @Override
    public TagsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tag, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TagsAdapter.ViewHolder holder, int position) {
        final String tag = tags.get(position);
        holder.tag.setText(tag);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!activity.isDestroyed() && !activity.isFinishing()){
                    activity.showTagDialog();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }
}
