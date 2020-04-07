package org.sil.storyproducer.controller.remote

import android.content.Context
import org.sil.storyproducer.model.Recording
import org.sil.storyproducer.model.UploadState
import android.os.Bundle
import android.provider.Settings
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.VolleyError
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.NetworkResponse;
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import org.json.JSONException
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.Network.paramStringRequest
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.toolbar.PlayBackRecordingToolbar
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class UploadAudioButtonManager(
    val context: Context, 
    val uploadAudioButton: ImageButton, 
    val getUploadState: () -> UploadState, 
    val setUploadState: (UploadState) -> Unit,
    val getAudioRecording: () -> Recording?,
    val slideNumber: Int?
) {

    private val notUploadedIcon: VectorDrawableCompat
    private val uploadingIcon: VectorDrawableCompat
    private val uploadedIcon: VectorDrawableCompat

    init {
        notUploadedIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_cloud_upload_24dp, null)!!
        uploadingIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_cloud_uploading_24dp, null)!!
        uploadedIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_cloud_done_24dp, null)!!

        refreshBackground()

        uploadAudioButton.setOnClickListener {
            when (getUploadState()) {
                UploadState.UPLOADED -> Toast.makeText(context, "Selected recording already uploaded", Toast.LENGTH_SHORT).show()
                UploadState.NOT_UPLOADED -> {
                    val audioRecording = getAudioRecording()
                    if (audioRecording != null) {
                        setUploadState(UploadState.UPLOADING)
                        uploadAudioButton.background = uploadingIcon
                        Toast.makeText(context, "Uploading audio", Toast.LENGTH_SHORT).show()
                        val input = getStoryChildInputStream(context, audioRecording.fileName)
                        val audioBytes = IOUtils.toByteArray(input)
                        val byteString = android.util.Base64.encodeToString(audioBytes, android.util.Base64.DEFAULT)

                        val js = HashMap<String, String>()
                        // Default to 0 as the slide number to send to the server
                        // because the server will ignore it if the request has
                        // IsWholeStory set to true.
                        var finalSlideNumber = 0
                        // Null slideNumber indicates that this is a a whole story
                        // upload button.
                        if (slideNumber == null) {
                            js["IsWholeStory"] = "true"
                        } else {
                            finalSlideNumber = slideNumber
                        }
                        sendSlideSpecificRequest(context, finalSlideNumber, context.getString(R.string.url_upload_audio), byteString, {
                            Toast.makeText(context, "Audio File Sent Successfuly", Toast.LENGTH_SHORT).show()
                            setUploadState(UploadState.UPLOADED)
                            uploadAudioButton.background = uploadedIcon
                        }, {
                            Toast.makeText(context, "Audio upload failed", Toast.LENGTH_SHORT).show()
                            setUploadState(UploadState.NOT_UPLOADED)
                            uploadAudioButton.background = notUploadedIcon
                        }, js)
                    } else {
                        Toast.makeText(context, "No recording found", Toast.LENGTH_SHORT).show()
                    }
                }
                UploadState.UPLOADING -> {
                    uploadAudioButton.background = uploadingIcon
                    Toast.makeText(context, "Upload already in progress", Toast.LENGTH_SHORT).show()
                }
            }
        }

        uploadAudioButton.setOnLongClickListener {
            when (getUploadState()) {
                UploadState.UPLOADING -> {
                    setUploadState(UploadState.NOT_UPLOADED)
                    Toast.makeText(context, "Cancelling upload", Toast.LENGTH_SHORT).show()
                    uploadAudioButton.background = notUploadedIcon
                }
                UploadState.UPLOADED -> {
                    setUploadState(UploadState.NOT_UPLOADED)
                    Toast.makeText(context, "Ignoring previous upload", Toast.LENGTH_SHORT).show()
                    uploadAudioButton.background = notUploadedIcon
                }
                UploadState.NOT_UPLOADED -> Toast.makeText(context, "There have been no uploads yet", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    fun refreshBackground() {
        uploadAudioButton.background = when (getUploadState()) {
            UploadState.UPLOADED -> uploadedIcon
            UploadState.NOT_UPLOADED -> notUploadedIcon
            UploadState.UPLOADING -> uploadingIcon
        }
    }
}
