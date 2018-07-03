package org.sil.storyproducer.controller.draft

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag

import org.sil.storyproducer.model.Workspace

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class DraftFrag : MultiRecordFrag() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        super.onCreateView(inflater, container, savedInstanceState)
        setScriptureText(rootView!!.findViewById<View>(R.id.fragment_mr_scripture_text) as TextView)
        setReferenceText(rootView!!.findViewById<View>(R.id.fragment_mr_reference_text) as TextView)
        return rootView
    }

    /**
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    private fun setScriptureText(textView: TextView) {
        textView.text = slide.draftText
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
        textView.setText(R.string.draft_bible_story)
    }
}
