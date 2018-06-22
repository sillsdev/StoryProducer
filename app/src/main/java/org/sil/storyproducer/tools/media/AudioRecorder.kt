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

class AudioRecorder {

    private val mRecorder = MediaRecorder()
    var isRecording = false
    private set

    init {
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder.setOutputFormat(OUTPUT_FORMAT)
        mRecorder.setAudioEncoder(AUDIO_ENCODER)
        mRecorder.setAudioEncodingBitRate(BIT_RATE)
        mRecorder.setAudioSamplingRate(SAMPLE_RATE)
    }

    fun startNewRecording(activity: Activity, relPath: String,
                               storyName: String = Workspace.activeStory.title){
        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        mRecorder.setOutputFile(getStoryFileDescriptor(activity,relPath,storyName))
        try{
            mRecorder.prepare()
            mRecorder.start()
            isRecording = true
            Toast.makeText(activity, R.string.recording_toolbar_recording_voice, Toast.LENGTH_SHORT).show()
        }
        catch (e: IllegalStateException) {Log.e(AUDIO_RECORDER, "Could not start recording voice.", e)}
        catch (e: IOException) {Log.e(AUDIO_RECORDER, "Could not start recording voice.", e)}
    }

    fun stop(context: Context) {
        try {
            mRecorder.stop()
            mRecorder.reset()
            isRecording = false
            Toast.makeText(context, R.string.recording_toolbar_stop_recording_voice, Toast.LENGTH_SHORT).show()
        } catch (stopException: RuntimeException) {
            Toast.makeText(context, R.string.recording_toolbar_error_recording, Toast.LENGTH_SHORT).show()
        } catch (e: InterruptedException) {
            Log.e(AUDIO_RECORDER, "Voice recorder interrupted!", e)
        }
    }

    fun release(){mRecorder.release()}
}
