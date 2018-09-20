package io.renderapps.balizinha.service

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.reactivex.Single
import io.renderapps.balizinha.util.Constants

class FirebaseService(){
    companion object {
        fun getRemoteConfig(): Single<FirebaseRemoteConfig> {
            return Single.create({ singleSubscriber ->
                val remoteConfig = FirebaseRemoteConfig.getInstance()
                remoteConfig.fetch(Constants.REMOTE_CACHE_EXPIRATION.toLong()).addOnCompleteListener{ task ->
                    if (task.isSuccessful)
                        remoteConfig.activateFetched()

                    if (remoteConfig == null)
                        singleSubscriber.onError(Exception("Error initializing Firebase Remote Config"))
                    else
                        singleSubscriber.onSuccess(remoteConfig)
                }
            })
        }
    }
}