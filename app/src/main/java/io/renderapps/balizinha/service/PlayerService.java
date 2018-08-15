package io.renderapps.balizinha.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import io.renderapps.balizinha.model.Player;

import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;

public class PlayerService {

    public interface PlayerCallback {
        void onSuccess(@Nullable Player player);
    }


    public static void getPlayer(final String uid, final PlayerCallback callback){
        FirebaseDatabase.getInstance().getReference().child(REF_PLAYERS).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    final Player player = dataSnapshot.getValue(Player.class);
                    if (player == null) {
                        callback.onSuccess(null);
                        return;
                    }

                    player.setUid(uid);
                    callback.onSuccess(player);
                    return;
                }

                callback.onSuccess(null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }
}
