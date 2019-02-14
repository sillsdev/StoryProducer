package org.sil.storyproducer.controller.adapter

import android.app.AlertDialog
import android.content.Context
import android.media.MediaPlayer
import android.support.design.widget.BottomSheetBehavior
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
import org.sil.storyproducer.controller.keyterm.KeyTermActivity
import org.sil.storyproducer.controller.keyterm.RecyclerDataAdapter
import org.sil.storyproducer.model.BackTranslation
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.AUDIO_EXT
import org.sil.storyproducer.tools.file.RenameCode
import org.sil.storyproducer.tools.file.renameStoryFile
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

class RecordingsListAdapter(private val values: MutableList<String>?) : RecyclerView.Adapter<RecordingsListAdapter.ViewHolder>() {

    var onItemClick: ((String) -> Unit)? = null
    var onItemLongClick: ((Int) -> Unit)? = null
    var onPlayClick: ((String, ImageButton) -> Unit)? = null
    var onDeleteClick: ((String, Int) -> Unit)? = null
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
            if (Workspace.activePhase.getChosenFilename().contains(audioText)) {
                val color = ContextCompat.getColor(holder.itemView.context, R.color.primary)
                holder.itemView.setBackgroundColor(color)
                selectedPos = position
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
                onItemClick?.invoke(values?.get(adapterPosition).toString())
            }
            itemView.setOnLongClickListener {
                onItemLongClick?.invoke(adapterPosition)
                return@setOnLongClickListener true
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

    class RecordingsListModal(private val context: Context, private val toolbar: RecordingToolbar?, private val adapter: RecyclerView? = null) : Modal {
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

        fun setParentFragment(parentFragment: Fragment){
            try{
                playbackListener = parentFragment as RecordingToolbar.RecordingListener
            }catch (e:Exception){}
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

            updateRecordingList()
            if (adapter != null) {
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
            } else {

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

        private fun showDeleteItemDialog(name: String, position: Int) {
            val dialog = AlertDialog.Builder(context)
                    .setTitle("Delete Recording")
                    .setMessage("Are you sure you want to delete recording: $name?")
                    .setNegativeButton(context.getString(R.string.no), null)
                    .setPositiveButton(context.getString(R.string.yes)) { _, _ ->
                        onDeleteClick(name,position)
                        if (adapter == null) {
                            (recyclerView?.adapter as RecordingsListAdapter).notifyDataSetChanged()
                        } else {
                            (adapter.adapter as RecyclerDataAdapter).notifyDataSetChanged()
                        }
                        if(Workspace.activePhase.phaseType == PhaseType.KEYTERM){
                            updateBottomSheetState(context)
                        }
                    }
                    .create()

            dialog.show()
        }

        private fun updateBottomSheetState(context: Context){
            val bottomSheetBehavior = BottomSheetBehavior.from((context as KeyTermActivity).bottomSheet)
            if(bottomSheetBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
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
                        filenames = Workspace.activePhase.getRecordedAudioFiles(slideNum)!!
                        val returnCode = onRenameClick(filenames[position], newName.text.toString())
                        when (returnCode) {
                            RenameCode.SUCCESS -> {
                                onRenameSuccess(position)
                                Toast.makeText(context, context.resources.getString(R.string.renamed_success), Toast.LENGTH_SHORT).show()
                            }
                            RenameCode.ERROR_LENGTH -> Toast.makeText(context, context.resources.getString(R.string.rename_must_be_20), Toast.LENGTH_SHORT).show()
                            RenameCode.ERROR_SPECIAL_CHARS -> Toast.makeText(context, context.resources.getString(R.string.rename_no_special), Toast.LENGTH_SHORT).show()
                            RenameCode.ERROR_UNDEFINED -> Toast.makeText(context, context.resources.getString(R.string.rename_failed), Toast.LENGTH_SHORT).show()
                        }
                    }.create()

            dialog.show()
            //TODO make keyboard show at once, but right now there are too many issues with it.
        }

        /**
         * Updates the list of draft recordings at beginning of fragment creation and after any list change
         */
        fun updateRecordingList() {
            filenames = Workspace.activePhase.getRecordedAudioFiles(slideNum)!!
            strippedFilenames = filenames
            if (strippedFilenames != null) {
                for (i in 0 until strippedFilenames!!.size) {
                    strippedFilenames!![i] = strippedFilenames!![i].split("/").last()
                }
            }
        }

        private fun onRowClick(name: String) {
            Workspace.activePhase.setChosenFilename("${Workspace.activeDir}/$name")
            if (adapter == null) {
                (recyclerView?.adapter as RecordingsListAdapter).notifyDataSetChanged()
            } else {
                (adapter.adapter as RecyclerDataAdapter).notifyDataSetChanged()
            }
        }

        private fun onPlayClick(name: String, buttonClickedNow: ImageButton) {
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
                if (storyRelPathExists(context, name)) {
                    audioPlayer.setStorySource(context, name)
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

        private fun onDeleteClick(name: String, position: Int){
            Workspace.deleteAudioFileFromList(context,name,position)
            updateRecordingList()
            if ("${Workspace.activeDir}/$name" == Workspace.activePhase.getChosenFilename()) {
                if (filenames.size > 0) {
                    Workspace.activePhase.setChosenFilename(filenames.last())
                }
                else {
                    Workspace.activePhase.setChosenFilename("")
                    toolbar?.setupToolbarButtons()
                    dialog?.dismiss()
                }
            }
        }

        private fun onRenameClick(name: String, newName: String): RenameCode {
            lastOldName = name
            val tempName = newName + AUDIO_EXT
            lastNewName = tempName
            return when (renameStoryFile(name, tempName)) {
                true -> RenameCode.SUCCESS
                false -> RenameCode.ERROR_UNDEFINED
            }
        }

        private fun onRenameSuccess(index: Int) {
            //val index = filenames.indexOf("$PROJECT_DIR/$lastOldName")
            filenames.removeAt(index)
            filenames.add(index, "$lastNewName")
            if(Workspace.activePhase.phaseType == PhaseType.KEYTERM) {
                Workspace.activeKeyterm.backTranslations[index] = BackTranslation(Workspace.activeKeyterm.backTranslations[index].textBackTranslation, "${Workspace.activeKeyterm.term}/$lastNewName")
            }
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
