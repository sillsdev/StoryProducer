package org.sil.storyproducer.tools;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Designed to create PCM or Wav file audio recording
 *
 */
public class WavAudioRecorder {
    private static final int OUTPUT_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    private static final int SAMPLE_RATE = 44100;
    private static final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, OUTPUT_FORMAT);

    private AudioRecord audioRecord;

    public WavAudioRecorder(Activity activity){
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{ Manifest.permission.RECORD_AUDIO}, 1);
        }

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_MASK, OUTPUT_FORMAT, );
    }


}
