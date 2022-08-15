package org.tyndalebt.spadv.tools.media

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.model.PROJECT_DIR
import org.tyndalebt.spadv.model.PhaseType
import org.tyndalebt.spadv.model.Story
import org.tyndalebt.spadv.model.Workspace
import org.tyndalebt.spadv.tools.file.*
import org.tyndalebt.spadv.tools.media.story.AutoStoryMaker
import org.tyndalebt.spadv.tools.media.story.StoryMaker
import org.tyndalebt.spadv.tools.media.story.StoryPage
import java.io.File
import java.io.IOException


//See https://developer.android.com/guide/topics/media/media-formats.html for supported formats.
internal val OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4
internal val AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC
internal val SAMPLE_RATE = 44100
internal val BIT_DEPTH = 16
internal val AUDIO_CHANNELS = 1
internal val MAX_AUDIO_ARCHIVE_COUNT = 3
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

    private fun initRecorder() {
        mRecorder.release()
        mRecorder = MediaRecorder()
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder.setOutputFormat(OUTPUT_FORMAT)
        mRecorder.setAudioEncoder(AUDIO_ENCODER)
        mRecorder.setAudioEncodingBitRate(BIT_RATE)
        mRecorder.setAudioSamplingRate(SAMPLE_RATE)
        mRecorder.setAudioChannels(AUDIO_CHANNELS)
    }

    override fun startNewRecording(relPath: String) {
        initRecorder()
        mRecorder.setOutputFile(getStoryFileDescriptor(activity, relPath, "", "w"))
        isRecording = true
        try {
            mRecorder.prepare()
            mRecorder.start()
        } catch (e: IllegalStateException) {
            Toast.makeText(activity, "IllegalStateException!", Toast.LENGTH_SHORT).show()
            FirebaseCrashlytics.getInstance().recordException(e)
        } catch (e: IOException) {
            Toast.makeText(activity, "IOException!", Toast.LENGTH_SHORT).show()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    override fun stop() {
        if (!isRecording) return
        try {
            mRecorder.stop()
            mRecorder.reset()
            mRecorder.release()
            isRecording = false
            cleanupOlderFiles()
        } catch (stopException: RuntimeException) {
            Toast.makeText(activity, R.string.recording_toolbar_error_recording, Toast.LENGTH_SHORT).show()
            FirebaseCrashlytics.getInstance().recordException(stopException)
        } catch (e: InterruptedException) {
            Log.e(AUDIO_RECORDER, "Voice recorder interrupted!", e)
        }
    }

    private fun isCurrentPhase(fileName: String?): Boolean {
        var prefix: String = when (Workspace.activePhase.phaseType) {
            PhaseType.TRANSLATE_REVISE -> {
                "${Workspace.activePhase.getFileSafeName()}${Workspace.activeSlideNum}_"
            }
            PhaseType.VOICE_STUDIO -> {
                "${Workspace.activePhase.getFileSafeName()}${Workspace.activeSlideNum}_"
            }
            PhaseType.BACK_T -> {
                "${Workspace.activePhase.getFileSafeName()}${Workspace.activeSlideNum}_"
            }
            else -> {
                return false // nothing
            }
        }
        return fileName?.startsWith(prefix, false) == true
    }

    private fun getAudioFiles(phase: PhaseType): MutableList <String> {
        when (Workspace.activePhase.phaseType) {
            PhaseType.TRANSLATE_REVISE -> {
                return Workspace.activeSlide!!.translateReviseAudioFiles
            }
            PhaseType.VOICE_STUDIO -> {
                return Workspace.activeSlide!!.voiceStudioAudioFiles
            }
            PhaseType.BACK_T -> {
                return Workspace.activeSlide!!.backTranslationAudioFiles
            }
            else -> {
                var mList: MutableList <String> = arrayListOf()
                return mList // nothing
            }
        }
    }

    private fun cleanupOlderFiles() {
        var pos: Int? = null
        var mCombName: String
        var aSize: Int
        do
        {
            aSize = getAudioFiles(Workspace.activePhase.phaseType).size
            if (aSize <= MAX_AUDIO_ARCHIVE_COUNT)
                break
            pos = 0
            val recordings = Workspace.activePhase.getCombNames()
            mCombName = recordings!![pos]
            val fileLocation = Story.getFilename(mCombName)
            val filename = mCombName

            recordings.removeAt(pos)
            if (getChosenCombName() == filename) {
                // current chosen WL has been deleted, shift the file index
                if (recordings.size == 0) {
                    setChosenFileIndex(-1)
                }else {
                    setChosenFileIndex(0)
                }
            }
            // delete the recording file
            deleteStoryFile(activity, fileLocation)
        } while (aSize > MAX_AUDIO_ARCHIVE_COUNT)
        if (aSize != 0)
            cleanupUnreferencedFiles()
    }

    private fun cleanupUnreferencedFiles()
    {
        var docFile: DocumentFile? = null
        var aList = getAudioFiles(Workspace.activePhase.phaseType)
        if (aList.size < 1)
            return
        var found: Boolean
        var title: String = Workspace.activeStory.title
        docFile = Workspace.workdocfile.findFile(title)
        docFile = docFile!!.findFile("project")
        val audioList1: List<DocumentFile> = docFile!!.listFiles().filter { isCurrentPhase(it.name) }
        if (audioList1.size <= MAX_AUDIO_ARCHIVE_COUNT)
            return
        var idx: Int = 0
        do {
            val name: String = audioList1[idx].name!!
            var idx1: Int = 0
            found = false
            do {
                if (aList[idx1].contains(name))
                    found = true
                idx1 += 1
            } while (idx1 < aList.size)
            if (!found) {

                val fileName = docFile.findFile(name)
                fileName!!.delete()
            }
            idx += 1
        } while (idx < audioList1.size)
    }
}
