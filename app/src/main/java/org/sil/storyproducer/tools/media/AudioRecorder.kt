package org.sil.storyproducer.tools.media

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.getStoryFileDescriptor

/**
 * Thin wrapper for [MediaRecorder] which provides some default behavior for recorder.
 */
class AudioRecorder(activity: Activity, relPath: String,
                    storyName: String = Workspace.activeStory.title) : MediaRecorder() {

    init {
        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(OUTPUT_FORMAT)
        setAudioEncoder(AUDIO_ENCODER)
        setAudioEncodingBitRate(BIT_RATE)
        setAudioSamplingRate(SAMPLE_RATE)
        setOutputFile(getStoryFileDescriptor(activity,relPath,storyName))
    }

    companion object {
        //See https://developer.android.com/guide/topics/media/media-formats.html for supported formats.
        private val OUTPUT_FORMAT = OutputFormat.MPEG_4
        private val AUDIO_ENCODER = AudioEncoder.AAC
        private val SAMPLE_RATE = 44100
        private val BIT_DEPTH = 16
        //Set bit rate to exact spec of Android doc or to SAMPLE_RATE * BIT_DEPTH.
        private val BIT_RATE = SAMPLE_RATE * BIT_DEPTH
    }
}
