package io.renderapps.balizinha

import android.app.Application
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

import io.renderapps.balizinha.data.prefs.SharedPrefsHelper
import io.renderapps.balizinha.data.DataManager

class AppController : Application() {

    private lateinit var dataManager: DataManager

    override fun onCreate() {
        super.onCreate()

//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return
//        }
//
//        LeakCanary.install(this)


        val fabric = Fabric.Builder(this)
                .kits(Crashlytics())
                .debuggable(true)
                .build()
        Fabric.with(fabric)

        val sharedPrefsHelper = SharedPrefsHelper(applicationContext)
        dataManager = DataManager(sharedPrefsHelper)
    }

    fun getDataManager(): DataManager {
        return dataManager
    }
}