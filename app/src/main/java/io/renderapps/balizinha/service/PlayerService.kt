package io.renderapps.balizinha.service

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.Single
import io.renderapps.balizinha.model.Player
import io.renderapps.balizinha.util.Constants.REF_PLAYERS

class PlayerService {
    companion object {
        fun getPlayer(uid: String): Single<Player> {
            return Single.create { player ->
                FirebaseDatabase.getInstance().reference.child(REF_PLAYERS).child(uid)
                        .addListenerForSingleValueEvent(object: ValueEventListener{
                            override fun onCancelled(p0: DatabaseError) {
                                player.onError(Exception("Error fetching player."))
                            }

                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists() && snapshot.value != null){
                                    val p = snapshot.getValue(Player::class.java)
                                    if (p != null){
                                        p.uid = uid
                                        player.onSuccess(p)
                                    } else {
                                        player.onError(Exception("Error fetching player."))
                                    }
                                } else {
                                    player.onError(Exception("Error fetching player."))
                                }
                            }
                        })

                }
            }
        }
    }