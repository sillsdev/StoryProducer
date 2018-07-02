package org.sil.storyproducer.controller.draft

import android.app.AlertDialog
import android.content.Context
import android.media.MediaPlayer
import android.support.v7.widget.Toolbar
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.Modal
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.model.StoryState
import org.sil.storyproducer.tools.StorySharedPreferences
import org.sil.storyproducer.tools.file.AudioFiles
import org.sil.storyproducer.tools.media.AudioPlayer


class DraftListRecordingsModal(private val context: Context, private val parentFragment: DraftFrag) : RecordingsListAdapter.ClickListeners, Modal {
    private var rootView: LinearLayout? = null
    private var dialog: AlertDialog? = null

    private var draftTitles: Array<String>? = null
    private var lastNewName: String? = null
    private var lastOldName: String? = null

    private val audioPlayer: AudioPlayer
    private var currentPlayingButton: ImageButton? = null

    init {
        audioPlayer = AudioPlayer()
    }

    override fun show() {
        val inflater = parentFragment.activity.layoutInflater
        rootView = inflater.inflate(R.layout.recordings_list, null) as LinearLayout

        createRecordingList()


        val tb = rootView!!.findViewById<Toolbar>(R.id.toolbar2)
        //Note that user-facing slide number is 1-based while it is 0-based in code.
        tb.setTitle(R.string.draft_recordings_title)
        val exit = rootView!!.findViewById<ImageButton>(R.id.exitButton)

        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setView(rootView)
        dialog = alertDialog.create()
        exit.setOnClickListener { dialog!!.dismiss() }
        dialog!!.show()

    }

    /**
     * Updates the list of draft recordings at beginning of fragment creation and after any list change
     */
    private fun createRecordingList() {
        val listView = rootView!!.findViewById<ListView>(R.id.recordings_list)
        listView.isScrollbarFadingEnabled = false
        val adapter = RecordingsListAdapter(context, draftTitles, slidePosition, this)
        adapter.setDeleteTitle(context.resources.getString(R.string.delete_draft_title))
        adapter.setDeleteMessage(context.resources.getString(R.string.delete_draft_message))
        listView.adapter = adapter
    }

    override fun onRowClick(recordingTitle: String) {
        StorySharedPreferences.setDraftForSlideAndStory(recordingTitle, slidePosition, StoryState.getStoryName())
        parentFragment.updatePlayBackPath()
        dialog!!.dismiss()
    }

    override fun onPlayClick(recordingTitle: String, buttonClickedNow: ImageButton) {
        parentFragment.stopPlayBackAndRecording()
        if (audioPlayer.isAudioPlaying && currentPlayingButton == buttonClickedNow) {
            currentPlayingButton!!.setImageResource(R.drawable.ic_green_play)
            audioPlayer.stopAudio()
        } else {
            if (audioPlayer.isAudioPlaying) {
                currentPlayingButton!!.setImageResource(R.drawable.ic_green_play)
                audioPlayer.stopAudio()
            }
            currentPlayingButton = buttonClickedNow
            currentPlayingButton!!.setImageResource(R.drawable.ic_stop_red)
            audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener { currentPlayingButton!!.setImageResource(R.drawable.ic_green_play) })
            val draftFile = AudioFiles.getDraft(StoryState.getStoryName(), slidePosition, recordingTitle)
            if (draftFile.exists()) {
                //FIXME
                //audioPlayer.setSource(draftFile.getPath());
                audioPlayer.playAudio()
                Toast.makeText(parentFragment.context, context.getString(R.string.draft_playing_draft), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(parentFragment.context, context.getString(R.string.draft_no_draft_found), Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun onDeleteClick(recordingTitle: String) {
        AudioFiles.deleteDraft(StoryState.getStoryName(), slidePosition, recordingTitle)
        createRecordingList()
        if (StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()) == recordingTitle) {        //deleted the selected file
            if (draftTitles!!.size > 0) {
                StorySharedPreferences.setDraftForSlideAndStory(draftTitles!![draftTitles!!.size - 1], slidePosition, StoryState.getStoryName())
            } else {
                StorySharedPreferences.setDraftForSlideAndStory("", slidePosition, StoryState.getStoryName())       //no stories to set it to
                parentFragment.hideButtonsToolbar()
            }

        }
        parentFragment.updatePlayBackPath()
    }

    override fun onRenameClick(name: String, newName: String): AudioFiles.RenameCode {
        lastOldName = name
        lastNewName = newName
        return AudioFiles.renameDraft(StoryState.getStoryName(), slidePosition, name, newName)
    }

    override fun onRenameSuccess() {
        createRecordingList()
        if (StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()) == lastOldName) {
            StorySharedPreferences.setDraftForSlideAndStory(lastNewName, slidePosition, StoryState.getStoryName())
        }
        parentFragment.updatePlayBackPath()
    }
}
