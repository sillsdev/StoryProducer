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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Designed to create WAV file audio recording.
 */
public class WavAudioRecorder {
    private static final String TAG = "WavAudioRecorder";
    private static final String PCM_EXTENSION = ".pcm";
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int SAMPLE_RATE = 44100;
    private static final int INPUT_SOURCE = MediaRecorder.AudioSource.MIC;
    private static int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private Object recordingLock = new Object();
    private boolean isPCMWritingDone;
    private boolean isRecording = false;
    private Runnable writePcmToFileRunnable;
    private File wavFile;
    private File pcmFile;

    private AudioRecord audioRecord;

    /**
     * C-tor.
     *
     * @param activity
     * @param filePathToRecordTo
     */
    public WavAudioRecorder(Activity activity, File filePathToRecordTo) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        this.wavFile = filePathToRecordTo;
        this.pcmFile = new File(filePathToRecordTo.getPath() + PCM_EXTENSION);

        writePcmToFileRunnable = new Runnable() {
            @Override
            public void run() {
                createPcmFile();
            }
        };

        audioRecord = new AudioRecord(INPUT_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
    }

    /**
     * Change the path that the WavAudioRecorder class saves to.
     *
     * @param newWavFile The file where the new audio will be saved to.
     */
    public void setNewPath(File newWavFile) {
        wavFile = newWavFile;
    }

    public void startRecording() {
        if (!isRecording) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                isPCMWritingDone = false;
                isRecording = true;
                audioRecord.startRecording();
                new Thread(writePcmToFileRunnable).start();
            } else if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                audioRecord = new AudioRecord(INPUT_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);

                isPCMWritingDone = false;
                isRecording = true;
                audioRecord.startRecording();
                new Thread(writePcmToFileRunnable).start();
            }
        }
    }

    public void stopRecording() {
        if (isRecording && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            isRecording = false;
            boolean tempIsDone = false;
            while(!tempIsDone) {
                synchronized (recordingLock) {
                    tempIsDone = isPCMWritingDone;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    tempIsDone = true;
                    Log.e(TAG, "Waiting for PCM completion interrupted.", e);
                }
            }
            createWavFile();
            audioRecord.release();
        }
    }

    /**
     * Checks if the WavAudioRecorder is currently recording.
     * @return true if recording is happening.
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * This function continuously polls the mic and saves to the file stream. Stops polling
     when the isRecording is set to false in the stopRecording() function.
     */
    private void createPcmFile() {
        DataOutputStream dataOutputStream = null;
        try {
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(pcmFile)));
            while (isRecording) {
                short[] audioData = new short[minBufferSize];
                int amountRead = audioRecord.read(audioData, 0, minBufferSize);
                for (int i = 0; i < amountRead; i++) {
                    dataOutputStream.writeShort(audioData[i]);
                }
            }
            dataOutputStream.close();
            audioRecord.stop();
            synchronized (recordingLock) {
                isPCMWritingDone = true;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not create WAV file.", e);
        } catch (IOException e) {
            Log.e(TAG, "Could not write to WAV file.", e);
        }
    }

    /**
     * This function does these things:
     * <ol>
     * <li>
     * Read in raw PCM audio file from phone into byte array.
     * </li>
     * <li>
     * Convert the byte array into a WAV file byte array.
     * </li>
     * <li>
     * Write the WAV byte array into a new file.
     * </li>
     * <li>
     * Delete the PCM audio file.
     * </li>
     * </ol>
     */
    private void createWavFile() {
        if (pcmFile.exists()) {
            try {
                //TODO handle large file size over 2^32
                byte[] bytes = new byte[(int) pcmFile.length()];

                //read raw audio file from phone
                FileInputStream fil = new FileInputStream(pcmFile);
                fil.read(bytes);

                byte[] wavBytes = WavFileCreator.createWavFileInBytes(bytes, new WavFileCreator.AudioAttributes(AUDIO_FORMAT, CHANNEL_CONFIG, SAMPLE_RATE));

                //write the WAV file to phone
                FileOutputStream fos = new FileOutputStream(wavFile);
                fos.write(wavBytes);
                fos.close();
                fil.close();

                //delete the raw audio data file (pcm file)
                pcmFile.delete();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Could not find PCM file", e);
            } catch (IOException e) {
                Log.e(TAG, "Could not read/write to/from file!", e);
            }
        }
    }
}
