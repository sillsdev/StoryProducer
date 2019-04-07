package org.sil.storyproducer.controller

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.Workspace

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class ScriptureFrag : Fragment() {
    private lateinit var slide: Slide
    private var slideNum: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_scripture_text, container, false)

        slideNum = arguments?.getInt(SlidePhaseFrag.SLIDE_NUM)!!
        slide = Workspace.activeStory.slides[slideNum]

        setScriptureText(rootView.findViewById(R.id.fragment_scripture_text))
        setReferenceText(rootView.findViewById(R.id.fragment_reference_text))

        return rootView
    }

    /**
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    private fun setScriptureText(textView: TextView) {
        textView.text = slide.content
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
    private fun setReferenceText(textView: TextView) {
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
}
