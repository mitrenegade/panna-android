package io.renderapps.balizinha.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;


public class FirebaseService extends IntentService {

    private static final String ACTION_SAVE_PAYMENT = "io.renderapps.balizinha.service.action.PAYMENT";
    private static final String ACTION_UPLOAD_PHOTO = "io.renderapps.balizinha.service.action.UPLOAD_PHOTO";
    private static final String ACTION_ADD_CHARGE = "io.renderapps.balizinha.service.action.ADD_CHARGE";

    private static final String EXTRA_UID = "io.renderapps.balizinha.service.extra.UID";
    private static final String EXTRA_SOURCE_ID = "io.renderapps.balizinha.service.extra.SOURCE_ID";
    private static final String EXTRA_LABEL = "io.renderapps.balizinha.service.extra.LABEL";
    private static final String EXTRA_LAST_FOUR = "io.renderapps.balizinha.service.extra.FOUR";

    // charges
    private static final String EXTRA_EID = "io.renderapps.balizinha.service.extra.EID";
    private static final String EXTRA_KEY = "io.renderapps.balizinha.service.extra.CHARGE_KEY";
    private static final String EXTRA_AMOUNT = "io.renderapps.balizinha.service.extra.AMOUNT";


    // photo
    private static final String EXTRA_BYTES = "io.renderapps.balizinha.service.extra.BYTES";


    public FirebaseService() {
        super("FirebaseService");
    }

    public static void startActionSavePayment(Context context, String uid, String sid,
                                              String label, String lastFour) {
        Intent intent = new Intent(context, FirebaseService.class);
        intent.setAction(ACTION_SAVE_PAYMENT);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_SOURCE_ID, sid);
        intent.putExtra(EXTRA_LABEL, label);
        intent.putExtra(EXTRA_LAST_FOUR, lastFour);
        context.startService(intent);
    }

    public static void startActionUploadPhoto(Context context, String uid, byte[] bytes){
        Intent intent = new Intent(context, FirebaseService.class);
        intent.setAction(ACTION_UPLOAD_PHOTO);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_BYTES, bytes);
        context.startService(intent);
    }

    public static void startActionAddCharge(Context context, String uid, String eid,
                                            String key, int amount){
        Intent intent = new Intent(context, FirebaseService.class);
        intent.setAction(ACTION_ADD_CHARGE);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_EID, eid);
        intent.putExtra(EXTRA_KEY, key);
        intent.putExtra(EXTRA_AMOUNT, amount);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SAVE_PAYMENT.equals(action)) {
                final String uid = intent.getStringExtra(EXTRA_UID);
                final String sourceId = intent.getStringExtra(EXTRA_SOURCE_ID);
                final String label = intent.getStringExtra(EXTRA_LABEL);
                final String four = intent.getStringExtra(EXTRA_LAST_FOUR);
                handleActionSavePayment(uid, sourceId, label, four);
            } else if (ACTION_UPLOAD_PHOTO.equals(action)){
                final String uid = intent.getStringExtra(EXTRA_UID);
                final byte[] bytes = intent.getByteArrayExtra(EXTRA_BYTES);
                handleActionUploadPhoto(uid, bytes);
            } else if (ACTION_ADD_CHARGE.equals(action)){
                final String uid = intent.getStringExtra(EXTRA_UID);
                final String eid = intent.getStringExtra(EXTRA_EID);
                final String key = intent.getStringExtra(EXTRA_KEY);
                final int amount = intent.getIntExtra(EXTRA_AMOUNT, 0);
                if (amount == 0)
                    return;
                handleActionAddCharge(uid, eid, key, amount);
            }
        }
    }

    private void handleActionSavePayment(String uid, String sourceId, String label, String lastFour) {
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("source", sourceId);
        childUpdates.put("last4", lastFour);
        childUpdates.put("label", label.concat(" ").concat(lastFour));
        FirebaseDatabase.getInstance().getReference().child("stripe_customers")
                .child(uid).updateChildren(childUpdates);
    }

    private void handleActionAddCharge(String uid, String eid, String key, int amount){
        Map<String, Object> chargeChild = new HashMap<>();
        chargeChild.put("amount", amount);
        chargeChild.put("player_id", uid);

        //            paymentRef.child(chargeKey).updateChildren(chargeChild);
        FirebaseDatabase.getInstance().getReference().child("charges").child("events")
                .child(eid).child(key).updateChildren(chargeChild);
    }

    public void handleActionUploadPhoto(final String uid, byte[] mBytes){
        final StorageReference photoRef = FirebaseStorage.getInstance().getReference()
                .child("images/player/" + uid);


        UploadTask uploadTask = photoRef.putBytes(mBytes);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                @SuppressWarnings("VisibleForTests") Uri downloadUrl = taskSnapshot.getDownloadUrl();
                updateUserDb(uid, downloadUrl);
            }
        });
    }

    void updateUserDb(String uid, Uri url){
        if (url != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child(REF_PLAYERS).child(uid).child("photoUrl")
                    .setValue(url.toString());
        }
    }
}
