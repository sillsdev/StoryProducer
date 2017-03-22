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

    public AudioRecorder(String filename, Activity activity) {
        super();

        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{ Manifest.permission.RECORD_AUDIO}, 1);
        }

        setAudioSource(MediaRecorder.AudioSource.MIC);
        setOutputFormat(OutputFormat.MPEG_4);
        setAudioEncoder(AudioEncoder.AAC);
        setAudioEncodingBitRate(16);
        setAudioSamplingRate(44100);
        setOutputFile(filename);
    }
}
