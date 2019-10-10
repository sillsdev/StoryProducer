package org.sil.storyproducer.controller

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        try {
            val title: TextView = findViewById(R.id.version)
            title.text = packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        Workspace.initializeWorskpace(this)

        Handler().postDelayed(Runnable {
            //do we have a workspace?
            if (!Workspace.workspace.isDirectory) {
                val intent = Intent(this@SplashScreenActivity, WorkspaceDialogUpdateActivity::class.java)
                startActivity(intent)
                return@Runnable
            }
            // Checks registration file to see if email has been sent and launches registration if it hasn't
            if (!Workspace.registration.complete) {
                val intent = Intent(this@SplashScreenActivity, RegistrationActivity::class.java)
                startActivity(intent)
                return@Runnable
            }

            val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, TIME_OUT.toLong())
    }

    companion object {
        //Time in ms for splash screen to be shown
        private val TIME_OUT = 300
    }
}
