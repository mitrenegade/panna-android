package io.renderapps.balizinha.ui.event.chat;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Action;

import static io.renderapps.balizinha.model.Action.ACTION_CREATE;
import static io.renderapps.balizinha.model.Action.ACTION_HOLD;
import static io.renderapps.balizinha.model.Action.ACTION_JOIN;
import static io.renderapps.balizinha.model.Action.ACTION_LEAVE;
import static io.renderapps.balizinha.model.Action.ACTION_PAID;

public class ActionVH extends RecyclerView.ViewHolder {
    private TextView action;
    private SimpleDateFormat mFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());


    ActionVH(View itemView) {
        super(itemView);
        action = itemView.findViewById(R.id.action);
    }

    public void bind(String title, Action action) {
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        final Date date = new Date((long)action.getCreatedAt() * 1000);
        final boolean isCurrentUser = action.getUser().equals(firebaseUser.getUid());

        switch (action.getType()) {

            case ACTION_CREATE:
                if (isCurrentUser) {
                    this.action.setText("You created ".concat(title).concat(" on ").concat(mFormatter.format(date)));
                    break;
                }

                if (action.getUsername() != null) {
                    this.action.setText(action.getUsername().concat(" created ").concat(title)
                            .concat(" on ").concat(mFormatter.format(date)));
                } else {
                    this.action.setText(title.concat(" created on ").concat(mFormatter.format(date)));
                }

                break;

            case ACTION_JOIN:
                if (isCurrentUser) {
                    this.action.setText("You joined ".concat(title));
                    break;
                }

                if (action.getUsername() != null)
                    this.action.setText(action.getUsername().concat(" joined ").concat(title));
                else
                    this.action.setText("A new user has joined ".concat(title));

                break;

            case ACTION_PAID:
                if (isCurrentUser) {
                    this.action.setText("You paid for ".concat(title));
                    break;
                }

                if (action.getUsername() != null)
                    this.action.setText(action.getUsername().concat(" paid for ").concat(title));
                else
                    this.action.setText("A new user has paid for ".concat(title));
                break;

            case ACTION_LEAVE:
                if (isCurrentUser) {
                    this.action.setText("You left ".concat(title));
                    break;
                }

                if (action.getUsername() != null)
                    this.action.setText(action.getUsername().concat(" has left ").concat(title));
                else
                    this.action.setText("A user has left ".concat(title));

                break;

            case ACTION_HOLD:
                if (isCurrentUser) {
                    this.action.setText("You reserved a spot.");
                    break;
                }

                if (action.getUsername() != null)
                    this.action.setText(action.getUsername().concat(" reserved a spot."));
                else
                    this.action.setText("A user has reserved a spot.");
                break;
        }
    }
}