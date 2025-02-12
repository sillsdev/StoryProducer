package org.sil.storyproducer

import android.app.Application
import android.content.Context
import android.os.Build
import timber.log.Timber


class App : Application() {

    companion object {
        lateinit  var appContext: Context
        var languageCode: String = ""
        fun isRoboUnitTest(): Boolean {
            return "robolectric" == Build.FINGERPRINT
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        initTimber()
    }

    fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

}