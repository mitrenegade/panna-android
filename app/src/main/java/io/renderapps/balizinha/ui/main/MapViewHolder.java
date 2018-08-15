package io.renderapps.balizinha.ui.main;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.service.CloudService;
import io.renderapps.balizinha.service.PlayerService;
import io.renderapps.balizinha.ui.event.EventDetailsActivity;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.util.Constants;
import io.renderapps.balizinha.util.DialogHelper;

import static io.renderapps.balizinha.util.Constants.REF_EVENT_USERS;
import static io.renderapps.balizinha.util.Constants.REF_USER_EVENTS;

/**
 * Created by joel on 7/23/18.
 */

public class MapViewHolder extends RecyclerView.ViewHolder {

    private Context mContext;
    private SimpleDateFormat mFormatter = new SimpleDateFormat("EE, MMM dd @ h:mm aa", Locale.getDefault());
    private Calendar mCalendar = Calendar.getInstance();

    private DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference paymentRef = databaseRef.child("charges").child("events");
    private FirebaseRemoteConfig mRemoteConfig  = FirebaseRemoteConfig.getInstance();
    private FirebaseUser currentUser;

    private TextView title;
    private TextView time;
    private TextView location;
    private TextView playerCount;
    private TextView availability;
    private Button joinButton;

    public MapViewHolder(View itemView, Context context) {
        super(itemView);
        mContext = context;

        title = itemView.findViewById(R.id.title);
        time = itemView.findViewById(R.id.time);
        location = itemView.findViewById(R.id.address);
        playerCount = itemView.findViewById(R.id.player_count);
        availability = itemView.findViewById(R.id.availability);
        joinButton = itemView.findViewById(R.id.join_button);
    }

    public void bind(final Event event) {
        setTitle(event.getName());
        setTime(event.getStartTime());
        setLocation(event.getPlace(), event.getCity(), event.getState());
        setPlayersCount(event.getEid(), event.getMaxPlayers());
        setJoinButton(event);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, EventDetailsActivity.class);
                intent.putExtra(EventDetailsActivity.EVENT_ID, event.getEid());
                intent.putExtra(EventDetailsActivity.EVENT_TITLE, event.getName());
                intent.putExtra(EventDetailsActivity.EVENT_PLAYER_STATUS, false);
                intent.putExtra(EventDetailsActivity.EVENT_STATUS, false);
                intent.putExtra(EventDetailsActivity.EVENT_LAUNCH_MODE, false);

                if (isValidContext()){
                    mContext.startActivity(intent);
                    ((MainActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                }
            }
        });
    }

    private void setTitle(String title) {
        this.title.setText(title);
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

                        if (event.paymentRequired)
                            hasUserAlreadyPaid(event, currentUser.getUid());
                        else
                            onUserJoin(event);

                    }
                });

