package io.renderapps.balizinha.service

import com.google.firebase.database.*
import io.reactivex.Single
import io.renderapps.balizinha.model.League
import io.renderapps.balizinha.util.Constants.REF_LEAGUES
import java.util.ArrayList

class FireLeague {

    companion object {

        fun getLeague(leagueId: String): Single<League> {
            return Single.create { leagueSingle ->
                FirebaseDatabase.getInstance().reference.child(REF_LEAGUES).child(leagueId)
                        .addListenerForSingleValueEvent(object: ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {
                                leagueSingle.onError(Exception("Unable to fetch league."))
                            }

                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists() && snapshot.value != null){
                                    val league = snapshot.getValue(League::class.java)
                                    if (league != null) {
                                        league.id = leagueId
                                        leagueSingle.onSuccess(league)
                                        return
                                    }
                                }
                                leagueSingle.onError(Exception("Unable to fetch league."))
                            }
                        })
            }
        }


        fun addTag(leagueId: String, tag: String) {
            FirebaseDatabase.getInstance().reference
                    .child(REF_LEAGUES)
                    .child(leagueId)
                    .child("tags")
                    .runTransaction(object : Transaction.Handler {
                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                            val tags = ArrayList<String>()

                            // add prev tags
                            if (mutableData.hasChildren()) {
                                for (child in mutableData.children) {
                                    if (child != null)
                                        tags.add(child.getValue(String::class.java)!!)
                                }
                            }

                            // add new tag
                            tags.add(tag)

                            // update league tags
                            mutableData.value = tags
                            return Transaction.success(mutableData)
                        }

                        override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot: DataSnapshot?) {}
                    })
        }
    }
}