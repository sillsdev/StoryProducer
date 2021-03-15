package org.sil.storyproducer.tools.media.film

import android.util.Log
import com.arthenica.mobileffmpeg.Config.*
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FFmpegReturn(commandStr : String) {

    private val lastCommandOutput : String
    private val returnCode : Int
    private val isSuccess : Boolean
        get() {
            return returnCode == RETURN_CODE_SUCCESS
        }

    init {
        returnCode = FFmpeg.execute(commandStr)
        lastCommandOutput = getLastCommandOutput()

        if (returnCode == RETURN_CODE_SUCCESS) {
            Log.i(TAG, "Command execution completed successfully.")
        } else {
            var logErr = String.format("Command execution failed returnCode: %d command: %s\n\nLast Output:\n%s", returnCode, commandStr, lastCommandOutput)
            Log.e(TAG, logErr)
            FirebaseCrashlytics.getInstance().recordException(FFmpegException(logErr))
        }
    }

}

class FFmpegException(message: String) : Exception(message)