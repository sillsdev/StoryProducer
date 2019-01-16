package org.sil.storyproducer.controller.draft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.ACTION_IMAGE_CAPTURE
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.sil.storyproducer.BuildConfig.APPLICATION_ID

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import java.io.File

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class DraftFrag : MultiRecordFrag() {

    private var tempPicFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        super.onCreateView(inflater, container, savedInstanceState)
        setScriptureText(rootView!!.findViewById(R.id.fragment_scripture_text))
        setReferenceText(rootView!!.findViewById(R.id.fragment_reference_text))

        // display the image selection button, if on the title slide
        if(Workspace.activeStory.slides[slideNum].slideType in
                arrayOf(SlideType.FRONTCOVER,SlideType.LOCALSONG)){
            val imageFab: FloatingActionButton = rootView!!.findViewById<View>(R.id.insert_image_fab) as FloatingActionButton
            imageFab.show()
            imageFab.setOnClickListener {
                val chooser = Intent(Intent.ACTION_CHOOSER)
                chooser.putExtra(Intent.EXTRA_TITLE,"Select From:")

                val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
                galleryIntent.type = "image/*"
                chooser.putExtra(Intent.EXTRA_INTENT,galleryIntent)

                val cameraIntent = Intent(ACTION_IMAGE_CAPTURE)
                cameraIntent.resolveActivity(activity!!.packageManager).also {
                    tempPicFile = File.createTempFile("temp",".jpg",activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(activity!!, "$APPLICATION_ID.fileprovider",tempPicFile!!))
                }
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,arrayOf(cameraIntent))

                startActivityForResult(chooser, ACTIVITY_SELECT_IMAGE)
            }
        }

        return rootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == ACTIVITY_SELECT_IMAGE) {
            //copy image into workspace
            var uri = data?.data
            if(uri == null) uri = FileProvider.getUriForFile(context!!,"$APPLICATION_ID.fileprovider",tempPicFile!!)   //it was a camera intent
            Workspace.activeStory.slides[slideNum].imageFile = "${slideNum}_Local.png"
            copyToWorkspacePath(context!!,uri!!,
                    "${Workspace.activeStory.title}/${Workspace.activeStory.slides[slideNum].imageFile}")
            tempPicFile?.delete()
            setPic(rootView!!.findViewById(R.id.fragment_image_view) as ImageView)
        }
    }

    private val ACTIVITY_SELECT_IMAGE = 53

}
