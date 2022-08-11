package org.tyndalebt.spadv.controller.remote

import android.app.Activity
import android.os.Bundle
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import androidx.core.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.sql.Timestamp
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.controller.MultiRecordFrag
import org.tyndalebt.spadv.model.UploadState
import org.tyndalebt.spadv.model.Workspace
import org.tyndalebt.spadv.model.messaging.Approval
import org.tyndalebt.spadv.tools.toolbar.RecordingToolbar
import org.tyndalebt.spadv.controller.dramatization.DramatizationRecordingToolbar
import org.tyndalebt.spadv.model.messaging.MessageROCC
import java.util.*
//Code copied from the ROCC version of the app

class BackTranslationFrag : MultiRecordFrag(), CoroutineScope by MainScope() {

    override var recordingToolbar: RecordingToolbar = DramatizationRecordingToolbar() //This toolbar is specific to the slide-tellback

    private lateinit var uploadAudioButtonManager: UploadAudioButtonManager
    private lateinit var approvalIndicatorManager: ApprovalIndicatorManager
    private var approvalReceiveChannel: ReceiveChannel<Approval>? = null
    private var skipChanged: Boolean = false

//Check other files for examples on how to use the old version of onCreateView
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        rootView = inflater.inflate(R.layout.fragment_back_translation, container, false)
//apparently this function might not be needed        onCreateView() // Also check the comment on why CU used initializeViews() instead
        setPic(rootView?.findViewById<View>(R.id.fragment_image_view) as ImageView) //uncertain if this line is correct


        val whiteSendIcon = VectorDrawableCompat.create(resources, R.drawable.ic_send_white_24dp, null)!!
        val blackSendIcon = VectorDrawableCompat.create(resources, R.drawable.ic_send_black_24dp, null)!!
        val sendTranscriptButton: Button = rootView.findViewById(R.id.send_transcript_button)

        val transcriptEditText: EditText = rootView.findViewById(R.id.transcript_edit_text)
        val transcriptString = slide.backTranslationTranscript
        if (transcriptString != null && transcriptString != "") {
            skipChanged = true // Not changed by user, don't change colors, etc.
            slide.backTranslationTranscriptModified = false
            slide.backTranslationTranscriptPresent = true
            transcriptEditText.setText(transcriptString)
        }
        if (!slide.backTranslationTranscriptPresent) {
            transcriptEditText.setTextColor(ContextCompat.getColor(context!!, R.color.transcript_dirty));
            sendTranscriptButton.background = whiteSendIcon
        } else {
            transcriptEditText.setTextColor(ContextCompat.getColor(context!!, R.color.transcript_sent));
            sendTranscriptButton.background = blackSendIcon
        }

        sendTranscriptButton.setOnClickListener {
            val text = transcriptEditText.text.toString()
            if (text.isNotEmpty() && slide.backTranslationTranscriptModified) {
                val storyId = Workspace.activeStory.remoteId ?: 0
                val message = MessageROCC(slideNum, storyId, false, true, Timestamp(0), text)
                launch {
                    Workspace.toSendMessageChannel.send(message)
                }

                // Close the keyboard
                val imm = context!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(rootView.windowToken, 0)
                rootView.requestFocus()

                transcriptEditText.setTextColor(ContextCompat.getColor(context!!, R.color.transcript_sent));
                sendTranscriptButton.background = blackSendIcon;
                slide.backTranslationTranscriptModified = false
            }
        }

        transcriptEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                if (!skipChanged) {
                    transcriptEditText.setTextColor(ContextCompat.getColor(context!!, R.color.transcript_dirty));
                    slide.backTranslationTranscript = text.toString()
                    sendTranscriptButton.background = whiteSendIcon;
                    slide.backTranslationTranscriptModified = true
                }
                else {
                    val text = transcriptEditText.text.toString()
                    if (text.isNotEmpty()) {
                        // Text exists, we are setting it, so make it sent color
                        transcriptEditText.setTextColor(ContextCompat.getColor(context!!, R.color.transcript_sent));
                        sendTranscriptButton.background = blackSendIcon
                        skipChanged = false
                    }
                }
            }
        })

        uploadAudioButtonManager = UploadAudioButtonManager(
            context!!,
            rootView.findViewById(R.id.upload_audio_botton),
            { slide.backTranslationUploadState },
            { slide.backTranslationUploadState = it },
            { org.tyndalebt.spadv.tools.file.getChosenFilename(slideNum) },
            slideNum)

        approvalIndicatorManager = ApprovalIndicatorManager(
            context!!,
            this,
            rootView.findViewById(R.id.slide_approved_indicator),
            slide,
            slideNum)

        setToolbar() //<---- This brings up the audio recording bar

        return rootView
    }

    override fun onStart() {
        super.onStart()

        approvalIndicatorManager.start()
    }

    override fun onStop() {
        super.onStop()

        approvalIndicatorManager.stop()
    }

    override fun onStoppedToolbarRecording() {
        super.onStoppedToolbarRecording()
        slide.backTranslationUploadState = UploadState.NOT_UPLOADED
        uploadAudioButtonManager.refreshBackground()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (::uploadAudioButtonManager.isInitialized) {
            uploadAudioButtonManager.refreshBackground()
        }
    }

}