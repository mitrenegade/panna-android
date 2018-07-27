package io.renderapps.balizinha.util;

import android.text.TextUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;

/**
 * General purpose helpers
 */

public class GeneralHelpers {

    public static void syncEndpoints(DatabaseReference ref, String uid){
        ref.child(Constants.REF_PLAYERS).child(uid).keepSynced(true);
        ref.child("userEvents").child(uid).keepSynced(true);
        ref.child("stripe_customers").child(uid).keepSynced(true);
    }

    public static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static boolean isValidFirebaseName(FirebaseUser firebaseUser){
        boolean isValid = true;
        if (firebaseUser.getDisplayName() == null || firebaseUser.getDisplayName().isEmpty())
            isValid = false;

        for (UserInfo userInfo : firebaseUser.getProviderData()) {
            if (userInfo.getDisplayName() == null || userInfo.getDisplayName().isEmpty()) {
                isValid = false;
            }
        }
        return isValid;
    }


}
