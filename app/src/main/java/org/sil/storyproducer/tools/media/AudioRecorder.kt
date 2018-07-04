package org.sil.storyproducer.tools.media

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.getStoryFileDescriptor
import java.io.IOException


//See https://developer.android.com/guide/topics/media/media-formats.html for supported formats.
internal val OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4
internal val AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC
internal val SAMPLE_RATE = 44100
internal val BIT_DEPTH = 16
//Set bit rate to exact spec of Android doc or to SAMPLE_RATE * BIT_DEPTH.
internal val BIT_RATE = SAMPLE_RATE * BIT_DEPTH

/**
 * Thin wrapper for [MediaRecorder] which provides some default behavior for recorder.
 */

private const val AUDIO_RECORDER = "audio_recorder"

abstract class AudioRecorder(val activity: Activity) {
    var isRecording = false
        protected set

    init {
        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    abstract fun startNewRecording(relPath: String)

    abstract fun stop()

}


class AudioRecorderMP4(activity: Activity) : AudioRecorder(activity) {

    private var mRecorder = MediaRecorder()

    private fun initRecorder(){
        mRecorder.release()
        mRecorder = MediaRecorder()
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder.setOutputFormat(OUTPUT_FORMAT)
        mRecorder.setAudioEncoder(AUDIO_ENCODER)
        mRecorder.setAudioEncodingBitRate(BIT_RATE)
        mRecorder.setAudioSamplingRate(SAMPLE_RATE)
    }

    override fun startNewRecording(relPath: String){
        initRecorder()
        mRecorder.setOutputFile(getStoryFileDescriptor(activity,relPath))
        isRecording = true
        try{
            mRecorder.prepare()
            mRecorder.start()
            Toast.makeText(activity, R.string.recording_toolbar_recording_voice, Toast.LENGTH_SHORT).show()
        }
        catch (e: IllegalStateException) {
            Log.e(AUDIO_RECORDER, "Could not start recording voice.", e)
            Toast.makeText(activity, "IllegalStateException!", Toast.LENGTH_SHORT).show()
        }
        catch (e: IOException) {
            Log.e(AUDIO_RECORDER, "Could not start recording voice.", e)
            Toast.makeText(activity, "IOException!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun stop() {
        try {
            mRecorder.stop()
            mRecorder.reset()
            mRecorder.release()
            isRecording = false
            Toast.makeText(activity, R.string.recording_toolbar_stop_recording_voice, Toast.LENGTH_SHORT).show()
        } catch (stopException: RuntimeException) {
            Toast.makeText(activity, R.string.recording_toolbar_error_recording, Toast.LENGTH_SHORT).show()
        } catch (e: InterruptedException) {
            Log.e(AUDIO_RECORDER, "Voice recorder interrupted!", e)
        }
    }
}
