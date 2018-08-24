package io.renderapps.balizinha.service.notification;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;

public class NotificationService {
    public static void storeFcmToken(){
        // Get token
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    return;
                }

                // New Instance ID token
                String token = task.getResult().getToken();

                final String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null && !uid.isEmpty()){
                    FirebaseDatabase.getInstance().getReference().child(REF_PLAYERS).child(uid)
                            .child("fcmToken").setValue(token);
                }
            }
        });
    }
}
