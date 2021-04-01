package org.sil.storyproducer.controller

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import org.sil.storyproducer.R
import org.sil.storyproducer.model.*

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class StoryPhaseFrag : Fragment() {
    protected var rootView: View? = null

    protected var storyPlayer : StoryPlayerFrag? = null

    protected var slideNum: Int = 0 // gets overwritten
    protected lateinit var slide: Slide

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        slideNum = this.arguments!!.getInt(SLIDE_NUM)
        slide = Workspace.activeStory.slides[slideNum]
        setHasOptionsMenu(true)

        createStoryPlayer()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(R.layout.fragment_slide, container, false)

        return rootView
    }

    protected fun createStoryPlayer() {
        storyPlayer = if(Workspace.activeStory.isFilmStory) FilmStoryPlayerFrag() else ImageStoryPlayerFrag()
        storyPlayer?.startSlide = slideNum
        storyPlayer?.slideRange = 1
        storyPlayer?.phaseType = Workspace.activePhase.phaseType
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val item = menu.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_mic_white_48dp)
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        if(storyPlayer != null) {
            storyPlayer?.stop()
        }
    }

//    /**
//     * This function serves to handle page changes and stops the audio streams from
//     * continuing.
//     */
//
//    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
//        super.setUserVisibleHint(isVisibleToUser)
//        referenceAudioPlayer.stopAudio()
//        referencePlayButton?.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
//    }

    /**
     * Sets the main text of the layout.
     *
     * @param slideImage    The ImageView that will contain the picture.
     */
    protected fun setScriptureText(textView: TextView) {
        textView.text = slide.content
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
        // There is no reference text.
        textView.text = ""
    }

    abstract fun stopPlayBack()

    open fun onStartedSlidePlayBack() {}
}
