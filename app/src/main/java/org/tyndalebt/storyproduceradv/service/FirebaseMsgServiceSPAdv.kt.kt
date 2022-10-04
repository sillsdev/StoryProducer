package org.tyndalebt.storyproduceradv.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.tyndalebt.storyproduceradv.activities.DisplayAlert
import org.tyndalebt.storyproduceradv.controller.remote.RemoteCheckFrag

class FirebaseMsgServiceSPAdv : FirebaseMessagingService() {

    val sTAG="SPAdv"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(sTAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(sTAG, "Message data payload: ${remoteMessage.data}")

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                //scheduleJob()
            } else {
                // Handle message within 10 seconds
                //handleNow()
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(sTAG, "Message Notification Body: ${it.body}")
            val mDisplayAlert = Intent(this, DisplayAlert::class.java)
            mDisplayAlert.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            mDisplayAlert.putExtra("title",  remoteMessage.notification?.title)
            mDisplayAlert.putExtra("body",  remoteMessage.notification?.body)
            startActivity(mDisplayAlert)
        }


        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(sTAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.

        // Store token in preferences with flag to upload when checked
        val prefs = getSharedPreferences(RemoteCheckFrag.R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        prefsEditor.putString("FirebaseToken", token)
        prefsEditor.putBoolean("FirebaseChanged", true)
        prefsEditor.apply()
    }
}
