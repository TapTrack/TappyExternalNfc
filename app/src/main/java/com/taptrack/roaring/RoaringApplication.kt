package com.taptrack.roaring

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.taptrack.experiments.rancheria.business.TappyNotificationManager
import io.reactivex.Observable
import timber.log.Timber

class RoaringApplication : Application() {
    lateinit private var prefs: SharedPreferences
    lateinit private var rxPrefs: RxSharedPreferences

    override fun onCreate() {
        super.onCreate()

        TappyNotificationManager.createNotificationChannelIfOreo(this)

        prefs = getSharedPreferences(PREFS_GLOBAL,Context.MODE_PRIVATE)
        rxPrefs = RxSharedPreferences.create(prefs)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    fun getAutolaunchEnabled() : Observable<Boolean> = rxPrefs.getBoolean(KEY_AUTOLAUNCH,false).asObservable()

    fun setAutolaunchEnabled(shouldLaunch: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOLAUNCH,shouldLaunch).apply()
    }

    companion object {
        val ACTION_TAG_FOUND = "com.taptrack.roaring.action.TAG_FOUND"
        val ACTION_NDEF_FOUND = "com.taptrack.roaring.action.NDEF_FOUND"
        val EXTRA_TAG_TYPE_INT = "com.taptrack.roaring.extra.TAG_TYPE"

        private val PREFS_GLOBAL = RoaringApplication::class.java.name+".PREFS_GLOBAL"
        private val KEY_AUTOLAUNCH = RoaringApplication::class.java.name+".KEY_AUTOLAUNCH"
    }
}

inline fun Context.getRoaringApplication() = this.applicationContext as RoaringApplication
