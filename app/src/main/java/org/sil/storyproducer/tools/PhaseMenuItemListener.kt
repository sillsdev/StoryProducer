package org.sil.storyproducer.tools

import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import org.sil.storyproducer.controller.phase.PhaseBaseActivity

import org.sil.storyproducer.model.Workspace

class PhaseMenuItemListener(private val pbactivity: PhaseBaseActivity) : OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>, view: View,
                                pos: Int, id: Long) {
        pbactivity.jumpToPhase(Workspace.phases[pos])
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }

}
