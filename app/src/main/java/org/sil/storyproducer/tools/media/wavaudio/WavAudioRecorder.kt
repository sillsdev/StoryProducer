package org.sil.storyproducer.tools.media.wavaudio

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.media.AudioRecorder

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max

/**
 * Designed to create WAV file audio recording.
 */
class AudioRecorderWav (activity: Activity) : AudioRecorder(activity) {

    private var wavRelPath: String = ""
    private val pcmRelPath: String
    get() { return wavRelPath.slice(0 until max(0,wavRelPath.length-4)) + PCM_EXTENSION}
    private val writePcmToFileRunnable: Runnable = Runnable { createPcmFile() }
    private var isPCMWritingDone: Boolean = false

    private var audioRecord: AudioRecord? = null

    private val recordingLock = Any()


    override fun startNewRecording(relPath: String) {
        wavRelPath = relPath
        audioRecord = AudioRecord(INPUT_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize)
        if (!isRecording) {
            isPCMWritingDone = false
            isRecording = true
            audioRecord!!.startRecording()
            Thread(writePcmToFileRunnable).start()
        }
    }

    override fun stop() {
        if (isRecording && audioRecord!!.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            isRecording = false
            var tempIsDone = false
            while (!tempIsDone) {
                synchronized(recordingLock) {
                    tempIsDone = isPCMWritingDone
                }
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                    tempIsDone = true
                    Log.e(TAG, "Waiting for PCM completion interrupted.", e)
                }

            }
            audioRecord!!.release()
            createWavFile()
        }
    }

    /**
     * This function continuously polls the mic and saves to the file stream. Stops polling
     * when the isRecording is set to false in the stopRecording() function.
     */
    private fun createPcmFile() {
        try {
            val dataOutputStream = getStoryChildOutputStream(activity,pcmRelPath)
            val audioData = ByteArray(minBufferSize)
            while (isRecording) {
                val amountRead = audioRecord!!.read(audioData, 0, minBufferSize)
                dataOutputStream?.write(audioData)
            }
            dataOutputStream?.close()
            audioRecord!!.stop()
            synchronized(recordingLock) {
                isPCMWritingDone = true
            }
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Could not create WAV file.", e)
        } catch (e: IOException) {
            Log.e(TAG, "Could not write to WAV file.", e)
        }
    }

    /**
     * This function does these things:
     *
     *  1.
     * Read in raw PCM audio file from phone into byte array.
     *
     *  1.
     * Convert the byte array into a WAV file byte array.
     *
     *  1.
     * Write the WAV byte array into a new file.
     *
     *  1.
     * Delete the PCM audio file.
     *
     *
     */
    private fun createWavFile() {
        if (storyRelPathExists(activity,pcmRelPath)) {
            try {
                //TODO handle large file size over 2^32  (is this still relevant?)
                //read raw audio file from phone
                val iStream = getStoryChildInputStream(activity,pcmRelPath)
                val wavBytes = WavFileCreator.createWavFileInBytes(iStream!!.readBytes(), WavFileCreator.AudioAttributes(AUDIO_FORMAT, CHANNEL_CONFIG, SAMPLE_RATE))
                iStream.close()

                //write the WAV file to phone
                val oStream = getStoryChildOutputStream(activity,wavRelPath)
                oStream?.write(wavBytes)
                oStream?.close()

                //delete the raw audio data file (pcm file)
                deleteStoryFile(activity,pcmRelPath)
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "Could not find PCM file", e)
            } catch (e: IOException) {
                Log.e(TAG, "Could not read/write to/from file!", e)
            }

        }
    }

    companion object {
        private val TAG = "WavAudioRecorder"
        private val PCM_EXTENSION = ".pcm"
        private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private val SAMPLE_RATE = 44100
        private val INPUT_SOURCE = MediaRecorder.AudioSource.MIC
        private val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    }
}
