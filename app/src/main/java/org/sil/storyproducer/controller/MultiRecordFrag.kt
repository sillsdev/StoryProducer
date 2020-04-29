package org.sil.storyproducer.controller

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.model.PROJECT_DIR
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.PHASE_TYPE
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import org.sil.storyproducer.tools.toolbar.MultiRecordRecordingToolbar
import org.sil.storyproducer.tools.toolbar.PlayBackRecordingToolbar
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import java.io.File

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class MultiRecordFrag : SlidePhaseFrag(), PlayBackRecordingToolbar.ToolbarMediaListener {
    protected open var recordingToolbar: RecordingToolbar = MultiRecordRecordingToolbar()

    private var tempPicFile: File? = null

    override fun initializeViews() {
        super.initializeViews()
        if (Workspace.activeStory.slides[slideNumber].slideType != SlideType.LOCALCREDITS) {
            setToolbar()
        }
        setupCameraAndEditButton()
    }

    /**
     * Setup camera button for updating background image
     * and edit button for renaming text and local credits
     */
    fun setupCameraAndEditButton() {
        // display the image selection button, if on the title slide
        if(Workspace.activeStory.slides[slideNumber].slideType in arrayOf(SlideType.FRONTCOVER,SlideType.LOCALSONG) 
           && phaseType != PhaseType.BACKT)
        {
            val imageFab: ImageView = rootView.findViewById<View>(R.id.insert_image_view) as ImageView
            imageFab.visibility = View.VISIBLE
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

            //for these, use the edit text button instead of the text in the lower half.
            //In the phases that these are not there, do nothing.
            val editBox = rootView.findViewById<View>(R.id.fragment_dramatization_edit_text) as EditText?
            editBox?.visibility = View.INVISIBLE

            val editFab = rootView.findViewById<View>(R.id.edit_text_view) as ImageView?
            editFab?.visibility = View.VISIBLE
            editFab?.setOnClickListener {
                val editText = EditText(context)
                editText.id = R.id.edit_text_input

                // Programmatically set layout properties for edit text field
                val params = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
                // Apply layout properties
                editText.layoutParams = params
                editText.minLines = 5
                editText.text.insert(0,Workspace.activeSlide!!.translatedContent)

                val dialog = AlertDialog.Builder(context)
                        .setTitle(getString(R.string.enter_text))
                        .setView(editText)
                        .setNegativeButton(getString(R.string.cancel), null)
                        .setPositiveButton(getString(R.string.save)) { _, _ ->
                            Workspace.activeSlide!!.translatedContent = editText.text.toString()
                            setPic()
                        }.create()

                dialog.show()
            }
        }
    }


    /**
     * Change the picture behind the screen.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            if (resultCode == Activity.RESULT_OK && requestCode == ACTIVITY_SELECT_IMAGE) {
                //copy image into workspace
                var uri = data?.data
                if (uri == null) uri = FileProvider.getUriForFile(context!!, "${BuildConfig.APPLICATION_ID}.fileprovider", tempPicFile!!)   //it was a camera intent
                Workspace.activeStory.slides[slideNumber].imageFile = "$PROJECT_DIR/${slideNumber}_Local.png"
                copyToWorkspacePath(context!!, uri!!,
                        "${Workspace.activeStory.title}/${Workspace.activeStory.slides[slideNumber].imageFile}")
                tempPicFile?.delete()
                setPic()
            }
        }catch (e:Exception){
            Toast.makeText(context,"Error",Toast.LENGTH_SHORT).show()
            Crashlytics.logException(e)
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
                if (Workspace.activeStory.slides[slideNumber].slideType != SlideType.LOCALCREDITS) {
                    recordingToolbar.stopToolbarMedia()
                }
            }
        }
    }

    protected open fun setToolbar() {
        val bundle = Bundle()
        bundle.putInt(SLIDE_NUM, slideNumber)
        bundle.putInt(PHASE_TYPE, phaseType.ordinal)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
    }

    override fun onStartedToolbarMedia() {
        super.onStartedToolbarMedia()

        stopSlidePlayBack()
    }

    override fun onStartedSlidePlayBack() {
        super.onStartedSlidePlayBack()

        recordingToolbar.stopToolbarMedia()
    }

    companion object {
        private const val ACTIVITY_SELECT_IMAGE = 53
    }
}
