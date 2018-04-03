package io.renderapps.balizinha.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.renderapps.balizinha.activity.EventDetailsActivity;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.MainActivity;
import io.renderapps.balizinha.activity.SetupProfileActivity;
import io.renderapps.balizinha.fragment.MapFragment;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.Constants;
import io.renderapps.balizinha.util.GeneralHelpers;

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

    // firebase
    private DatabaseReference databaseRef;
    private DatabaseReference paymentRef;
    private FirebaseUser firebaseUser;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    // properties
    private MapFragment mapFragment;
    private Context mContext;
    private List<Event> events;
    private Calendar calendar;
    private SimpleDateFormat mFormatter = new SimpleDateFormat("EE, MMM dd @ h:mm aa", Locale.getDefault());

    public MapAdapter(Context context, MapFragment mapFragment, List<Event> events){
        this.mContext = context;
        this.mapFragment = mapFragment;
        this.events = events;
        this.databaseRef = FirebaseDatabase.getInstance().getReference();
        this.paymentRef = databaseRef.child("charges").child("events");
        this.firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        this.calendar = Calendar.getInstance();
        this.mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_map, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Event event = events.get(position);

        holder.title.setText(event.getName().concat(" ").concat("(").concat(event.getType())
                .concat(")"));
        if (event.getPlace() != null && !event.getPlace().isEmpty()){
            holder.address.setText(event.getPlace());
        } else {
            String address = event.getCity();
            if (event.getState() != null)
                address = address.concat(", ").concat(event.getState());
            holder.address.setText(address);
        }

        // Date
        formatTime(event.getStartTime());
        String time = mFormatter.format(calendar.getTime());
        holder.time.setText(time);

        holder.joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapFragment.isPaymentInProcess(event.getEid())){
                    Toast.makeText(mContext, "Still processing payment...",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mContext instanceof MainActivity){
                    Player player = ((MainActivity)mContext).getCurrentUser();
                    if (player != null && player.getName() != null && !player.getName().isEmpty()) {
                        if (event.paymentRequired)
                            hasUserAlreadyPaid(event);
                        else
                            onUserJoin(event);
                    } else {
                        GeneralHelpers.showAddNameDialog(mContext);
                    }
                }
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, EventDetailsActivity.class);
                intent.putExtra(EventDetailsActivity.EVENT_ID, event.getEid());
                intent.putExtra(EventDetailsActivity.EVENT_TITLE, event.getName());
                intent.putExtra(EventDetailsActivity.EVENT_PLAYER_STATUS, false);
                intent.putExtra(EventDetailsActivity.EVENT_STATUS, false);
                intent.putExtra(EventDetailsActivity.EVENT_LAUNCH_MODE, false);
                mContext.startActivity(intent);
                ((MainActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
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
                    if (count >= event.getMaxPlayers()) {
                        holder.availability.setText(R.string.unavailable);
                        holder.joinButton.setVisibility(View.GONE);
                    }
                    else {
                        holder.availability.setText(R.string.available);
                        holder.joinButton.setVisibility(View.VISIBLE);
                    }
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

    // has user paid for game already, left and is now joining again
    private void hasUserAlreadyPaid(final Event event){
        paymentRef.child(event.getEid()).keepSynced(true);
        paymentRef.child(event.getEid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean userDidPay = false;
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()){
                    for (DataSnapshot child: dataSnapshot.getChildren()){
                        if (child.child("player_id").getValue(String.class).equals(firebaseUser.getUid())){
                            final String status = child.child("status").getValue(String.class);
                            if (status != null && status.equals("succeeded")){
                                userDidPay = true;
                                paymentRef.child(event.getEid()).keepSynced(false);
                                onUserJoin(event);
                                break;
                            }
                        }
                    }
                }
                if (!userDidPay)
                    isPaymentConfigEnabled(event);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void isPaymentConfigEnabled(final Event event){
        final int cacheExpiration = Constants.CACHE_EXPIRATION;
        mFirebaseRemoteConfig.fetch(cacheExpiration).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // After config data is successfully fetched, it must be activated before newly fetched
                    // values are returned.
                    mFirebaseRemoteConfig.activateFetched();
                }
                boolean paymentRequired = mFirebaseRemoteConfig.getBoolean(Constants.PAYMENT_CONFIG_KEY);
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
        if (firebaseUser != null) {
            FirebaseDatabase.getInstance().getReference().child("stripe_customers")
                    .child(firebaseUser.getUid()).child("source")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getValue() != null)
                                confirmPaymentDialog(event);
                            else
                                GeneralHelpers.showPaymentRequiredDialog(mContext);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
        }
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
                        GeneralHelpers.showSuccessfulJoin(mContext);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                    }
                });

        builder.create().show();
    }

    private void onUserJoin(Event event){
        databaseRef.child("userEvents").child(firebaseUser.getUid())
                .child(event.getEid()).setValue(true);
        databaseRef.child("eventUsers").child(event.getEid())
                .child(firebaseUser.getUid()).setValue(true);

        GeneralHelpers.showSuccessfulJoin(mContext);
    }

    private void onAddCharge(final Event event){
        if (mapFragment.isAdded())
            mapFragment.addProcessingPayment(event.getEid());
        Toast.makeText(mContext, mContext.getString(R.string.processing_payment), Toast.LENGTH_SHORT).show();
        if (firebaseUser != null) {
            Map<String, Object> chargeChild = new HashMap<>();
            final double price = event.getAmount() * 100;
            final int chargeAmount = (int) price;

            chargeChild.put("amount", chargeAmount);
            chargeChild.put("player_id", firebaseUser.getUid());

            // update
            final String chargeKey = paymentRef.child(event.getEid()).push().getKey();
            createPaymentListener(event, chargeKey);
            paymentRef.child(event.getEid()).child(chargeKey).updateChildren(chargeChild);
        }
    }

    // listen for successful payments
    private void createPaymentListener(final Event event, String chargeKey){
        paymentRef.child(event.getEid()).child(chargeKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    final String status = dataSnapshot.child("status").getValue(String.class);
                    if (status != null){
                        if (status.equals("succeeded")){
                            onUserJoin(event);
                        } else
                            showFailedPayment();

                        // remove processing payment
                        if (mapFragment.isAdded())
                            mapFragment.removeProcessingPayment(event.getEid());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showFailedPayment();
            }
        });
    }

    private void showFailedPayment(){
        Toast.makeText(mContext, mContext.getString(R.string.payment_failed), Toast.LENGTH_LONG).show();
    }
}
