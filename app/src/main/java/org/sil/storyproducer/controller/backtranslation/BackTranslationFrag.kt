package org.sil.storyproducer.controller.backtranslation

import android.content.Context
import android.os.Bundle
import android.support.graphics.drawable.VectorDrawableCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.sql.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.apache.commons.io.IOUtils
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.remote.RemoteCheckFrag
import org.sil.storyproducer.controller.remote.sendSlideSpecificRequest
import org.sil.storyproducer.controller.remote.UploadAudioButtonManager
import org.sil.storyproducer.model.UploadState
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.messaging.Approval
import org.sil.storyproducer.model.messaging.Message
import java.util.*

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class BackTranslationFrag : MultiRecordFrag(), CoroutineScope by MainScope() {

    private lateinit var greenCheckmark: VectorDrawableCompat
    private lateinit var grayCheckmark: VectorDrawableCompat
    private lateinit var yellowCheckmark: VectorDrawableCompat
    private lateinit var slideApprovedIndicator: ImageButton
    private lateinit var uploadAudioButtonManager: UploadAudioButtonManager
    private var approvalReceiveChannel: ReceiveChannel<Approval>? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        rootView = inflater.inflate(R.layout.fragment_backtranslation, container, false)
        initializeViews()


        greenCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null)!!
        grayCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_gray, null)!!
        yellowCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_yellow, null)!!

        val sendTranscriptButton: Button = rootView.findViewById(R.id.send_transcript_button)
        val transcriptEditText: EditText = rootView.findViewById(R.id.transcript_edit_text)
        sendTranscriptButton.setOnClickListener {
            val text = transcriptEditText.text.toString()
            if (text.length > 0) {
                val storyId = Workspace.activeStory.remoteId ?: 0
                val message = Message(slideNum, storyId, false, true, Timestamp(0), text)
                launch {
                    Workspace.toSendMessageChannel.send(message)
                }
            }
        }

        slideApprovedIndicator = rootView.findViewById(R.id.slide_approved_indicator)
        slideApprovedIndicator.background = if (slide.isApproved) {
            greenCheckmark
        } else {
            grayCheckmark
        }

        uploadAudioButtonManager = UploadAudioButtonManager(
            context!!,
            rootView.findViewById(R.id.upload_audio_botton),
            { slide.backTranslationUploadState },
            { slide.backTranslationUploadState = it },
            { slide.backTranslationRecordings.selectedFile }, 
            slideNum)

        setToolbar()
        return rootView
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (::uploadAudioButtonManager.isInitialized) {
            uploadAudioButtonManager.refreshBackground()
        }

        approvalReceiveChannel = if (isVisibleToUser) {
            val sub = Workspace.approvalChannel.openSubscription()
            launch(Dispatchers.Main) {
                for (approval in sub) {
                    Log.e("@pwhite", "got approval $approval, remoteId = ${Workspace.activeStory.remoteId}")
                    if (approval.slideNumber == slideNum && approval.storyId == Workspace.activeStory.remoteId) {
                        slideApprovedIndicator.background = if (approval.approvalStatus) { greenCheckmark } else { grayCheckmark }
                    }
                }
            }
            sub
        } else {
            approvalReceiveChannel?.cancel()
            null
        }
    }


}
