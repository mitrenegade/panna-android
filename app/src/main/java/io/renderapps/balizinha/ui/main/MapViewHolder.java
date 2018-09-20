package io.renderapps.balizinha.ui.main;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.service.PaymentService;
import io.renderapps.balizinha.service.PlayerService;
import io.renderapps.balizinha.service.StorageService;
import io.renderapps.balizinha.ui.event.EventDetailsActivity;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.util.DialogHelper;
import io.renderapps.balizinha.util.PhotoHelper;

import static io.renderapps.balizinha.util.CommonUtils.isValidContext;

/**
 * Created by joel on 7/23/18.
 */

public class MapViewHolder extends RecyclerView.ViewHolder {

    private Context mContext;
    private SimpleDateFormat mFormatter = new SimpleDateFormat("EE, MMM dd @ h:mm aa", Locale.getDefault());
    private Calendar mCalendar = Calendar.getInstance();
    private FirebaseUser currentUser;

    @BindView(R.id.image) ImageView image;
    @BindView(R.id.join_button) Button joinButton;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.time) TextView time;
    @BindView(R.id.address) TextView location;
    @BindView(R.id.player_count) TextView playerCount;
    @BindView(R.id.availability) TextView availability;

    MapViewHolder(View itemView, Context context) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        mContext = context;
    }

    public void bind(final Event event) {
        loadImage(event.getEid(), event.league);
        setTitle(event.getName(), event.type);
        setTime(event.getStartTime());
        setLocation(event.getPlace(), event.getCity(), event.getState());


        joinButton.setEnabled(false);
        setJoinButton(event);
        setPlayersCount(event.getEid(), event.getMaxPlayers());

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, EventDetailsActivity.class);
                intent.putExtra(EventDetailsActivity.EVENT_ID, event.getEid());
                intent.putExtra(EventDetailsActivity.EVENT_TITLE, event.getName());
                intent.putExtra(EventDetailsActivity.EVENT_STATUS, false);
                intent.putExtra(EventDetailsActivity.EVENT_LAUNCH_MODE, false);

                if (mContext instanceof MainActivity){
                    if (isValidContext((MainActivity)mContext)){
                        mContext.startActivity(intent);
                        ((MainActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                    }
                }
            }
        });
    }

    private void setTitle(String title, String type) {
        this.title.setText(title);
        if (type != null && !type.isEmpty())
            this.title.setText(title.concat(" ").concat("(").concat(type)
                    .concat(")"));
    }

    private void loadImage(String eventId, final String leagueId){
        if (eventId == null || eventId.isEmpty()) return;

        PhotoHelper.clearImage(mContext, image, R.drawable.ic_loading_image);
        StorageService.Companion.getEventImage(eventId, new StorageService.StorageCallback() {
            @Override
            public void onSuccess(@org.jetbrains.annotations.Nullable Uri uri) {
                if (uri != null){
                    PhotoHelper.glideImage(mContext, image, uri.toString(), R.drawable.ic_loading_image);
                } else if (leagueId != null && !leagueId.isEmpty()) {
                    // load league logo
                    StorageService.Companion.getLeagueHeader(leagueId, new StorageService.StorageCallback() {
                        @Override
                        public void onSuccess(@org.jetbrains.annotations.Nullable Uri leagueUri) {
                            if (leagueUri != null){
                                PhotoHelper.glideImage(mContext, image, leagueUri.toString(), R.drawable.ic_loading_image);
                                } else {
                                    PhotoHelper.clearImage(mContext, image, R.drawable.ic_soccer);
                                }
                            }
                        });
                    } else {
                    PhotoHelper.clearImage(mContext, image, R.drawable.ic_soccer);
                }
            }
        });
    }

    private void setTime(long startTime) {
        mCalendar.clear();
        mCalendar.setTimeInMillis(startTime * 1000);
        int unroundedMinutes = mCalendar.get(Calendar.MINUTE);
        int mod = unroundedMinutes % 15;
        mCalendar.add(Calendar.MINUTE, mod < 8 ? -mod : (15-mod));
        mCalendar.set(Calendar.SECOND, 0);
        mCalendar.set(Calendar.MILLISECOND, 0);

        String time = mFormatter.format(mCalendar.getTime());
        this.time.setText(time);
    }

    private void setLocation(String place, String city, String state){
        if (place != null && !place.isEmpty()){
            this.location.setText(place);
        } else {
            String location = "";
            if (city != null)
                location = city;
            if (state != null)
                location = location.concat(", ").concat(state);

            this.location.setText(location);
        }
    }

    private void setPlayersCount(String eid, final int maxPlayers){
        FirebaseDatabase.getInstance().getReference().child("eventUsers").child(eid).addValueEventListener(new ValueEventListener() {
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
                    if (count >= maxPlayers) {
                        availability.setText(R.string.unavailable);
                        joinButton.setVisibility(View.GONE);
                    } else {
                        availability.setText(R.string.available);
                        joinButton.setVisibility(View.VISIBLE);
                        joinButton.setEnabled(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void setJoinButton(final Event event){
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) return;

                PlayerService.getPlayer(currentUser.getUid(), new PlayerService.PlayerCallback() {
                    @Override
                    public void onSuccess(@Nullable Player player) {
                        if (player == null){
                            Toast.makeText(mContext.getApplicationContext(), "Please log in to continue.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (player.getName() == null || player.getName().isEmpty()){
                            DialogHelper.showAddNameDialog(((MainActivity) mContext));
                            return;
                        }

                        if (!playerCount.getText().toString().isEmpty()){
                            final int count = Integer.valueOf(playerCount.getText().toString());
                            if (count >= event.getMaxPlayers()){
                                Toast.makeText(mContext.getApplicationContext(), "Unable to join, event is full.", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }


                        if (event.paymentRequired){
                            PaymentService.Companion.hasUserAlreadyPaid(mContext, event, currentUser.getUid());
                        } else {
                            PaymentService.Companion.onUserJoin(mContext, event);
                        }
                    }
                });
            }
        });
    }
}
