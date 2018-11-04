package io.renderapps.balizinha.service

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.reactivex.Single
import io.renderapps.balizinha.util.Constants
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import io.renderapps.balizinha.BuildConfig


class FirebaseService(){
    companion object {
        fun getRemoteConfig(): Single<FirebaseRemoteConfig> {
            return Single.create { singleSubscriber ->
                val remoteConfig = FirebaseRemoteConfig.getInstance()
                var cache = Constants.REMOTE_CACHE_EXPIRATION.toLong()


                if (BuildConfig.DEBUG) {
                    val configSettings = FirebaseRemoteConfigSettings.Builder()
                            .setDeveloperModeEnabled(BuildConfig.DEBUG)
                            .build()
                    remoteConfig.setConfigSettings(configSettings)
                    cache = 0
                }

                remoteConfig.fetch(cache).addOnCompleteListener{ task ->
                    if (task.isSuccessful)
                        remoteConfig.activateFetched()

                    if (remoteConfig == null)
                        singleSubscriber.onError(Exception("Error initializing Remote Config"))
                    else
                        singleSubscriber.onSuccess(remoteConfig)
                }
            }
        }
    }
}