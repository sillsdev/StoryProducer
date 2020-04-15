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
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.RecordingList
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

class RecordingsListAdapter(private val recordings: RecordingList, private val listeners: ClickListeners) : RecyclerView.Adapter<RecordingsListAdapter.ViewHolder>() {

    interface ClickListeners {
        fun onRowClick(pos: Int)
        fun onPlayClick(pos: Int, buttonClickedNow: ImageButton)
        fun onDeleteClick(name: String, pos: Int)
        fun onRenameClick(pos: Int, newName: String)
    }

    private var selectedPos = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val individualAudio = inflater.inflate(R.layout.audio_comment_list_item, parent, false)

        return ViewHolder(individualAudio)
    }

    override fun getItemCount(): Int = recordings.getFiles().size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (recordings.selectedIndex == position) {
            val color = ContextCompat.getColor(holder.itemView.context, R.color.primary)
            holder.itemView.setBackgroundColor(color)
            selectedPos = holder.adapterPosition
        } else {
            val color = ContextCompat.getColor(holder.itemView.context, R.color.black)
            holder.itemView.setBackgroundColor(color)
        }
        holder.bindView(recordings.getFiles()[position].displayName)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(text: String) {
            itemView.setOnClickListener {
                notifyItemChanged(selectedPos)
                selectedPos = adapterPosition
                notifyItemChanged(selectedPos)
                listeners.onRowClick(adapterPosition)
            }
            itemView.setOnLongClickListener {
                showItemRenameDialog(adapterPosition)
                return@setOnLongClickListener true
            }
            val messageButton = itemView.findViewById<TextView>(R.id.audio_comment_title)
            messageButton.text = text

            val playButton = itemView.findViewById<ImageButton>(R.id.audio_comment_play_button)
            playButton.setOnClickListener {
                listeners.onPlayClick(adapterPosition, it as ImageButton)
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
                        listeners.onRenameClick(position, newName.text.toString())
                        notifyDataSetChanged()
                    }.create()

            dialog.show()
            //TODO make keyboard show at once, but right now there are too many issues with it.
        }
    }

    class RecordingsListModal(private val context: Context, private val toolbar: RecordingToolbar?, private val phaseType: PhaseType) : ClickListeners, Modal {
        private lateinit var rootView: ViewGroup
        private var dialog: AlertDialog? = null
        private var displayNames = RecordingList()
        internal lateinit var recyclerView: RecyclerView
        private val audioPlayer: AudioPlayer = AudioPlayer()
        private var currentPlayingButton: ImageButton? = null
        private var audioPos = -1
        private var embedded = false
        private var playbackListener: RecordingToolbar.ToolbarMediaListener? = null
        private var slideNumber: Int = Workspace.activeStory.lastSlideNum

        // TODO @pwhite: Can I remove this function?
        fun setSlideNum(mSlideNum: Int) {
            slideNumber = mSlideNum
        }

        fun setParentFragment(parentFragment: Fragment) {
            try {
                playbackListener = parentFragment as RecordingToolbar.ToolbarMediaListener
            } catch (e: ClassCastException) {
                playbackListener = context as RecordingToolbar.ToolbarMediaListener
            } catch (e: Exception) {
            }
        }

        fun embedList(view: ViewGroup) {
            rootView = view
            embedded = true
        }

        override fun show() {
            if (!embedded) {
                val inflater = LayoutInflater.from(context)
                rootView = inflater.inflate(R.layout.recordings_list, null) as ViewGroup
            }

            recyclerView = rootView.findViewById(R.id.recordings_list)

            resetRecordingList()
            recyclerView.adapter = RecordingsListAdapter(displayNames, this)
            recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            recyclerView.layoutManager = LinearLayoutManager(context)

            if (!embedded) {
                val tb = rootView.findViewById<Toolbar>(R.id.toolbar2)
                tb?.setTitle(R.string.recordings_title)

                val alertDialog = AlertDialog.Builder(context)
                alertDialog.setView(rootView)
                dialog = alertDialog.create()

                val exit = rootView.findViewById<ImageButton>(R.id.exitButton)
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
         * Initializes the list of draft recordings at beginning of fragment creation and after externally driven list changes.
         */
        fun resetRecordingList() {
            //only update if there was a change.
            displayNames = phaseType.getRecordings()
            recyclerView.adapter = RecordingsListAdapter(displayNames, this)
        }

        override fun onRowClick(pos: Int) {
            displayNames.selectedIndex = pos
        }

        override fun onPlayClick(pos: Int, buttonClickedNow: ImageButton) {
            if (audioPlayer.isAudioPlaying && audioPos == pos) {
                stopAudio()
            } else {
                stopAudio()
                audioPos = pos

                toolbar?.stopToolbarMedia()

                playbackListener?.onStartedToolbarMedia()

                currentPlayingButton = buttonClickedNow
                currentPlayingButton?.setImageResource(R.drawable.ic_stop_white_36dp)

                audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
                    buttonClickedNow.setImageResource(R.drawable.ic_play_arrow_white_36dp)
                    stopAudio()
                })

                val recordingFile = phaseType.getRecordings().getFiles()[pos].fileName
                if (storyRelPathExists(context, recordingFile)) {
                    audioPlayer.setStorySource(context, recordingFile)
                    audioPlayer.playAudio()
                    when (phaseType) {
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

        override fun onDeleteClick(name: String, pos: Int) {
            deleteAudioFileFromList(context, pos)
            recyclerView.adapter!!.notifyDataSetChanged()
            if (displayNames.getFiles().isEmpty()) {
                toolbar?.updateInheritedToolbarButtonVisibility()
                dialog?.dismiss()
            }
        }

        override fun onRenameClick(pos: Int, newName: String) {
            updateDisplayName(pos, newName)
            displayNames.selectedIndex = pos
            recyclerView.adapter!!.notifyDataSetChanged()
        }

        fun stopAudio() {
            if (audioPlayer.isAudioPlaying) {
                currentPlayingButton?.setImageResource(R.drawable.ic_play_arrow_white_36dp)
                audioPlayer.stopAudio()
            }
        }
    }
}
