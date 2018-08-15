package io.renderapps.balizinha.ui.calendar;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.ui.event.EventDetailsActivity;
import io.renderapps.balizinha.ui.main.MainActivity;

public class CalendarVH extends RecyclerView.ViewHolder {
    private Context mContext;
    private boolean IS_UPCOMING;

    @BindView(R.id.payment_required) ImageView paymentView;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.time) TextView time;
    @BindView(R.id.address) TextView address;
    @BindView(R.id.player_count) TextView playerCount;
    @BindView(R.id.edit_delete_button) Button editButton;

    private SimpleDateFormat mFormatter = new SimpleDateFormat("EE, MMM dd @ h:mm aa", Locale.getDefault());

    CalendarVH(Context context, boolean isUpcoming, View view) {
        super(view);
        ButterKnife.bind(this, view);

        mContext = context;
        IS_UPCOMING = isUpcoming;
    }

    public void bind(final Event event){

        title.setText(event.getName().concat(" ").concat("(").concat(event.getType())
                .concat(")"));

        int showPaymentView = (event.paymentRequired) ? View.VISIBLE : View.GONE;
        paymentView.setVisibility(showPaymentView);

        setTime(event.getStartTime());
        setLocation(event.getPlace(), event.getCity(), event.getState());
        setLeaveEditButton(event.getOwner(), event);
        setPlayerCount(event.getEid());

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mContext != null && mContext instanceof MainActivity) {
                    if (((MainActivity) mContext).isDestroyed() || ((MainActivity) mContext).isFinishing())
                        return;

                    Intent intent = new Intent(mContext, EventDetailsActivity.class);
                    intent.putExtra(EventDetailsActivity.EVENT_ID, event.getEid());
                    intent.putExtra(EventDetailsActivity.EVENT_TITLE, event.getName());
                    intent.putExtra(EventDetailsActivity.EVENT_LAUNCH_MODE, true);

                    // user already joined game if accessing from Calendar
                    intent.putExtra(EventDetailsActivity.EVENT_PLAYER_STATUS, true);
                    if (!IS_UPCOMING)
                        intent.putExtra(EventDetailsActivity.EVENT_STATUS, true);
                    else
                        intent.putExtra(EventDetailsActivity.EVENT_STATUS, false);

                    // start activity
                    mContext.startActivity(intent);
                    ((MainActivity) mContext).overridePendingTransition(R.anim.anim_slide_in_right,
                            R.anim.anim_slide_out_left);
                }
            }
        });

    }

    private void setTime(long startTime){
        Date date = formatTime(startTime);
        String sTime = mFormatter.format(date);
        time.setText(sTime);
    }

    private void setLocation(String place, String city, String state){
        if (place != null && !place.isEmpty()){
            address.setText(place);
            return;
        }

        String location = city;
        if (state != null)
            location = location.concat(", ").concat(state);
        address.setText(location);
    }

    private void setLeaveEditButton(String organizer, final Event event){
        final String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        if (!IS_UPCOMING || organizer.equals(uid)) {
            editButton.setVisibility(View.GONE);
            return;
        }

        if (mContext != null) {
            editButton.setText(mContext.getString(R.string.leave));
            editButton.setTextColor(ContextCompat.getColor(mContext, android.R.color.white));
        }

        editButton.setVisibility(View.VISIBLE);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onUserLeave(event);
            }
        });
    }

    private void setPlayerCount(String eid){
        FirebaseDatabase.getInstance().getReference().child("eventUsers").child(eid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    if (dataSnapshot.hasChildren()){
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            if (data.getValue() != null && (Boolean) data.getValue())
                                count++;
                        }
                    }
                    playerCount.setText(String.valueOf(count));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void onUserLeave(final Event event) {
        if (mContext == null) return;
        if (mContext instanceof MainActivity) {
            if (((MainActivity) mContext).isDestroyed() || ((MainActivity) mContext).isFinishing())
                return;
        }

        final String uid = FirebaseAuth.getInstance().getUid();
        final DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        if (uid == null || uid.isEmpty()) return;

        // dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false);

        if (!event.paymentRequired){
            builder.setMessage(mContext.getString(R.string.leave_event));
        } else {
            builder.setTitle(mContext.getString(R.string.leave_event_title));
            builder.setMessage(mContext.getString(R.string.leave_paid_event));
        }

        builder.setPositiveButton(
                "Yes, I'm sure",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        databaseRef.child("userEvents").child(uid)
                                .child(event.getEid()).setValue(false);
                        databaseRef.child("eventUsers").child(event.getEid())
                                .child(uid).setValue(false);

                    }
                });

        builder.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    private Date formatTime(double sec){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis((long) sec * 1000);

        int unroundedMinutes = calendar.get(Calendar.MINUTE);
        int mod = unroundedMinutes % 15;

        calendar.add(Calendar.MINUTE, mod < 8 ? -mod : (15-mod));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
