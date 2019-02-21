package org.sil.storyproducer.controller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import org.sil.storyproducer.tools.toolbar.RecordingToolbar.RecordingListener
import java.io.File

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class MultiRecordFrag : SlidePhaseFrag(), RecordingListener {

    protected var recordingToolbar: RecordingToolbar = RecordingToolbar()
    private var tempPicFile: File? = null


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        if (Workspace.activeStory.slides[slideNum].slideType != SlideType.LOCALCREDITS) {
            setToolbar()
        }

        setupCameraButton()

        return rootView
    }

    /**
     * Setup camera button for updating background image
     */
    fun setupCameraButton() {
        // display the image selection button, if on the title slide
        if(Workspace.activeStory.slides[slideNum].slideType in
        arrayOf(SlideType.FRONTCOVER,SlideType.LOCALSONG))
        {
            val imageFab: ImageView = rootView!!.findViewById<View>(R.id.insert_image_view) as ImageView
            imageFab.visibility = android.view.View.VISIBLE
            imageFab.setOnClickListener {
                val chooser = Intent(Intent.ACTION_CHOOSER)
                chooser.putExtra(Intent.EXTRA_TITLE, "Select From:")

                val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
                galleryIntent.type = "image/*"
                chooser.putExtra(Intent.EXTRA_INTENT, galleryIntent)

                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraIntent.resolveActivity(activity!!.packageManager).also {
                    tempPicFile = File.createTempFile("temp", ".jpg", activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(activity!!, "${BuildConfig.APPLICATION_ID}.fileprovider", tempPicFile!!))
                }
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

                startActivityForResult(chooser, ACTIVITY_SELECT_IMAGE)
            }
        }
    }


    /**
     * Change the picture behind the screen.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == ACTIVITY_SELECT_IMAGE) {
            //copy image into workspace
            var uri = data?.data
            if(uri == null) uri = FileProvider.getUriForFile(context!!,"${BuildConfig.APPLICATION_ID}.fileprovider",tempPicFile!!)   //it was a camera intent
            Workspace.activeStory.slides[slideNum].imageFile = "${slideNum}_Local.png"
            copyToWorkspacePath(context!!,uri!!,
                    "${Workspace.activeStory.title}/${Workspace.activeStory.slides[slideNum].imageFile}")
            tempPicFile?.delete()
            setPic(rootView!!.findViewById(R.id.fragment_image_view) as ImageView)
        }
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is currently visible to user
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                recordingToolbar.stopToolbarMedia()
            }
        }
    }

    override fun stopPlayBackAndRecording() {
        super.stopPlayBackAndRecording()
        recordingToolbar.stopToolbarMedia()
    }

    override fun onStoppedRecordingOrPlayback(isRecording: Boolean) {
        //updatePlayBackPath()
    }

    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
        stopPlayBackAndRecording()
    }

    protected open fun setToolbar() {
        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(true,false,true,false))
        bundle.putInt("slideNum", slideNum)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
    }
    companion object {
        private const val ACTIVITY_SELECT_IMAGE = 53
    }
}
