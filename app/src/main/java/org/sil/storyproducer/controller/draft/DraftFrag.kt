package org.sil.storyproducer.controller.draft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.design.widget.FloatingActionButton
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.VIDEO_DIR
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import org.sil.storyproducer.tools.file.getChildInputStream
import org.sil.storyproducer.tools.file.getWorkspaceUri

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class DraftFrag : MultiRecordFrag() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        super.onCreateView(inflater, container, savedInstanceState)
        setScriptureText(rootView!!.findViewById<View>(R.id.fragment_scripture_text) as TextView)
        setReferenceText(rootView!!.findViewById<View>(R.id.fragment_reference_text) as TextView)

        // display the image selection button, if on the title slide
        if(Workspace.activeStory.slides[slideNum].slideType == SlideType.FRONTCOVER){
            val imageFab: FloatingActionButton = rootView!!.findViewById<View>(R.id.insert_image_fab) as FloatingActionButton
            imageFab.setImageResource(R.drawable.ic_insert_photo_white_24dp)
            imageFab.show()
            imageFab.setOnClickListener {
                val imageIntent = Intent(Intent.ACTION_PICK)
                imageIntent.type = "image/*"
                startActivityForResult(imageIntent, ACTIVITY_SELECT_IMAGE)
            }
        }

        return rootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == ACTIVITY_SELECT_IMAGE) {
            //copy image into workspace
            val uri = data!!.data ?: return
            Workspace.activeStory.slides[slideNum].imageFile = "TitlePicture.png"
            copyToWorkspacePath(context!!,uri,
                    "${Workspace.activeStory.title}/${Workspace.activeStory.slides[slideNum].imageFile}")
        }
    }

    private val ACTIVITY_SELECT_IMAGE = 53

    }
