package org.sil.storyproducer.controller

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.model.Workspace

class SplashScreenActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        try {
            val title: TextView = findViewById(R.id.version)
            title.text = packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        if (!Workspace.isInitialized) {
            initWorkspace()
        }
    }

}
