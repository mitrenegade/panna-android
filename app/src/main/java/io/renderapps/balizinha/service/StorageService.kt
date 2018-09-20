package io.renderapps.balizinha.service

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import io.renderapps.balizinha.util.Constants.*

class StorageService {

    interface StorageCallback {
        fun onSuccess(uri: Uri?)
    }

    companion object {
        fun getEventImage(eventId: String?, callback: StorageCallback) {
            if (eventId == null) return

            FirebaseStorage.getInstance().reference.child(REF_STORAGE_IMAGES).child(REF_STORAGE_EVENT)
                    .child(eventId).downloadUrl.addOnSuccessListener { uri ->
                if (uri != null) {
                    callback.onSuccess(uri)
                } else {
                    callback.onSuccess(null)
                }
            }.addOnFailureListener {
                // Handle any errors
                callback.onSuccess(null)
            }
        }

        fun getLeagueHeader(leagueId: String?, callback: StorageCallback) {
            if (leagueId == null) return

            FirebaseStorage.getInstance().reference.child(REF_STORAGE_IMAGES).child(REF_STORAGE_LEAGUE)
                    .child(leagueId).downloadUrl.addOnSuccessListener { uri ->
                if (uri != null) {
                    callback.onSuccess(uri)
                } else {
                    callback.onSuccess(null)
                }
            }.addOnFailureListener {
                // Handle any errors
                callback.onSuccess(null)
            }
        }

        fun getPlayerImage(playerId: String?, callback: StorageCallback) {
            if (playerId == null) return

            FirebaseStorage.getInstance().reference.child(REF_STORAGE_IMAGES).child(REF_STORAGE_PLAYER)
                    .child(playerId).downloadUrl.addOnSuccessListener { uri ->
                if (uri != null) {
                    callback.onSuccess(uri)
                } else {
                    callback.onSuccess(null)
                }
            }.addOnFailureListener {
                // Handle any errors
                callback.onSuccess(null)
            }
        }
    }

}
