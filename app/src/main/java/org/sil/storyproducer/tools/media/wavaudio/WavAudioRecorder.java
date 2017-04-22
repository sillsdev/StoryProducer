package org.sil.storyproducer.tools.media.wavaudio;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.file.AudioFiles;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ShortBuffer;

/**
 * Designed to create PCM (WAV) file audio recording
 */
public class WavAudioRecorder {
    private static final String TAG = "WavAudioRecorder";
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int SAMPLE_RATE = 48000;
    private static final int INPUT_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    private int slideNumber;

    private boolean isRecording = false;
    private Runnable readToFileRunnable;
    private String filePathToWrite;

    private AudioRecord audioRecord;

    public WavAudioRecorder(Activity activity, String filePathToRecordTo, int slideNumber) {
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
                    dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePathToWrite)));
                    while (isRecording) {
                        short[] audioData = new short[minBufferSize];
                        int amountRead = audioRecord.read(audioData, 0, minBufferSize);
                        for (int i = 0; i < amountRead; i++) {
                            dataOutputStream.writeShort(audioData[i]);
                        }
                    }
                    dataOutputStream.close();
                    audioRecord.stop();
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Could not create WAV file.");
                } catch (IOException e) {
                    Log.e(TAG, "Could not write to WAV file.");
                }
            }
        };
        this.slideNumber = slideNumber;
        audioRecord = new AudioRecord(INPUT_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
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
                new Thread(readToFileRunnable).start();
            } else if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                audioRecord = new AudioRecord(INPUT_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);

                isRecording = true;
                audioRecord.startRecording();
                new Thread(readToFileRunnable).start();
            }
        }
    }

    public void stopRecording() {
        if (isRecording && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            isRecording = false;

            File file = AudioFiles.getDraftPCM(StoryState.getStoryName(), slideNumber);

            if(file.exists()){
                try{
                    FileInputStream fil = new FileInputStream(file);
                    byte [] bytes = new byte[(int)file.length()];
                    fil.read(bytes);

                    bytes = WavHeader.createWavFile(bytes, new WavHeader.AudioAttributes(AUDIO_FORMAT, CHANNEL_CONFIG, SAMPLE_RATE));

                    FileOutputStream fos = new FileOutputStream(AudioFiles.getDraftWav(StoryState.getStoryName(), slideNumber));
                    fos.write(bytes);
                    fos.close();
                    fil.close();
                }catch(FileNotFoundException e){

                }catch(IOException e){

                }catch(Exception e){

                }
                audioRecord.release();
        }
    }}

    public boolean isRecording() {
        return isRecording;
    }

    private String removeWavFileExtension(String fileName) {
        String tempFileName = fileName.toLowerCase();
        if (tempFileName.contains(".wav") && tempFileName.indexOf(".wav") == tempFileName.length() - 4) {
            return fileName.substring(0, tempFileName.length() - 4);
        } else {
            return fileName;
        }
    }

}
