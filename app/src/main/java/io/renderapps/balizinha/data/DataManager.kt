package io.renderapps.balizinha.data

import io.renderapps.balizinha.data.prefs.SharedPrefsHelper

class DataManager(private val mSharedPrefsHelper: SharedPrefsHelper) {

    var locationRequested: Boolean
        get() = mSharedPrefsHelper.locationRequested
        set(requested) {
            mSharedPrefsHelper.locationRequested = requested
        }

    var showPlaystoreUpdate: Boolean
        get() = mSharedPrefsHelper.showPlaystoreUpdate
        set(show) {
            mSharedPrefsHelper.showPlaystoreUpdate = show
        }

    var updateElapsedTime: Long
        get() = mSharedPrefsHelper.updateElapsedTime
        set(time) {
            mSharedPrefsHelper.updateElapsedTime = time
        }
}