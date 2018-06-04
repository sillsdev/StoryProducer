package org.sil.storyproducer.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat

import org.sil.storyproducer.R

class SplashScreenActivity : AppCompatActivity() {
    val WORKSPACE_CHOOSE : Int = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler().postDelayed(Runnable {
            // Checks registration file to see if email has been sent and launches registration if it hasn't
            val prefs = getSharedPreferences(getString(R.string.registration_filename), Context.MODE_PRIVATE)
            val preferences = prefs.all
            val registrationComplete = preferences[RegistrationActivity.EMAIL_SENT]
            if (registrationComplete != true) {
                val intent = Intent(this@SplashScreenActivity, RegistrationActivity::class.java)
                intent.putExtra(RegistrationActivity.FIRST_ACTIVITY_KEY, true)
                startActivity(intent)
                return@Runnable
            }

            val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, TIME_OUT.toLong())
        var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        ActivityCompat.startActivityForResult(this, intent, WORKSPACE_CHOOSE,null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == WORKSPACE_CHOOSE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            var uri: Uri? = null
            if (resultData != null) {
                uri = resultData.data
            }
        }
    }

    companion object {
        //Time in ms for splash screen to be shown
        private val TIME_OUT = 500
    }
}
