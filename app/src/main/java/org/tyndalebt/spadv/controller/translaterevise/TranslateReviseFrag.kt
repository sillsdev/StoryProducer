package org.tyndalebt.spadv.controller.translaterevise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.controller.MultiRecordFrag

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class TranslateReviseFrag : MultiRecordFrag() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setScriptureText(rootView!!.findViewById(R.id.fragment_scripture_text))
        setReferenceText(rootView!!.findViewById(R.id.fragment_reference_text))

        return rootView
    }

}
