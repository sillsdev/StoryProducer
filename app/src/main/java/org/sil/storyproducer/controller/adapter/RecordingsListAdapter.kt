package org.sil.storyproducer.controller.adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.Modal
import org.sil.storyproducer.controller.keyterm.RecyclerDataAdapter
import org.sil.storyproducer.model.PROJECT_DIR
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

class RecordingsListAdapter(private val values: MutableList<String>?) : RecyclerView.Adapter<RecordingsListAdapter.ViewHolder>() {

    var onItemClick: ((String) -> Unit)? = null
    var onItemLongClick: ((Int) -> Unit)? = null
    var onPlayClick: ((String, ImageButton) -> Unit)? = null
    var onDeleteClick: ((String, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val individualAudio = inflater.inflate(R.layout.audio_comment_list_item, parent, false)

        return ViewHolder(individualAudio)
    }

    override fun getItemCount(): Int = values?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioText = values?.get(position)
        if (audioText != null) {
            holder.bindView(audioText)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(values?.get(adapterPosition).toString())
            }
            itemView.setOnLongClickListener {
                onItemLongClick?.invoke(adapterPosition)
                return@setOnLongClickListener true
            }
        }

        fun bindView(text: String) {
            if (Workspace.activePhase.getChosenFilename().contains(text)) {
                itemView.setBackgroundColor(Color.CYAN)
            }
            val messageButton = itemView.findViewById<TextView>(R.id.audio_comment_title)
            messageButton.text = text

            val playButton = itemView.findViewById<ImageButton>(R.id.audio_comment_play_button)
            playButton.setOnClickListener {
                onPlayClick?.invoke(text, playButton)
            }

            val deleteButton = itemView.findViewById<ImageButton>(R.id.audio_comment_delete_button)
            deleteButton.setOnClickListener {
                onDeleteClick?.invoke(text, this.adapterPosition)
            }
        }

    }


    class RecordingsListModal(private val parentView: View?, private val context: Context, private val toolbar: RecordingToolbar?, private val adapter: RecyclerView? = null) : Modal {
        private var rootView: ViewGroup? = null
        private var dialog: AlertDialog? = null
        private var filenames: MutableList<String> = mutableListOf()
        private var strippedFilenames: MutableList<String>? = null
        internal var recyclerView: RecyclerView? = null
        private var lastNewName: String? = null
        private var lastOldName: String? = null
        private val audioPlayer: AudioPlayer = AudioPlayer()
        private var currentPlayingButton: ImageButton? = null
        private var embedded = false

        private val slideNum: Int = Workspace.activeSlideNum

        fun embedList(view: ViewGroup){
            rootView = view
            embedded = true
        }

        override fun show() {
            val inflater = LayoutInflater.from(parentView?.context)

            if(!embedded) {
                rootView = inflater?.inflate(R.layout.recordings_list, rootView) as ViewGroup?
            }

            filenames = Workspace.activePhase.getRecordedAudioFiles(slideNum)!!

            if(adapter!= null){
                (adapter.adapter as RecyclerDataAdapter).onItemClick = { value ->
                    onRowClick(value)
                }
                (adapter.adapter as RecyclerDataAdapter).onItemLongClick = { pos ->
                    showItemRenameDialog(pos)
                }
                (adapter.adapter as RecyclerDataAdapter).onPlayClick = { name, button ->
                    onPlayClick(name, button)
                }
                (adapter.adapter as RecyclerDataAdapter).onDeleteClick = { name, pos ->
                    showDeleteItemDialog(name, pos)
                }
            }
            else{
                updateRecordingList()

                val viewManager = LinearLayoutManager(context)
                val viewAdapter = RecordingsListAdapter(strippedFilenames)

                recyclerView = rootView?.findViewById(R.id.recordings_list)
                recyclerView?.setHasFixedSize(true)
                recyclerView?.adapter = viewAdapter
                recyclerView?.layoutManager = viewManager
                recyclerView?.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

                (viewAdapter).onItemClick = { value ->
                    onRowClick(value)
                }
                (viewAdapter).onItemLongClick = { pos ->
                    showItemRenameDialog(pos)
                }
                (viewAdapter).onPlayClick = { name, button ->
                    onPlayClick(name, button)
                }
                (viewAdapter).onDeleteClick = { name, pos ->
                    showDeleteItemDialog(name, pos)
                }
            }

            if(!embedded) {
                val tb = rootView?.findViewById<Toolbar>(R.id.toolbar2)
                tb?.setTitle(R.string.recordings_title)

                val alertDialog = AlertDialog.Builder(context)
                alertDialog.setView(rootView)
                dialog = alertDialog.create()

                val exit = rootView?.findViewById<ImageButton>(R.id.exitButton)
                exit?.setOnClickListener {
                    dialog?.dismiss()
                }
                dialog?.setOnDismissListener {
                    if (audioPlayer.isAudioPlaying) {
                        audioPlayer.stopAudio()
                    }
                }
                dialog?.show()
            }
        }

        private fun showDeleteItemDialog(name: String, position: Int) {
            val dialog = AlertDialog.Builder(context)
                    .setTitle("Delete Recording")
                    .setMessage("Are you sure you want to delete recording: $name?")
                    .setNegativeButton(context.getString(R.string.no), null)
                    .setPositiveButton(context.getString(R.string.yes)) { _, _ ->
                        onDeleteClick(name)
                        updateRecordingList()
                        if(adapter == null) {
                            (recyclerView?.adapter as RecordingsListAdapter).notifyItemRemoved(position)
                        }
                        else{
                            (adapter?.adapter as RecyclerDataAdapter).notifyItemRemoved(position)
                        }
                    }
                    .create()

            dialog.show()
        }

        /**
         * Show to the user a dialog to rename the audio comment
         *
         * @param position the integer position of the comment the user "long-clicked"
         */
        private fun showItemRenameDialog(position: Int) {
            val newName = EditText(context)

            // Programmatically set layout properties for edit text field
            val params = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            // Apply layout properties
            newName.layoutParams = params

            val dialog = AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.rename_title))
                    .setView(newName)
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .setPositiveButton(context.getString(R.string.save)) { _, _ ->
                        val returnCode = onRenameClick(filenames[position], newName.text.toString())
                        when (returnCode) {
                            AudioFiles.RenameCode.SUCCESS -> {
                                onRenameSuccess()
                                Toast.makeText(context, context.resources.getString(R.string.renamed_success), Toast.LENGTH_SHORT).show()
                            }
                            AudioFiles.RenameCode.ERROR_LENGTH -> Toast.makeText(context, context.resources.getString(R.string.rename_must_be_20), Toast.LENGTH_SHORT).show()
                            AudioFiles.RenameCode.ERROR_SPECIAL_CHARS -> Toast.makeText(context, context.resources.getString(R.string.rename_no_special), Toast.LENGTH_SHORT).show()
                            AudioFiles.RenameCode.ERROR_UNDEFINED -> Toast.makeText(context, context.resources.getString(R.string.rename_failed), Toast.LENGTH_SHORT).show()
                        }
                    }.create()

            dialog.show()
            //TODO make keyboard show at once, but right now there are too many issues with it.
        }

        /**
         * Updates the list of draft recordings at beginning of fragment creation and after any list change
         */
        fun updateRecordingList() {
            strippedFilenames = filenames
            if (strippedFilenames != null) {
                for (i in 0 until strippedFilenames!!.size) {
                    strippedFilenames!![i] = strippedFilenames!![i].split("/").last()
                }
            }
        }

        private fun onRowClick(name: String) {
            if (Workspace.activePhase.phaseType == PhaseType.KEYTERM) {
                Workspace.activePhase.setChosenFilename("${Workspace.activeKeyterm.term}_${Workspace.activeKeyterm.term.hashCode()}/$name")
            } else {
                Workspace.activePhase.setChosenFilename("$PROJECT_DIR/$name")
            }
            dialog?.dismiss()
        }

        fun onPlayClick(name: String, buttonClickedNow: ImageButton) {
            if (audioPlayer.isAudioPlaying && currentPlayingButton == buttonClickedNow) {
                currentPlayingButton!!.setImageResource(R.drawable.ic_play_arrow_white_36dp)
                audioPlayer.stopAudio()
            } else {
                stopAudio()
                currentPlayingButton = buttonClickedNow
                currentPlayingButton?.setImageResource(R.drawable.ic_stop_white_36dp)
                audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
                    currentPlayingButton?.setImageResource(R.drawable.ic_play_arrow_white_36dp)
                    audioPlayer.stopAudio()
                })
                if (Workspace.activePhase.phaseType == PhaseType.KEYTERM) {
                    if (storyRelPathExists(context, name, "keyterms")) {
                        audioPlayer.setStorySource(context, name, "keyterms")
                        audioPlayer.playAudio()
                        Toast.makeText(context, context.getString(R.string.recording_toolbar_play_back_recording), Toast.LENGTH_SHORT).show()
                        when (Workspace.activePhase.phaseType) {
                            PhaseType.DRAFT -> saveLog(context.getString(R.string.DRAFT_PLAYBACK))
                            PhaseType.COMMUNITY_CHECK -> saveLog(context.getString(R.string.COMMENT_PLAYBACK))
                            else -> {
                            }
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.recording_toolbar_no_recording), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (storyRelPathExists(context, "$PROJECT_DIR/$name")) {
                        audioPlayer.setStorySource(context, "$PROJECT_DIR/$name")
                        audioPlayer.playAudio()
                        Toast.makeText(context, context.getString(R.string.recording_toolbar_play_back_recording), Toast.LENGTH_SHORT).show()
                        when (Workspace.activePhase.phaseType) {
                            PhaseType.DRAFT -> saveLog(context.getString(R.string.DRAFT_PLAYBACK))
                            PhaseType.COMMUNITY_CHECK -> saveLog(context.getString(R.string.COMMENT_PLAYBACK))
                            else -> {
                            }
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.recording_toolbar_no_recording), Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

        private fun onDeleteClick(name: String) {
            filenames.remove(name)
            if (Workspace.activePhase.phaseType == PhaseType.KEYTERM) {
                for ((i, audioFile) in Workspace.activeKeyterm.backTranslations.withIndex()) {
                    if (audioFile.audioBackTranslation == name) {
                        Workspace.activeKeyterm.backTranslations.removeAt(i)
                        break
                    }
                }
                deleteStoryFile(context, name, "keyterms")
                if (name == Workspace.activePhase.getChosenFilename()) {
                    if (filenames.size > 0)
                        Workspace.activePhase.setChosenFilename(filenames.last())
                    else {
                        Workspace.activePhase.setChosenFilename("")
                        rootView?.findViewById<LinearLayout>(R.id.toolbar_for_recording_toolbar)?.removeAllViews()
                        toolbar?.setupToolbarButtons()
                        dialog?.dismiss()
                    }
                }
            } else {
                deleteStoryFile(context, "$PROJECT_DIR/$name")
                if ("$PROJECT_DIR/$name" == Workspace.activePhase.getChosenFilename()) {
                    if (filenames.size > 0)
                        Workspace.activePhase.setChosenFilename("$PROJECT_DIR/" + filenames.last())
                    else {
                        Workspace.activePhase.setChosenFilename("")
                        rootView?.findViewById<LinearLayout>(R.id.toolbar_for_recording_toolbar)?.removeAllViews()
                        toolbar?.setupToolbarButtons()
                        dialog?.dismiss()
                    }
                }
            }
        }

        fun onRenameClick(name: String, newName: String): AudioFiles.RenameCode {
            lastOldName = name
            val tempName = newName + AUDIO_EXT
            lastNewName = tempName
            return when (renameStoryFile(context, "$PROJECT_DIR/$name", tempName)) {
                true -> AudioFiles.RenameCode.SUCCESS
                false -> AudioFiles.RenameCode.ERROR_UNDEFINED
            }
        }

        fun onRenameSuccess() {
            val index = filenames.indexOf("$PROJECT_DIR/$lastOldName")
            filenames.removeAt(index)
            filenames.add(index, "$PROJECT_DIR/$lastNewName")
            onRowClick(lastNewName.toString())
            updateRecordingList()
        }

        fun stopAudio() {
            if (audioPlayer.isAudioPlaying) {
                currentPlayingButton?.setImageResource(R.drawable.ic_play_arrow_white_36dp)
                audioPlayer.stopAudio()
            }
        }
    }
}