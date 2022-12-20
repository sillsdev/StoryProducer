package org.tyndalebt.storyproduceradv

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import timber.log.Timber
import dev.b3nedikt.restring.Restring.init
import dev.b3nedikt.reword.RewordInterceptor
import dev.b3nedikt.viewpump.ViewPump.init

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        context = this
        SPAdvApplication = this
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        init(this)
        init(RewordInterceptor)
        initTimber()
    }

    fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    companion object {
        var context: Context? = null
            private set
        var SPAdvApplication: App? = null
        operator fun get(activity: Activity): App {
            return activity.application as App
        }
    }
}