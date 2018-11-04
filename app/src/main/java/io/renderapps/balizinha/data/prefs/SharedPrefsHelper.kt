package io.renderapps.balizinha.data.prefs

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

class SharedPrefsHelper(context: Context) {
    companion object {
        private const val PANNA_PREFS = "PANNA_PREFS"
        private const val PREF_KEY_REQUESTED_LOCATION = "PREF_KEY_LOCATION"
        private const val PREF_KEY_SHOW_UPDATES= "PREF_KEY_SHOW_UPDATES"
        private const val PREF_KEY_ELAPSED_TIME = "PREF_KEY_ELAPSED_TIME"
    }

    private val mSharedPreferences: SharedPreferences

    var locationRequested: Boolean
        get() = mSharedPreferences.getBoolean(PREF_KEY_REQUESTED_LOCATION, false)
        set(requested) =
            mSharedPreferences.edit().putBoolean(PREF_KEY_REQUESTED_LOCATION, requested).apply()

    var showPlaystoreUpdate: Boolean
        get() = mSharedPreferences.getBoolean(PREF_KEY_SHOW_UPDATES, true)
        set(show) =
            mSharedPreferences.edit().putBoolean(PREF_KEY_SHOW_UPDATES, show).apply()

    var updateElapsedTime: Long
        get() = mSharedPreferences.getLong(PREF_KEY_ELAPSED_TIME, 0)
        set(time) =
            mSharedPreferences.edit().putLong(PREF_KEY_ELAPSED_TIME, time).apply()


    init {
        mSharedPreferences = context.getSharedPreferences(PANNA_PREFS, MODE_PRIVATE)
    }

    fun clear() {
        mSharedPreferences.edit().clear().apply()
    }
}