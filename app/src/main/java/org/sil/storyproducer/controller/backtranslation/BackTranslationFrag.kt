package org.sil.storyproducer.controller.backtranslation

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.support.graphics.drawable.VectorDrawableCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.AuthFailureError
import com.android.volley.NetworkError
import com.android.volley.NoConnectionError
import org.apache.commons.io.IOUtils
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.controller.remote.RemoteCheckFrag
import org.sil.storyproducer.controller.remote.sendSlideSpecificRequest
import org.sil.storyproducer.model.UploadState
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.Network.paramStringRequest
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import java.util.HashMap

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class BackTranslationFrag : MultiRecordFrag() {

    private lateinit var greenCheckmark: VectorDrawableCompat
    private lateinit var grayCheckmark: VectorDrawableCompat
    private lateinit var yellowCheckmark: VectorDrawableCompat
    private lateinit var uploadButton: ImageButton
    private lateinit var imageView: ImageView
    private lateinit var transcriptEditText: EditText
    private lateinit var sendTranscriptButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        rootView = inflater.inflate(R.layout.fragment_backtranslation, container, false)

        imageView = rootView!!.findViewById<View>(R.id.fragment_image_view) as ImageView
        uploadButton = rootView!!.findViewById(R.id.upload_audio_botton)
        transcriptEditText = rootView!!.findViewById(R.id.transcript_edit_text)
        sendTranscriptButton = rootView!!.findViewById(R.id.send_transcript_button)

        setPic(imageView)

        greenCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null)!!
        grayCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_gray, null)!!
        yellowCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_yellow, null)!!
        uploadButton.background = when (Workspace.activeSlide!!.backTranslationUploadState) {
            UploadState.UPLOADED -> greenCheckmark
            UploadState.NOT_UPLOADED -> grayCheckmark
            UploadState.UPLOADING -> yellowCheckmark
        }

        sendTranscriptButton.setOnClickListener {
            val prefs = activity!!.getSharedPreferences(RemoteCheckFrag.R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
            val prefsEditor = prefs.edit()
            val js = HashMap<String, String>()
            val message = transcriptEditText.text.toString()
            js["IsTranscript"] = 1.toString()
            sendSlideSpecificRequest(context!!, slideNum, getString(R.string.url_send_message), message, {
                prefsEditor.putString(slideNum.toString() + RemoteCheckFrag.TO_SEND_MESSAGE, "")
                prefsEditor.apply()
                transcriptEditText.setText("")
//                successToast!!.show()
            }, {
                //Save the message to send next time
                prefsEditor.putString(slideNum.toString() + RemoteCheckFrag.TO_SEND_MESSAGE, message)
                prefsEditor.apply()

//                if (it is NoConnectionError || it is NetworkError || it is AuthFailureError) {
//                    noConnection!!.show()
//                } else {
//                    unknownError!!.show()
//                }

            }, js)
        }

        uploadButton.setOnClickListener {
            when (Workspace.activeSlide!!.backTranslationUploadState) {
                UploadState.UPLOADED -> Toast.makeText(context, "Selected recording already uploaded", Toast.LENGTH_SHORT).show()
                UploadState.NOT_UPLOADED -> {
                    val audioRecording = Workspace.activeSlide!!.backTranslationRecordings.selectedFile
                    if (audioRecording != null) {
                        Workspace.activeSlide!!.backTranslationUploadState = UploadState.UPLOADING
                        uploadButton.background = yellowCheckmark
                        Toast.makeText(context!!, "Uploading audio", Toast.LENGTH_SHORT).show()
                        val input = getStoryChildInputStream(context!!, audioRecording.fileName)
                        val audioBytes = IOUtils.toByteArray(input)
                        val byteString = android.util.Base64.encodeToString(audioBytes, android.util.Base64.DEFAULT)
                        sendSlideSpecificRequest(context!!, slideNum, getString(R.string.url_upload_audio), byteString, {
                            Toast.makeText(context, R.string.audio_Sent, Toast.LENGTH_SHORT).show()
                            Workspace.activeSlide!!.backTranslationUploadState = UploadState.UPLOADED
                            uploadButton.background = greenCheckmark
                        }, {
                            Toast.makeText(context, R.string.audio_Send_Failed, Toast.LENGTH_SHORT).show()
                            Workspace.activeSlide!!.backTranslationUploadState = UploadState.NOT_UPLOADED
                            uploadButton.background = grayCheckmark
                        })
                    } else {
                        Toast.makeText(context!!, "No recording found", Toast.LENGTH_SHORT).show()
                    }
                }
                UploadState.UPLOADING -> {
                    uploadButton.background = yellowCheckmark
                    Toast.makeText(context!!, "Upload already in progress", Toast.LENGTH_SHORT).show()
                }
            }
        }

        uploadButton.setOnLongClickListener {
            when (Workspace.activeSlide!!.backTranslationUploadState) {
                UploadState.UPLOADING -> {
                    Workspace.activeSlide!!.backTranslationUploadState = UploadState.NOT_UPLOADED
                    Toast.makeText(context!!, "Cancelling upload", Toast.LENGTH_SHORT).show()
                    uploadButton.background = grayCheckmark
                }
                UploadState.UPLOADED -> {
                    Workspace.activeSlide!!.backTranslationUploadState = UploadState.NOT_UPLOADED
                    Toast.makeText(context!!, "Ignoring previous upload", Toast.LENGTH_SHORT).show()
                    uploadButton.background = grayCheckmark
                }
                UploadState.NOT_UPLOADED -> Toast.makeText(context!!, "There have been no uploads yet", Toast.LENGTH_SHORT).show()
            }
            true
        }

        setToolbar()
        return rootView
    }

}
