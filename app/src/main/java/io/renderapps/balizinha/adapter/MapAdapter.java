package io.renderapps.balizinha.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.renderapps.balizinha.activity.ChatActivity;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.MainActivity;
import io.renderapps.balizinha.activity.SetupProfileActivity;
import io.renderapps.balizinha.model.Action;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.model.Player;

/**
 * Created by joel on
 * 12/14/17.
 */

public class MapAdapter extends RecyclerView.Adapter<MapAdapter.ViewHolder> {

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView time;
        TextView address;
        TextView player_count;
        TextView availability;
        Button joinButton;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            time = itemView.findViewById(R.id.time);
            address = itemView.findViewById(R.id.address);
            player_count = itemView.findViewById(R.id.player_count);
            availability = itemView.findViewById(R.id.availability);
            joinButton = itemView.findViewById(R.id.join_button);
        }
    }

    // properties
    private Context mContext;
    private DatabaseReference databaseRef;
    private FirebaseUser firebaseUser;
    private List<Event> events;
    private Calendar calendar;
    private SimpleDateFormat mFormatter = new SimpleDateFormat("EE, MMM dd @ h:mm aa", Locale.getDefault());

    public MapAdapter(Context context, List<Event> events){
        this.mContext = context;
        this.events = events;
        this.databaseRef = FirebaseDatabase.getInstance().getReference();
        this.firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        this.calendar = Calendar.getInstance();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_map, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Event event = events.get(position);

        if (event.getAmount() == event.getMaxPlayers()) {
            holder.availability.setText(R.string.unavailable);
            holder.joinButton.setVisibility(View.GONE);
        }
        else {
            holder.availability.setText(R.string.available);
            holder.joinButton.setVisibility(View.VISIBLE);
        }

        holder.title.setText(event.getName().concat(" ").concat("(").concat(event.getType())
                .concat(")"));
        String address = event.getCity();
        if (event.getState() != null)
            address = address.concat(", ").concat(event.getState());
        holder.address.setText(address);

        // Date
        formatTime(event.getStartTime());
        String time = mFormatter.format(calendar.getTime());
        holder.time.setText(time);

        holder.joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Player player = null;
                Activity act = ((AppCompatActivity)mContext);
                if (act instanceof MainActivity)
                    player = ((MainActivity) act).getCurrentUser();

                if (player != null && player.getName() != null && !player.getName().isEmpty()) {
                    if (event.paymentRequired)
                        showPaymentDialog(event);
                    else {
                        databaseRef.child("userEvents").child(firebaseUser.getUid())
                                .child(event.getEid()).setValue(true);
                        databaseRef.child("eventUsers").child(event.getEid())
                                .child(firebaseUser.getUid()).setValue(true);

                        // create and add event action
                        Action action = new Action(event.getEid(), "", event.createdAt,
                                Action.ACTION_JOIN, firebaseUser.getUid());
                        if (firebaseUser.getDisplayName() != null)
                            action.setUsername(firebaseUser.getDisplayName());
                        String actionKey = databaseRef.child("action").push().getKey();
                        databaseRef.child("action").child(actionKey).setValue(action);

                        showSuccess();
                    }
                } else {
                    showAddNameDialog();
                }
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, ChatActivity.class);
                intent.putExtra(ChatActivity.EVENT_ID, event.getEid());
                intent.putExtra(ChatActivity.EVENT_TITLE, event.getName());
                intent.putExtra(ChatActivity.EVENT_TYPE, event.getType());
                intent.putExtra(ChatActivity.EVENT_INFO, event.getInfo());
                intent.putExtra(ChatActivity.EVENT_CITY, event.getCity());
                intent.putExtra(ChatActivity.EVENT_STATE, event.getState());
                intent.putExtra(ChatActivity.EVENT_CREATOR, event.getOwner());
                intent.putExtra(ChatActivity.EVENT_CREATED_AT, event.getCreatedAt());
                intent.putExtra(ChatActivity.EVENT_TIME, event.getStartTime());
                intent.putExtra(ChatActivity.EVENT_PAYMENT, event.paymentRequired);
                intent.putExtra(ChatActivity.EVENT_PLAYER_STATUS, false);
                intent.putExtra(ChatActivity.EVENT_STATUS, false);
                intent.putExtra(ChatActivity.EVENT_LAUNCH_MODE, false);

                mContext.startActivity(intent);
            }
        });

        databaseRef.child("eventUsers").child(event.getEid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int count = 0;
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    if (dataSnapshot.hasChildren()){
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            if (data.getValue(Boolean.class))
                                count++;
                        }
                    }
                    holder.player_count.setText(String.valueOf(count));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    private void formatTime(double sec){
        calendar.clear();
        calendar.setTimeInMillis((long)sec * 1000);
        int unroundedMinutes = calendar.get(Calendar.MINUTE);
        int mod = unroundedMinutes % 15;
        calendar.add(Calendar.MINUTE, mod < 8 ? -mod : (15-mod));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void showAddNameDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.add_name_title));
        builder.setMessage(mContext.getString(R.string.add_name));
        builder.setCancelable(false);

        builder.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent profileIntent = new Intent(mContext, SetupProfileActivity.class);
                        profileIntent.putExtra(SetupProfileActivity.EXTRA_PROFILE_UPDATE, true);
                        mContext.startActivity(new Intent(mContext, SetupProfileActivity.class));
                        ((MainActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                    }
                });

        builder.setNegativeButton(
                "Not now",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder.create().show();
    }

    private void showPaymentDialog(final Event event){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.payment_required_title));
        builder.setCancelable(false);

        // Get the layout inflater
        LayoutInflater inflater = ((MainActivity)mContext).getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_layout_payment, null))
                // Add action buttons
                .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        databaseRef.child("userEvents").child(firebaseUser.getUid())
                                .child(event.getEid()).setValue(true);
                        databaseRef.child("eventUsers").child(event.getEid())
                                .child(firebaseUser.getUid()).setValue(true);
                        showSuccess();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                    }
                });

        builder.create().show();
    }

    private void showSuccess(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.success_join_title));
        builder.setMessage(mContext.getString(R.string.success_join));
        builder.setCancelable(false);
        builder.setNegativeButton(
                "Close",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder.create().show();
    }
}
