package org.sil.storyproducer.controller

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.sil.storyproducer.R
import org.sil.storyproducer.activity.BaseActivity
import org.sil.storyproducer.activity.WorkspaceDialogUpdateActivity
import org.sil.storyproducer.model.Workspace
import timber.log.Timber

class SplashScreenActivity : BaseActivity() {

    lateinit var controller: SplashScreenController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = SplashScreenController(this, this)
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

    fun initWorkspace() {
        Workspace.initializeWorskpace(this)

        if (Workspace.workdocfile.isDirectory) {
            controller.updateStories()
        } else {
            showSelectTemplatesFolder()
        }
    }



    private fun showSelectTemplatesFolder() {
        startActivity(Intent(this, WorkspaceDialogUpdateActivity::class.java))
        finish()
    }

}
