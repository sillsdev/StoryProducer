package org.sil.storyproducer.controller.adapter

import android.app.AlertDialog
import android.content.Context
import android.media.MediaPlayer
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.Modal
import org.sil.storyproducer.controller.keyterm.KeytermRecordingListAdapter
import org.sil.storyproducer.controller.keyterm.KeytermRecordingListAdapter
import org.sil.storyproducer.model.KeytermRecording
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.AUDIO_EXT
import org.sil.storyproducer.tools.file.RenameCode
import org.sil.storyproducer.tools.file.renameStoryFile
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

class RecordingsListAdapter(private val values: MutableList<String>?, private val listeners: ClickListeners) : RecyclerView.Adapter<RecordingsListAdapter.ViewHolder>() {

    interface ClickListeners {
        fun onRowClick(name: String)
        fun onPlayClick(name: String, buttonClickedNow: ImageButton)
        fun onDeleteClick(name: String, pos: Int)
        fun onRenameClick(name: String, newName: String): RenameCode
        fun onRenameSuccess(pos: Int)
    }

    private var selectedPos = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val individualAudio = inflater.inflate(R.layout.audio_comment_list_item, parent, false)

        return ViewHolder(individualAudio)
    }

    override fun getItemCount(): Int = values?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioText = values?.get(position)
        if (audioText != null) {
            if (Workspace.activePhase.getChosenFilename().substringAfterLast('/') == audioText) {
                val color = ContextCompat.getColor(holder.itemView.context, R.color.primary)
                holder.itemView.setBackgroundColor(color)
                selectedPos = holder.adapterPosition
            }
            else{
                val color = ContextCompat.getColor(holder.itemView.context, R.color.black)
                holder.itemView.setBackgroundColor(color)
            }
            holder.bindView(audioText)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(text: String) {
            itemView.setOnClickListener {
                notifyItemChanged(selectedPos)
                selectedPos = adapterPosition
                notifyItemChanged(selectedPos)
                listeners.onRowClick(values?.get(adapterPosition).toString())
            }
            itemView.setOnLongClickListener {
                showItemRenameDialog(adapterPosition)
                return@setOnLongClickListener true
            }
            val messageButton = itemView.findViewById<TextView>(R.id.audio_comment_title)
            messageButton.text = text.substringBeforeLast('.')

            val playButton = itemView.findViewById<ImageButton>(R.id.audio_comment_play_button)
            playButton.setOnClickListener {
                listeners.onPlayClick(text, playButton)
            }

            val deleteButton = itemView.findViewById<ImageButton>(R.id.audio_comment_delete_button)
            deleteButton.setOnClickListener {
                showDeleteItemDialog(adapterPosition, text)
            }
        }

        private fun showDeleteItemDialog(position: Int, text: String) {
            val dialog = AlertDialog.Builder(itemView.context)
                    .setTitle(itemView.context.getString(R.string.delete_audio_title))
                    .setMessage(itemView.context.getString(R.string.delete_audio_message))
                    .setNegativeButton(itemView.context.getString(R.string.no), null)
                    .setPositiveButton(itemView.context.getString(R.string.yes)) { _, _ ->
                        listeners.onDeleteClick(text, position)
                        notifyItemRemoved(position)
                        notifyItemChanged(values?.size!! -1)
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
            val newName = EditText(itemView.context)

            // Programmatically set layout properties for edit text field
            val params = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            // Apply layout properties
            newName.layoutParams = params

            val dialog = AlertDialog.Builder(itemView.context)
                    .setTitle(itemView.context.getString(R.string.rename_title))
                    .setView(newName)
                    .setNegativeButton(itemView.context.getString(R.string.cancel), null)
                    .setPositiveButton(itemView.context.getString(R.string.save)) { _, _ ->
                        val returnCode = listeners.onRenameClick(values?.get(position)!!, newName.text.toString())
                        when (returnCode) {
                            RenameCode.SUCCESS -> {
                                listeners.onRenameSuccess(position)
                                notifyItemChanged(selectedPos)
                                notifyItemChanged(position)
                                Toast.makeText(itemView.context, itemView.context.resources.getString(R.string.renamed_success), Toast.LENGTH_SHORT).show()
                            }
                            RenameCode.ERROR_LENGTH -> Toast.makeText(itemView.context, itemView.context.resources.getString(R.string.rename_must_be_20), Toast.LENGTH_SHORT).show()
                            RenameCode.ERROR_SPECIAL_CHARS -> Toast.makeText(itemView.context, itemView.context.resources.getString(R.string.rename_no_special), Toast.LENGTH_SHORT).show()
                            RenameCode.ERROR_UNDEFINED -> Toast.makeText(itemView.context, itemView.context.resources.getString(R.string.rename_failed), Toast.LENGTH_SHORT).show()
                        }
                    }.create()

            dialog.show()
            //TODO make keyboard show at once, but right now there are too many issues with it.
        }
    }

    class RecordingsListModal(private val context: Context, private val toolbar: RecordingToolbar?) : RecordingsListAdapter.ClickListeners, KeytermRecordingListAdapter.ClickListeners, Modal {
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
        private var playbackListener: RecordingToolbar.RecordingListener? = null
        private var slideNum: Int = Workspace.activeSlideNum

        fun setSlideNum(mSlideNum:Int){
            slideNum = mSlideNum
        }

        fun setParentFragment(parentFragment: Fragment?){
            try{
                playbackListener = parentFragment as RecordingToolbar.RecordingListener
            }
            catch (e : ClassCastException){
                playbackListener = context as RecordingToolbar.RecordingListener
            }
            catch (e:Exception){}
        }

        fun embedList(view: ViewGroup) {
            rootView = view
            embedded = true
        }

        override fun show() {
            if (!embedded) {
                val inflater = LayoutInflater.from(context)
                rootView = inflater?.inflate(R.layout.recordings_list, rootView) as ViewGroup?
            }

            recyclerView = rootView?.findViewById(R.id.recordings_list)

            updateRecordingList()

            if(Workspace.activePhase.phaseType == PhaseType.KEYTERM){
                recyclerView?.adapter = KeytermRecordingListAdapter(context, Workspace.activeKeyterm.keytermRecordings, rootView?.findViewById(R.id.bottom_sheet)!!, this)
            }
            else{
                recyclerView?.adapter = RecordingsListAdapter(strippedFilenames, this)
                recyclerView?.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
            recyclerView?.layoutManager = LinearLayoutManager(context)
            recyclerView?.setHasFixedSize(true)

            if (!embedded) {
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

        /**
         * Updates the list of draft recordings at beginning of fragment creation and after any list change
         */
        fun updateRecordingList() {
            filenames = Workspace.activePhase.getRecordedAudioFiles(slideNum)!!
            strippedFilenames = filenames
            if (strippedFilenames != null) {
                for (i in 0 until strippedFilenames!!.size) {
                    strippedFilenames!![i] = strippedFilenames!![i].substringAfterLast('/')
                }
            }
        }

        override fun onRowClick(name: String) {
            Workspace.activePhase.setChosenFilename("${Workspace.activeDir}/$name")
        }

        override fun onPlayClick(name: String, buttonClickedNow: ImageButton) {
            if (audioPlayer.isAudioPlaying && currentPlayingButton == buttonClickedNow) {
                currentPlayingButton!!.setImageResource(R.drawable.ic_play_arrow_white_36dp)
                audioPlayer.stopAudio()
            } else {
                stopAudio()
                playbackListener?.onStartedRecordingOrPlayback(false)
                currentPlayingButton = buttonClickedNow
                currentPlayingButton?.setImageResource(R.drawable.ic_stop_white_36dp)
                audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
                    currentPlayingButton?.setImageResource(R.drawable.ic_play_arrow_white_36dp)
                    audioPlayer.stopAudio()
                })
                if (storyRelPathExists(context, "${Workspace.activeDir}/$name")) {
                    audioPlayer.setStorySource(context, "${Workspace.activeDir}/$name")
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

        override fun onDeleteClick(name: String, pos: Int){
            Workspace.deleteAudioFileFromList(context,name,pos)
            updateRecordingList()
            if ("${Workspace.activeDir}/$name" == Workspace.activePhase.getChosenFilename()) {
                if (filenames.size > 0) {
                    onRowClick(filenames.last())
                }
                else {
                    Workspace.activePhase.setChosenFilename("")
                    toolbar?.setupToolbarButtons()
                    dialog?.dismiss()
                }
            }
        }

        override fun onRenameClick(name: String, newName: String): RenameCode {
            lastOldName = name
            val tempName = newName + AUDIO_EXT
            lastNewName = tempName
            return when (renameStoryFile(name, tempName)) {
                true -> RenameCode.SUCCESS
                false -> RenameCode.ERROR_UNDEFINED
            }
        }

        override fun onRenameSuccess(pos: Int) {
            updateRecordingList()
            filenames[pos] = lastNewName!!
            if(Workspace.activePhase.phaseType == PhaseType.KEYTERM) {
                Workspace.activeKeyterm.keytermRecordings[pos] = KeytermRecording("${Workspace.activeKeyterm.term}/$lastNewName", Workspace.activeKeyterm.keytermRecordings[pos].textBackTranslation )
            }
            updateRecordingList()
            onRowClick(lastNewName.toString())
        }

        fun stopAudio() {
            if (audioPlayer.isAudioPlaying) {
                currentPlayingButton?.setImageResource(R.drawable.ic_play_arrow_white_36dp)
                audioPlayer.stopAudio()
            }
        }
    }
}
