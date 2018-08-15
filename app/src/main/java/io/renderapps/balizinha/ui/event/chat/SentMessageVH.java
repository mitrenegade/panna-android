package io.renderapps.balizinha.ui.event.chat;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import io.renderapps.balizinha.R;

public class SentMessageVH extends RecyclerView.ViewHolder {
    private TextView message;

    public SentMessageVH(View itemView) {
        super(itemView);
        message = itemView.findViewById(R.id.message);
    }

    public void bind(String message) {
        this.message.setText(message);
    }
}