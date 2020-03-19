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
    private lateinit var uploadAudioButton: ImageButton
    private lateinit var slideApprovedIndicator: ImageButton
    private lateinit var transcriptEditText: EditText
    private lateinit var sendTranscriptButton: Button
    private var approvalReceiveChannel: ReceiveChannel<Approval>? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        rootView = inflater.inflate(R.layout.fragment_backtranslation, container, false)
        initializeViews()

        uploadAudioButton = rootView.findViewById(R.id.upload_audio_botton)
        slideApprovedIndicator = rootView.findViewById(R.id.slide_approved_indicator)
        transcriptEditText = rootView.findViewById(R.id.transcript_edit_text)
        sendTranscriptButton = rootView.findViewById(R.id.send_transcript_button)

        greenCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null)!!
        grayCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_gray, null)!!
        yellowCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_yellow, null)!!
        uploadAudioButton.background = when (slide.backTranslationUploadState) {
            UploadState.UPLOADED -> greenCheckmark
            UploadState.NOT_UPLOADED -> grayCheckmark
            UploadState.UPLOADING -> yellowCheckmark
        }

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

        slideApprovedIndicator.background = if (slide.isApproved) {
            greenCheckmark
        } else {
            grayCheckmark
        }

        uploadAudioButton.setOnClickListener {
            when (slide.backTranslationUploadState) {
                UploadState.UPLOADED -> Toast.makeText(context, "Selected recording already uploaded", Toast.LENGTH_SHORT).show()
                UploadState.NOT_UPLOADED -> {
                    val audioRecording = slide.backTranslationRecordings.selectedFile
                    if (audioRecording != null) {
                        slide.backTranslationUploadState = UploadState.UPLOADING
                        uploadAudioButton.background = yellowCheckmark
                        Toast.makeText(context!!, "Uploading audio", Toast.LENGTH_SHORT).show()
                        val input = getStoryChildInputStream(context!!, audioRecording.fileName)
                        val audioBytes = IOUtils.toByteArray(input)
                        val byteString = android.util.Base64.encodeToString(audioBytes, android.util.Base64.DEFAULT)
                        sendSlideSpecificRequest(context!!, slideNum, getString(R.string.url_upload_audio), byteString, {
                            Toast.makeText(context, R.string.audio_Sent, Toast.LENGTH_SHORT).show()
                            slide.backTranslationUploadState = UploadState.UPLOADED
                            uploadAudioButton.background = greenCheckmark
                        }, {
                            //Toast.makeText(context, R.string.audio_Send_Failed, Toast.LENGTH_SHORT).show()
                            slide.backTranslationUploadState = UploadState.NOT_UPLOADED
                            uploadAudioButton.background = grayCheckmark
                        })
                    } else {
                        Toast.makeText(context!!, "No recording found", Toast.LENGTH_SHORT).show()
                    }
                }
                UploadState.UPLOADING -> {
                    uploadAudioButton.background = yellowCheckmark
                    Toast.makeText(context!!, "Upload already in progress", Toast.LENGTH_SHORT).show()
                }
            }
        }

        uploadAudioButton.setOnLongClickListener {
            when (slide.backTranslationUploadState) {
                UploadState.UPLOADING -> {
                    slide.backTranslationUploadState = UploadState.NOT_UPLOADED
                    Toast.makeText(context!!, "Cancelling upload", Toast.LENGTH_SHORT).show()
                    uploadAudioButton.background = grayCheckmark
                }
                UploadState.UPLOADED -> {
                    slide.backTranslationUploadState = UploadState.NOT_UPLOADED
                    Toast.makeText(context!!, "Ignoring previous upload", Toast.LENGTH_SHORT).show()
                    uploadAudioButton.background = grayCheckmark
                }
                UploadState.NOT_UPLOADED -> Toast.makeText(context!!, "There have been no uploads yet", Toast.LENGTH_SHORT).show()
            }
            true
        }

        setToolbar()
        return rootView
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

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