//                if (!CommonUtils.isValidFirebaseName(currentUser)) {
//                    DialogHelper.showAddNameDialog(((MainActivity) mContext));
//                    return;
//                }

            }
        });
    }


    // has user paid for game already, left and is now joining again
    private void hasUserAlreadyPaid(final Event event, final String uid){
        paymentRef.child(event.getEid()).keepSynced(true);

        paymentRef.child(event.getEid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()){
                    for (DataSnapshot child: dataSnapshot.getChildren()){

                        final String pid = child.child("player_id").getValue(String.class);
                        final String status = child.child("status").getValue(String.class);

                        if (pid != null && status != null && pid.equals(uid) && status.equals("succeeded")){

                            Log.d("debugJoin", "user has already paid");

                            // user has already paid
                            onUserJoin(event);
                            return;
                        }
                    }
                }

                // user has not paid
                isPaymentConfigEnabled(event);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void isPaymentConfigEnabled(final Event event){
        final int cacheExpiration = Constants.CACHE_EXPIRATION;
        FirebaseRemoteConfig.getInstance().fetch(cacheExpiration).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                    mRemoteConfig.activateFetched();
                boolean paymentRequired = mRemoteConfig.getBoolean(Constants.PAYMENT_CONFIG_KEY);

                // check if user has added a payment method
                if (paymentRequired)
                    checkUserPaymentMethod(event);
                else {
                    showPaymentDialog(event);
                }
            }
        });
    }

    private void confirmPaymentDialog(final Event event){
        if (!isValidContext())
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.confirm_payment));
        builder.setCancelable(false);

        // Get the layout inflater
        LayoutInflater inflater = ((MainActivity)mContext).getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_layout_payment, null);
        ((TextView)view.findViewById(R.id.payment_details)).setText("Press Ok to pay $"
                .concat(String.format(Locale.getDefault(), "%.2f", event.getAmount())).concat(" for this game."));
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        onAddCharge(event);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder.create().show();
    }

    private void checkUserPaymentMethod(final Event event){
        FirebaseDatabase.getInstance().getReference().child("stripe_customers")
                .child(currentUser.getUid()).child("source")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.getValue() != null)
                            confirmPaymentDialog(event);
                        else
                            DialogHelper.showPaymentRequiredDialog((MainActivity)mContext);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
    }

    private void showPaymentDialog(final Event event){
        if (!isValidContext())
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.payment_required_title));
        builder.setCancelable(false);

        LayoutInflater inflater = ((MainActivity)mContext).getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_layout_payment, null))
                .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        onUserJoin(event);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder.create().show();
    }

    private void onUserJoin(final Event event){
        databaseRef.child(REF_EVENT_USERS).child(event.getEid()).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                int numOfPlayers = 0;
                for (MutableData child: mutableData.getChildren()){
                    if (child != null && child.getValue() != null){
                        if ((Boolean)child.getValue()){
                            numOfPlayers++;
                        }
                    }
                }

                if (numOfPlayers < event.getMaxPlayers()){
                    databaseRef.child(REF_USER_EVENTS).child(currentUser.getUid()).child(event.getEid()).setValue(true);
                    databaseRef.child(REF_EVENT_USERS).child(event.getEid()).child(currentUser.getUid()).setValue(true);
                    return Transaction.success(mutableData);
                } else {
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean successful, @Nullable DataSnapshot dataSnapshot) {
                if (databaseError != null || !successful){
                    Toast.makeText(mContext.getApplicationContext(), "Unable to join game. Max number of players reached. ", Toast.LENGTH_LONG).show();
                    return;
                }

                if (mContext != null){
                    if (mContext instanceof MainActivity){
                        if (!((MainActivity) mContext).isDestroyed() && !((MainActivity) mContext).isFinishing()){

                            if (mContext instanceof MainActivity)
                                ((MainActivity) mContext).hideProcessingPayment();

                            // successful
                            DialogHelper.showSuccessfulJoin((MainActivity)mContext);
                        }
                    }
                }
            }
        });
    }

    private void onAddCharge(final Event event){
        if (mContext instanceof MainActivity)
            ((MainActivity) mContext).showProcessingPayment();

        if (currentUser == null || event == null || event.getEid()  == null || event.getEid().isEmpty()){
            Toast.makeText(mContext, "Unable to make payment.", Toast.LENGTH_LONG).show();
            return;
        }


        new CloudService(new CloudService.ProgressListener() {
            @Override
            public void onStringResponse(String response) {
                if (response == null || response.isEmpty()){
                    showFailedPayment();
                    return;
                }

                try {
                    Log.d("holdPaymentRes", response);
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.has("error")){
                        showFailedPayment();
                        return;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    showFailedPayment();
                    return;
                }

                // successful
                onUserJoin(event);

            }
        }).holdPaymentForEvent(currentUser.getUid(), event.getEid());
    }


    private boolean isValidContext() {
        return mContext instanceof MainActivity && !((MainActivity) mContext).isDestroyed() && !((MainActivity) mContext).isFinishing();

    }
    private void showFailedPayment(){
        if (isValidContext())
            Toast.makeText(mContext, mContext.getString(R.string.payment_failed), Toast.LENGTH_LONG).show();

        if (mContext instanceof MainActivity)
            ((MainActivity) mContext).hideProcessingPayment();
    }
}
