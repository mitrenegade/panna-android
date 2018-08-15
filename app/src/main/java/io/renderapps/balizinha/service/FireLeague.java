package io.renderapps.balizinha.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.ArrayList;

import static io.renderapps.balizinha.util.Constants.REF_LEAGUES;

public class FireLeague {

    public static void addTag(final String leagueId, final String tag){
        FirebaseDatabase.getInstance().getReference()
                .child(REF_LEAGUES)
                .child(leagueId)
                .child("tags")
                .runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                        ArrayList<String> tags = new ArrayList<>();

                        // add prev tags
                        if (mutableData.hasChildren()) {
                            for (MutableData child : mutableData.getChildren()) {
                                if (child != null)
                                    tags.add(child.getValue(String.class));
                            }
                        }

                        // add new tag
                        tags.add(tag);

                        // update league tags
                        mutableData.setValue(tags);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) { }
                });
    }
}
