package org.sil.storyproducer.tools.media;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Thin wrapper for {@link MediaRecorder} which provides some default behavior for recorder.
 */
public class AudioRecorder extends MediaRecorder {
    //See https://developer.android.com/guide/topics/media/media-formats.html for supported formats.
    private static final int OUTPUT_FORMAT = OutputFormat.MPEG_4;
    private static final int AUDIO_ENCODER = AudioEncoder.AAC;
    private static final int SAMPLE_RATE = 44100;
    private static final int BIT_DEPTH = 16;
    //Set bit rate to exact spec of Android doc or to SAMPLE_RATE * BIT_DEPTH.
    private static final int BIT_RATE = SAMPLE_RATE * BIT_DEPTH;

    public AudioRecorder(String filename, Activity activity) {
        super();

        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{ Manifest.permission.RECORD_AUDIO}, 1);
        }

        setAudioSource(MediaRecorder.AudioSource.MIC);
        setOutputFormat(OUTPUT_FORMAT);
        setAudioEncoder(AUDIO_ENCODER);
        setAudioEncodingBitRate(BIT_RATE);
        setAudioSamplingRate(SAMPLE_RATE);
        setOutputFile(filename);
    }
}
