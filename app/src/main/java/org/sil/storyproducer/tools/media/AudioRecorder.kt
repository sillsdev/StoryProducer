package org.sil.storyproducer.tools.media

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.widget.Toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import org.sil.storyproducer.tools.file.getStoryFileDescriptor
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.media.story.AutoStoryMaker
import org.sil.storyproducer.tools.media.story.StoryMaker
import org.sil.storyproducer.tools.media.story.StoryPage
import java.io.File
import java.io.IOException


//See https://developer.android.com/guide/topics/media/media-formats.html for supported formats.
internal val OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4
internal val AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC
internal val SAMPLE_RATE = 44100
internal val BIT_DEPTH = 16
internal val AUDIO_CHANNELS = 1
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

    companion object {
        /**
         * This class is used to concatenate two Wav files together.
         * <br></br>
         * Assumes the header of the Wav file resembles Microsoft's RIFF specification.<br></br>
         * A specification can be found [here](http://soundfile.sapp.org/doc/WaveFormat/).
         */

        fun concatenateAudioFiles(context: Context, orgAudioRelPath: String, appendAudioRelPath: String) {

            val tempDestPath  = "${context.filesDir}/temp.mp4"


            val outputFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            val audioFormat = AutoStoryMaker.generateAudioFormat()
            val pages: MutableList<StoryPage> = mutableListOf()

            var duration = MediaHelper.getAudioDuration(context, getStoryUri(orgAudioRelPath)!!)
            pages.add(StoryPage("",orgAudioRelPath,duration,null,null))
            duration = MediaHelper.getAudioDuration(context, getStoryUri(appendAudioRelPath)!!)
            pages.add(StoryPage("",appendAudioRelPath,duration,null,null))

            //If pages weren't generated, exit.
            val mStoryMaker = StoryMaker(context, File(tempDestPath), outputFormat, null, audioFormat,
                    pages.toTypedArray(), 10000, 10000)

            mStoryMaker.churn()
            mStoryMaker.close()

            copyToWorkspacePath(context, Uri.fromFile(File(tempDestPath)),
                    "${Workspace.activeDirRoot}/$orgAudioRelPath")
            File(tempDestPath).delete()
        }
    }
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
        mRecorder.setAudioChannels(AUDIO_CHANNELS)
    }

    override fun startNewRecording(relPath: String){
        initRecorder()
        mRecorder.setOutputFile(getStoryFileDescriptor(activity, relPath,"","w"))
        isRecording = true
        try{
            mRecorder.prepare()
            mRecorder.start()
        }
        catch (e: IllegalStateException) {
            Toast.makeText(activity, "IllegalStateException!", Toast.LENGTH_SHORT).show()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        catch (e: IOException) {
            Toast.makeText(activity, "IOException!", Toast.LENGTH_SHORT).show()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    override fun stop() {
        if(!isRecording) return
        try {
            mRecorder.stop()
            mRecorder.reset()
            mRecorder.release()
            isRecording = false
        } catch (stopException: RuntimeException) {
            Toast.makeText(activity, R.string.recording_toolbar_error_recording, Toast.LENGTH_SHORT).show()
            FirebaseCrashlytics.getInstance().recordException(stopException)
        } catch (e: InterruptedException) {
            Log.e(AUDIO_RECORDER, "Voice recorder interrupted!", e)
        }
    }
}

