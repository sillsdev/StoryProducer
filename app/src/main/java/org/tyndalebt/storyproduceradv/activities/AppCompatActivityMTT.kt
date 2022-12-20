package org.tyndalebt.storyproduceradv.activities

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.ViewPumpAppCompatDelegate
import dev.b3nedikt.restring.Restring
import dev.b3nedikt.reword.Reword

open class AppCompatActivityMTT() : AppCompatActivity() {
    private var appCompatDelegate: AppCompatDelegate? = null

    open override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getDelegate(): AppCompatDelegate {
        if (appCompatDelegate == null) {
            appCompatDelegate = ViewPumpAppCompatDelegate(
                super.getDelegate(),
                this
            ) { base: Context -> Restring.wrapContext(base) }
        }
        return appCompatDelegate as AppCompatDelegate
    }

    open override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        updateView()
    }

    fun updateView(resID: Int) {
        //android.R.id.content was the default
        val rootView: View = window.decorView.findViewById(resID)
        Reword.reword(rootView)
    }

    fun updateView() {
        updateView(android.R.id.content)
    }
}