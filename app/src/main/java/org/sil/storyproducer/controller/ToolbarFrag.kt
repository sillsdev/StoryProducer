package org.sil.storyproducer.controller


import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingAdapterV2
import org.sil.storyproducer.controller.draft.DraftFrag
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.PROJECT_DIR
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.AudioRecorder
import org.sil.storyproducer.tools.media.AudioRecorderMP4

/**
 * A simple [Fragment] subclass.
 *
 */
class ToolbarFrag: Fragment() {

    private var toolbar : LinearLayout? = null
    private var micButton: ImageButton? = null
    private var playButton: ImageButton? = null
    private var deleteButton: ImageButton? = null
    private var multiRecordButton: ImageButton? = null
    private var sendAudioButton: ImageButton? = null
    private var enablePlaybackButton: Boolean = false
    private var enableDeleteButton: Boolean = false
    private var enableMultiRecordButton: Boolean = false
    private var enableSendAudioButton: Boolean = false
    private lateinit var voiceRecorder: AudioRecorder
    private var multiRecordModal: Modal? = null
    private var slideNum : Int = 0
    private var listener : OnAudioPlayListener? = null

    interface OnAudioPlayListener {
        fun onPlayButtonClicked(path: String, image : ImageButton, stopImage: Int, playImage : Int)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        voiceRecorder = AudioRecorderMP4(activity!!)
        return inflater.inflate(R.layout.toolbar_for_recording, container, false)
    }
    fun setupToolbarButtons() {
        enablePlaybackButton = arguments?.getBoolean("enablePlaybackButton") ?: false
        enableDeleteButton = arguments?.getBoolean("enableDeleteButton") ?: false
        enableMultiRecordButton = arguments?.getBoolean("enableMultiRecordButton") ?: false
        enableSendAudioButton = arguments?.getBoolean("enableSendAudioButton") ?: false
        slideNum = arguments?.getInt("slideNum") ?: 0

        toolbar = view?.findViewById(R.id.toolbar_for_recording_toolbar)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val spaceLayoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        spaceLayoutParams.width = 0
        val drawables = intArrayOf(R.drawable.ic_mic_white_48dp, R.drawable.ic_play_arrow_white_48dp, R.drawable.ic_delete_forever_white_48dp, R.drawable.ic_playlist_play_white_48dp, R.drawable.ic_send_audio_48dp)
        val imageButtons = arrayOf(ImageButton(context), ImageButton(context), ImageButton(context), ImageButton(context), ImageButton(context))
        val buttonToDisplay = booleanArrayOf(true/*enable mic*/, enablePlaybackButton, enableDeleteButton, enableMultiRecordButton, enableSendAudioButton)

        var buttonSpacing = android.widget.Space(context!!)
        buttonSpacing.layoutParams = spaceLayoutParams
        toolbar?.addView(buttonSpacing)
        toolbar?.addView(android.widget.Space(context)) //Add a space to the left of the first button.
        for (i in drawables.indices) {
            if (buttonToDisplay[i]) {
                imageButtons[i].setBackgroundResource(drawables[i])
                imageButtons[i].visibility = View.VISIBLE
                imageButtons[i].layoutParams = layoutParams
                imageButtons[i].id = i
                toolbar?.addView(imageButtons[i])
                buttonSpacing = android.widget.Space(context)
                buttonSpacing.layoutParams = spaceLayoutParams
                toolbar?.addView(buttonSpacing)

                when (i) {
                    0 -> micButton = imageButtons[i]
                    1 -> playButton = imageButtons[i]
                    2 -> deleteButton = imageButtons[i]
                    3 -> multiRecordButton = imageButtons[i]
                    4 -> sendAudioButton = imageButtons[i]
                }
            }
        }
        val playBackFileExist = storyRelPathExists(activity!!,Workspace.activePhase.getChosenFilename(slideNum))
        if (enablePlaybackButton) {
            playButton?.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        if (enableMultiRecordButton) {
            multiRecordButton?.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        if (enableDeleteButton) {
            deleteButton?.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        if (enableSendAudioButton) {
            sendAudioButton?.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        setOnClickListeners()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is ToolbarFrag.OnAudioPlayListener) {
            listener = context
        }
    }

    private fun setOnClickListeners() {
        micButton?.setOnClickListener {
            //If recording, stop
            micListener()
        }

        if (enablePlaybackButton) {
            playButton?.setOnClickListener{
                //listener?.onPlayButtonClicked(playButton!!, R.drawable.ic_play_arrow_white_48dp)
                val id = listener?.onPlayButtonClicked(Workspace.activePhase.getChosenFilename(), playButton!!, R.drawable.ic_stop_white_48dp, R.drawable.ic_play_arrow_white_48dp)
                stopToolbarMedia()
                Toast.makeText(context!!, R.string.recording_toolbar_play_back_recording, Toast.LENGTH_SHORT).show()
                //TODO: make this logging more robust and encapsulated
                when (Workspace.activePhase.phaseType){
                    PhaseType.DRAFT -> saveLog(context!!.getString(R.string.DRAFT_PLAYBACK))
                    PhaseType.COMMUNITY_CHECK-> saveLog(context!!.getString(R.string.COMMENT_PLAYBACK))
                    else ->{}
                }
            }
        }
        if (enableDeleteButton) {
            deleteButton?.setOnClickListener { stopToolbarMedia() }
        }
        if (enableMultiRecordButton) {
                multiRecordButton?.setOnClickListener {
                    stopToolbarMedia()
                    multiRecordModal = RecordingsListModal(view?.parent as ViewGroup, context!!)
                    multiRecordModal?.show()
                }
        }
        if (enableSendAudioButton) {
            sendAudioButton?.setOnClickListener {
                stopToolbarMedia()
                //sendAudio()
            }
        }
    }

    fun micListener(){
        if (voiceRecorder.isRecording) {
            stopToolbarMedia()
        }
        else {
            //Now we need to start recording!
            val recordingRelPath = assignNewAudioRelPath()
            if (storyRelPathExists(activity!!,recordingRelPath)) {
                val dialog = AlertDialog.Builder(activity!!)
                        .setTitle(activity!!.getString(R.string.overwrite))
                        .setMessage(activity!!.getString(R.string.learn_phase_overwrite))
                        .setNegativeButton(activity!!.getString(R.string.no)) { dialog, id ->
                            //do nothing
                        }
                        .setPositiveButton(activity!!.getString(R.string.yes)) { dialog, id ->
                            //overwrite audio
                            recordAudio(recordingRelPath)
                        }.create()

                dialog.show()
            } else {
                recordAudio(recordingRelPath)
            }
        }
    }

    fun hideButtons() {
        if (enablePlaybackButton) {
            playButton?.visibility = View.INVISIBLE
        }
        if (enableMultiRecordButton) {
            multiRecordButton?.visibility = View.INVISIBLE
        }
        if (enableDeleteButton) {
            deleteButton?.visibility = View.INVISIBLE
        }
        if (enableSendAudioButton) {
            sendAudioButton?.visibility = View.INVISIBLE
        }
    }

    private fun startRecording(recordingRelPath: String) {
        //TODO: make this logging more robust and encapsulated
        voiceRecorder.startNewRecording(recordingRelPath)
    }

    private fun recordAudio(recordingRelPath: String) {
        stopToolbarMedia()
        startRecording(recordingRelPath)
        when(Workspace.activePhase.phaseType){
            PhaseType.DRAFT -> saveLog(activity!!.getString(R.string.DRAFT_RECORDING))
            PhaseType.COMMUNITY_CHECK -> saveLog(activity!!.getString(R.string.COMMENT_RECORDING))
            else -> {}
        }
        micButton?.setBackgroundResource(R.drawable.ic_stop_white_48dp)
        if (enableDeleteButton) {
            deleteButton?.visibility = View.INVISIBLE
        }
        if (enablePlaybackButton) {
            playButton?.visibility = View.INVISIBLE
        }
        if (enableMultiRecordButton) {
            multiRecordButton?.visibility = View.INVISIBLE
        }
        if (enableSendAudioButton) {
            sendAudioButton?.visibility = View.INVISIBLE
        }
    }

    private fun stopToolbarMedia() {
        if (voiceRecorder.isRecording) {
            voiceRecorder.stop()
            micButton?.setBackgroundResource(R.drawable.ic_mic_white_48dp)
        }
        //set playback button visible
        if (enableDeleteButton) {
            deleteButton?.visibility = View.VISIBLE
        }
        if (enablePlaybackButton) {
            playButton?.visibility = View.VISIBLE
        }
        if (enableMultiRecordButton) {
            multiRecordButton?.visibility = View.VISIBLE
        }
        if (enableSendAudioButton) {
            sendAudioButton?.visibility = View.VISIBLE
        }
    }


    inner class RecordingsListModal(private val parentView: View?, private val context: Context) : Modal {
        private var rootView: ViewGroup? = null
        private var dialog: AlertDialog? = null
        private var filenames: MutableList<String>? = null
        private var lastNewName: String? = null
        private var lastOldName: String? = null

        private val slideNum : Int = Workspace.activeSlideNum

        override fun show() {
            val inflater = LayoutInflater.from(parentView?.context)
            rootView = inflater?.inflate(R.layout.recordings_list, rootView) as ViewGroup?

            filenames = Workspace.activePhase.getRecordedAudioFiles(slideNum)

            val strippedFilenames = filenames
            if (strippedFilenames != null) {
                for (i in 0 until strippedFilenames.size){
                    strippedFilenames[i] = strippedFilenames[i].split("/").last()
                }
            }

            val viewManager = LinearLayoutManager(context)
            val viewAdapter = RecordingAdapterV2(strippedFilenames)

            val recyclerView = rootView?.findViewById<RecyclerView>(R.id.recordings_list)
            recyclerView?.setHasFixedSize(true)
            recyclerView?.adapter = viewAdapter
            recyclerView?.layoutManager = viewManager
            recyclerView?.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

            (viewAdapter).onItemClick = { value ->
                onRowClick(value)
            }
            (viewAdapter).onPlayClick = { name, button ->
                onPlayClick(name, button)
            }
            (viewAdapter).onDeleteClick = { name ->
                onDeleteClick(name)
            }

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
//            if (mViewModel?.isAudioPlaying == true) {
//                mViewModel?.stopAudio()
//            }
            }
            dialog?.show()
        }

        /**
         * Updates the list of draft recordings at beginning of fragment creation and after any list change
         */
        private fun createRecordingList() {
            val listView = rootView?.findViewById<RecyclerView>(R.id.recordings_list)
            val strippedFilenames = filenames
            if (strippedFilenames != null) {
                for (i in 0 until strippedFilenames.size){
                    strippedFilenames[i] = strippedFilenames[i].split("/").last()
                }
            }
            (listView?.adapter as RecordingAdapterV2).notifyDataSetChanged()
            val adapter = RecordingAdapterV2(strippedFilenames)
            //adapter.setDeleteTitle(context.resources.getString(R.string.delete_draft_title))
            //adapter.setDeleteMessage(context.resources.getString(R.string.delete_draft_message))
            listView.adapter = adapter

        }

        fun onRowClick(name: String) {
            Workspace.activePhase.setChosenFilename("$PROJECT_DIR/$name")
            dialog?.dismiss()
        }

        fun onPlayClick(name: String, buttonClickedNow: ImageButton) {
            listener?.onPlayButtonClicked("$PROJECT_DIR/$name", buttonClickedNow, R.drawable.ic_stop_white_36dp, R.drawable.ic_play_arrow_white_36dp)
            when (Workspace.activePhase.phaseType){
                PhaseType.DRAFT -> saveLog(context.getString(R.string.DRAFT_PLAYBACK))
                PhaseType.COMMUNITY_CHECK-> saveLog(context.getString(R.string.COMMENT_PLAYBACK))
                else -> {}
            }
        }

        fun onDeleteClick(name: String) {
            filenames?.remove(name)
            deleteStoryFile(context, "$PROJECT_DIR/$name")
            if("$PROJECT_DIR/$name" == Workspace.activePhase.getChosenFilename()){
                if(filenames!!.size > 0)
                    Workspace.activePhase.setChosenFilename("$PROJECT_DIR/" + filenames?.last())
                else{
                    Workspace.activePhase.setChosenFilename("")
                }
            }
            createRecordingList()
        }

        fun onRenameClick(name: String, newName: String): AudioFiles.RenameCode {
            lastOldName = name
            val tempName = newName + AUDIO_EXT
            lastNewName = tempName
            return when(renameStoryFile(context,"$PROJECT_DIR/$name",tempName)){
                true -> AudioFiles.RenameCode.SUCCESS
                false -> AudioFiles.RenameCode.ERROR_UNDEFINED
            }
        }

        fun onRenameSuccess() {
            val index = filenames?.indexOf("$PROJECT_DIR/$lastOldName")
            if (index != null) {
                filenames?.removeAt(index)
                filenames?.add(index,"$PROJECT_DIR/$lastNewName")
            }
            onRowClick(lastNewName.toString())
            createRecordingList()
        }
    }
}

