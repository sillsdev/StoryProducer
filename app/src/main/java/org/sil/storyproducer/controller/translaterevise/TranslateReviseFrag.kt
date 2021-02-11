package org.sil.storyproducer.controller.translaterevise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class TranslateReviseFrag : MultiRecordFrag() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)?.apply {
            findViewById<TextView>(R.id.fragment_reference_text).text = viewModel.scriptureReference
            findViewById<TextView>(R.id.fragment_scripture_text).text = viewModel.scriptureText
        }
    }

}
