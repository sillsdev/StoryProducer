package org.sil.storyproducer.controller

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import java.io.File

/**
 * The fragment that loads SlidePhaseFrag as well as the camera and edit buttons that overlays it.
 */
class MultiRecordFrag : Fragment(), SlidePhaseFrag.PlaybackListener {
    private var tempPicFile: File? = null
    private lateinit var imageFab: ImageView
    private lateinit var editFab: ImageView
    private var slideNum: Int = 0
    private val slidePhaseFrag = SlidePhaseFrag()
    private lateinit var playbackListener: PlaybackListener

    interface PlaybackListener {
        fun onStoppedPlayback()
        fun onStartedPlayback()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(parentFragment is PlaybackListener){
            playbackListener = parentFragment as PlaybackListener
        }
        else{
            throw ClassCastException("$parentFragment does not implement MultiRecordFrag.PlaybackListener")
        }

    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_multi_record_layout, container, false)

        slideNum = arguments?.getInt(SlidePhaseFrag.SLIDE_NUM)!!
        setSlide()

        imageFab = rootView.findViewById(R.id.insert_image_view)
        editFab = rootView.findViewById(R.id.edit_text_view)

        setupCameraAndEditButton()

        return rootView
    }

    /**
     * Setup camera button for updating background image
     * and edit button for renaming text and local credits
     */
    private fun setupCameraAndEditButton() {
        // display the image selection button, if on the title slide
        if(Workspace.activeStory.slides[slideNum].slideType in
        arrayOf(SlideType.FRONTCOVER,SlideType.LOCALSONG))
        {
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

        // display the image selection button, if on the title slide
        if(Workspace.activeStory.slides[slideNum].slideType in
                arrayOf(SlideType.FRONTCOVER,SlideType.LOCALCREDITS))
        {

            editFab.visibility = android.view.View.VISIBLE
            editFab.setOnClickListener {
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
                            (childFragmentManager.findFragmentById(R.id.slide_phase) as SlidePhaseFrag).setPic()
                        }.create()

                dialog.show()
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
            (childFragmentManager.findFragmentById(R.id.slide_phase) as SlidePhaseFrag).setPic()
        }
    }

    private fun setSlide(){
        val bundle = Bundle()
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, slideNum)
        slidePhaseFrag.arguments = bundle
        childFragmentManager.beginTransaction().add(R.id.slide_phase, slidePhaseFrag).commit()
    }

    override fun onStoppedPlayback() {
        playbackListener.onStoppedPlayback()
    }
    override fun onStartedPlayback() {
        playbackListener.onStartedPlayback()
    }

    fun stopPlayback(){
        slidePhaseFrag.stopPlayBackAndRecording()
    }

    companion object {
        private const val ACTIVITY_SELECT_IMAGE = 53
    }
}
