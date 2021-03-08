package org.sil.storyproducer.tools.media.film

import android.util.Log
import com.arthenica.mobileffmpeg.Config.*
import com.arthenica.mobileffmpeg.FFmpeg

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
            Log.e(TAG, String.format("Command execution failed returnCode: %d command: %s\n\nLast Output:\n%s", returnCode, commandStr, lastCommandOutput))
        }
    }

}