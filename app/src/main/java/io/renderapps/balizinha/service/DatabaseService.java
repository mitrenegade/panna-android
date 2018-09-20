package io.renderapps.balizinha.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;


public class DatabaseService extends IntentService {

    private static final String ACTION_SAVE_PAYMENT = "io.renderapps.balizinha.service.action.PAYMENT";
    private static final String ACTION_UPLOAD_PHOTO = "io.renderapps.balizinha.service.action.UPLOAD_PHOTO";
    private static final String ACTION_ADD_CHARGE = "io.renderapps.balizinha.service.action.ADD_CHARGE";
    private static final String ACTION_UPLOAD_FB_PHOTO = "io.renderapps.balizinha.service.action.UPLOAD_FB";

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


    public DatabaseService() {
        super("DatabaseService");
    }

    public static void startActionSavePayment(Context context, String uid, String sid,
                                              String label, String lastFour) {
        Intent intent = new Intent(context, DatabaseService.class);
        intent.setAction(ACTION_SAVE_PAYMENT);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_SOURCE_ID, sid);
        intent.putExtra(EXTRA_LABEL, label);
        intent.putExtra(EXTRA_LAST_FOUR, lastFour);
        context.startService(intent);
    }

    public static void startActionUploadPhoto(Context context, String uid, byte[] bytes){
        Intent intent = new Intent(context, DatabaseService.class);
        intent.setAction(ACTION_UPLOAD_PHOTO);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_BYTES, bytes);
        context.startService(intent);
    }

    public static void startActionUploadFacebookPhoto(Context context, String uid){
        Intent intent = new Intent(context, DatabaseService.class);
        intent.setAction(ACTION_UPLOAD_FB_PHOTO);
        intent.putExtra(EXTRA_UID, uid);
        context.startService(intent);
    }

    public static void startActionAddCharge(Context context, String uid, String eid,
                                            String key, int amount){
        Intent intent = new Intent(context, DatabaseService.class);
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
            } else if (ACTION_UPLOAD_FB_PHOTO.equals(action)){
                final String uid = intent.getStringExtra(EXTRA_UID);
                handleActionUploadFacebookPhoto(uid);
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


    /******************************************************************************
     * Photo Service
     *****************************************************************************/

    public void handleActionUploadPhoto(final String uid, byte[] mBytes){
        final StorageReference photoRef = FirebaseStorage.getInstance().getReference()
                .child("images/player/" + uid);

        UploadTask uploadTask = photoRef.putBytes(mBytes);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
//                @SuppressWarnings("VisibleForTests")

                photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        if (uri.toString() == null) return;
                        updateUserDb(uid, uri);
                    }
                });
            }
        });
    }

    /*
     *  fetch facebook image, resize and upload to firebase
     */
    void handleActionUploadFacebookPhoto(final String uid){
        // not logged in through facebook
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken == null || accessToken.isExpired()){
            return;
        }

        Bundle parameters = new Bundle();
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        if (response != null) {
                            try {
                                JSONObject data = response.getJSONObject();
                                if (data.has("picture")) {
                                    String profilePicUrl = data.getJSONObject("picture")
                                            .getJSONObject("data").getString("url");
                                    glideImageAndUpload(uid, profilePicUrl);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        parameters.putString("fields", "picture.type(large)");
        request.setParameters(parameters);
        request.executeAndWait();
    }

    void glideImageAndUpload(final String uid, String url){
        RequestOptions options = new RequestOptions()
                .centerInside();

        FutureTarget<Bitmap> futureTarget =
                Glide.with(getApplicationContext())
                        .asBitmap()
                        .apply(options)
                        .load(url)
                        .submit(500, 500);

        try {
            Bitmap bitmap = futureTarget.get();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            final byte[] data = baos.toByteArray();

            final StorageReference photoRef = FirebaseStorage.getInstance()
                    .getReference()
                    .child("images")
                    .child("player")
                    .child(uid);

            UploadTask uploadTask = photoRef.putBytes(data);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            if (uri.toString() == null) return;
                            updateUserDb(uid, uri);
                        }
                    });
                }
            });
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    void updateUserDb(String uid, Uri url){
        FirebaseDatabase.getInstance().getReference()
                .child(REF_PLAYERS)
                .child(uid)
                .child("photoUrl")
                .setValue(url.toString());

        // update firebase user
        UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest.Builder();
        profileUpdatesBuilder.setPhotoUri(url);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            user.updateProfile(profileUpdatesBuilder.build());
    }
}
