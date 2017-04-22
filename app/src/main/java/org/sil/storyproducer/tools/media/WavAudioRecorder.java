package org.sil.storyproducer.tools.media;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Designed to create PCM (WAV) file audio recording
 */
public class WavAudioRecorder {
    private static final String TAG = "WavAudioRecorder";
    private static final String WAV_FILE_EXTENSION = ".wav";
    private static final int OUTPUT_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    private static final int SAMPLE_RATE = 44100;
    private static final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, OUTPUT_FORMAT);

    private boolean isRecording = false;
    private Runnable readToFileRunnable;
    private String filePathToWrite;

    private AudioRecord audioRecord;

    public WavAudioRecorder(Activity activity, String filePathToRecordTo) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        this.filePathToWrite = removeWavFileExtension(filePathToRecordTo);

        readToFileRunnable = new Runnable() {
            @Override
            public void run() {
                DataOutputStream dataOutputStream = null;
                try {
                    while (isRecording) {
                        dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePathToWrite + WAV_FILE_EXTENSION)));
                        short[] audioData = new short[minBufferSize];
                        int amountRead = audioRecord.read(audioData, 0, minBufferSize);
                        for (int i = 0; i < amountRead; i++) {
                            dataOutputStream.writeShort(audioData[i]);
                        }
                    }
                    if(dataOutputStream != null){
                        dataOutputStream.close();
                    }
                    audioRecord.stop();
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Could not create WAV file.");
                } catch (IOException e) {
                    Log.e(TAG, "Could not write to WAV file.");
                }
            }
        };

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_MASK, OUTPUT_FORMAT, minBufferSize);
    }

    public void startRecording(String filePathToWrite) {
        this.filePathToWrite = removeWavFileExtension(filePathToWrite);
        startRecording();
    }

    public void startRecording() {
        if (!isRecording) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                isRecording = true;
                audioRecord.startRecording();
                readToFileRunnable.run();
            } else if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_MASK, OUTPUT_FORMAT, minBufferSize);

                isRecording = true;
                audioRecord.startRecording();
                readToFileRunnable.run();
            }
        }
    }

    public void stopRecording() {
        if (isRecording && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            isRecording = false;
        }
    }

    public boolean isRecording(){
        return isRecording;
    }

    private String removeWavFileExtension(String fileName){
        String tempFileName = fileName.toLowerCase();
        if(tempFileName.contains(".wav") && tempFileName.indexOf(".wav") == tempFileName.length() - 4){
            return fileName.substring(0, tempFileName.length() - 4);
        }else{
            return fileName;
        }
    }

}
