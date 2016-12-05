package org.sil.storyproducer.tools;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;

public class PhaseMenuItemListener implements OnItemSelectedListener {

    private String STORY_NAME = "storyname";
    private Activity activity;

    /**
     * Constructor for PhaseGestureListener
     * @param mActivity the actvity so that gestureListener can move to different activities
     */
    public PhaseMenuItemListener(Activity mActivity) {
        activity = mActivity;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
    int pos, long id) {
        Phase[] phases = StoryState.getPhases();
        //return if the phase is the same as the current phase
        if(phases[pos].getTitle().equals(StoryState.getCurrentPhase().getTitle())) return;
        jumpToPhase(phases[pos]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    /**
     * jump to the phase given as a parameter
     * @param phase
     */
    private void jumpToPhase(Phase phase) {
        StoryState.setCurrentPhase(phase);
        Intent intent = new Intent(activity.getApplicationContext(), phase.getTheClass());
        intent.putExtra(STORY_NAME, StoryState.getStoryName());
        activity.startActivity(intent);
    }
}
