package org.sil.storyproducer.controller.consultant

import android.content.Context
import android.os.Bundle
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.SlidePhaseFrag
import org.sil.storyproducer.controller.logging.LogListAdapter
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace

/**
 * The fragment for the Consultant check view. The consultant can check that the draft is ok
 */
abstract class ConsultantBaseFrag : Fragment() {

    var logDialog: AlertDialog? = null
    var greenCheckmark: VectorDrawableCompat? = null
    var grayCheckmark: VectorDrawableCompat? = null
    var slideNum: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        greenCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null)
        grayCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_gray, null)

        slideNum = arguments!!.getInt(SlidePhaseFrag.SLIDE_NUM)
    }

    /**
     * Sets on click listener for consultant to check off the slide and approve
     * @param button the check button
     */
    protected fun setCheckmarkButton(button: ImageButton) {
        if (Workspace.activeStory.slides[slideNum].isChecked) {
            button.background = greenCheckmark
        }
        else if(!Workspace.activeStory.slides[slideNum].isChecked){
            button.background = grayCheckmark
        }
    }

    /**
     * Set an on click listener to launch the interface to view the logs for that slide
     * @param button the logs button
     */
    protected fun setLogsButton(button: ImageButton) {
        button.background = VectorDrawableCompat.create(resources, R.drawable.ic_logs_blue, null)
        button.setOnClickListener {
            makeLogView()
            logDialog?.show()
        }
    }

    private fun makeLogView() {
        val alertDialog = android.support.v7.app.AlertDialog.Builder(context!!)
        val linf = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogLayout = linf.inflate(R.layout.activity_log_view, null)

        val listView = dialogLayout!!.findViewById<ListView>(R.id.log_list_view)
        val lla = LogListAdapter(context!!, slideNum)
        listView.adapter = lla
        val tb = dialogLayout.findViewById<Toolbar>(R.id.toolbar2)
        //Note that user-facing slide number is 1-based while it is 0-based in code.
        tb.title = context!!.getString(R.string.logging_slide_log_view_title, slideNum)
        val exit = dialogLayout.findViewById<ImageButton>(R.id.exitButton)
        val learnCB = dialogLayout.findViewById<CheckBox>(R.id.LearnCheckBox)
        val draftCB = dialogLayout.findViewById<CheckBox>(R.id.DraftCheckBox)
        val comChkCB = dialogLayout.findViewById<CheckBox>(R.id.CommunityCheckCheckBox)
        learnCB.setOnCheckedChangeListener { _, checked -> lla.updateList(checked, draftCB.isChecked, comChkCB.isChecked) }
        draftCB.setOnCheckedChangeListener { _, checked -> lla.updateList(learnCB.isChecked, checked, comChkCB.isChecked) }
        comChkCB.setOnCheckedChangeListener { _, checked -> lla.updateList(learnCB.isChecked, draftCB.isChecked, checked) }
        alertDialog.setView(dialogLayout)
        logDialog = alertDialog.create()
        exit.setOnClickListener {
            logDialog?.dismiss()
        }
    }


    /**
     * Checks each slide of the story to see if all slides have been approved
     * @return true if all approved, otherwise false
     */
    protected fun checkAllMarked(): Boolean {
        for (slide in Workspace.activeStory.slides) {
            if (!slide.isChecked && slide.slideType in
                    arrayOf(SlideType.FRONTCOVER,SlideType.NUMBEREDPAGE,SlideType.LOCALSONG)) {
                return false
            }
        }
        return true
    }

    /**
     * Updates the shared preference file to mark the story as approved
     */
    protected fun saveConsultantApproval() {
        Workspace.activeStory.isApproved = true
    }

    /**
     * Launches the dramatization phase for the story and starts back at first slide
     * TODO: moving back to first slide is currently broken
     */
    protected fun launchDramatizationPhase() {
        Toast.makeText(context, "Congrats!", Toast.LENGTH_SHORT).show()
        //Move to dramatization, slide 0.
        Workspace.activeSlideNum = 0
        (activity as PhaseBaseActivity).jumpToPhase(Phase(PhaseType.DRAMATIZATION))
    }
}
