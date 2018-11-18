package org.sil.storyproducer.controller

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.keyterm.stringToSpannableString
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.file.getStoryImage
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class SlidePhaseFrag : Fragment() {
    protected var rootView: View? = null
    //protected var rootViewToolbar: View? = null

    //protected var referenceAudioPlayer: AudioPlayer = AudioPlayer()
    //protected var referncePlayButton: ImageButton? = null

    protected var slideNum: Int = 0 //gets overwritten
    protected var slide: Slide = Workspace.activeSlide!! //this is a placeholder that gets overwritten in onCreate.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slideNum = this.arguments!!.getInt(SLIDE_NUM)

        slide = Workspace.activeStory.slides[slideNum]
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(R.layout.fragment_slide, container, false)

        val arguments = Bundle()
        arguments.putBoolean("enablePlaybackButton", true)
        arguments.putBoolean("enableDeleteButton", false)
        arguments.putBoolean("enableMultiRecordButton", true)
        arguments.putBoolean("enableSendAudioButton", false)
        arguments.putInt("slideNum", slideNum)

        val toolbarFrag = childFragmentManager.findFragmentById(R.id.bottom_toolbar) as ToolbarFrag
        toolbarFrag.arguments = arguments
        toolbarFrag.setupToolbarButtons()

        //setUiColors()
        //setPic(rootView!!.findViewById<ImageView>(R.id.fragment_image_view))

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        val item = menu?.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item?.setIcon(R.drawable.ic_mic_white_48dp)
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
//    protected open fun setUiColors() {
//        if (slideNum == 0) {
//            var rl = rootView!!.findViewById<ViewGroup>(R.id.fragment_envelope)
//            rl?.setBackgroundColor(ContextCompat.getColor(context!!, R.color.primaryDark))
//            rl = rootView!!.findViewById(R.id.fragment_text_envelope)
//            rl?.setBackgroundColor(ContextCompat.getColor(context!!, R.color.primaryDark))
//
//            var tv = rootView!!.findViewById<TextView>(R.id.fragment_scripture_text)
//            tv?.setBackgroundColor(ContextCompat.getColor(context!!, R.color.primaryDark))
//            tv = rootView!!.findViewById(R.id.fragment_reference_text)
//            tv?.setBackgroundColor(ContextCompat.getColor(context!!, R.color.primaryDark))
//        }
//    }

    /**
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    protected fun setScriptureText(textView: TextView) {
        val words = slide.content.split(" ")
        textView.text = stringToSpannableString(words, this.context)
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
    protected fun setReferenceText(textView: TextView) {
        val titleNamePriority = arrayOf(slide.reference, slide.subtitle, slide.title)

        for (title in titleNamePriority) {
            if (title != "") {
                textView.text = title
                return
            }
        }
        //There is no reference text.
        textView.text = ""
    }

    companion object {
        const val SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG"

    }
}
