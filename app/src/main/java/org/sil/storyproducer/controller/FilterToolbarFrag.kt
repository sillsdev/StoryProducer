package org.sil.storyproducer.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.R

class FilterToolbarFrag:androidx.fragment.app.Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val toolbar = inflater.inflate(R.layout.filter_toolbar, container, false)

        //TODO add button functionality

        return toolbar
    }
}