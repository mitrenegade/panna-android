package io.renderapps.balizinha.util;

import android.text.TextUtils;
import com.google.firebase.database.DatabaseReference;

import static io.renderapps.balizinha.util.Constants.REF_USER_EVENTS;

/**
 * General purpose helpers
 */

public class CommonUtils {

    public static void syncEndpoints(DatabaseReference ref, String uid){
        ref.child(Constants.REF_PLAYERS).child(uid).keepSynced(true);
        ref.child(REF_USER_EVENTS).child(uid).keepSynced(true);
        ref.child("stripe_customers").child(uid).keepSynced(true);
    }

    public static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
