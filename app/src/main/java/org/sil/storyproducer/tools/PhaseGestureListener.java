package org.sil.storyproducer.tools;

import android.app.Activity;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;

/**
 * PhaseGestureListener listens for swipe up and down events and moves to the correct activity based on that
 * it implements the SimpleOnGestureListener
 */
public class PhaseGestureListener extends SimpleOnGestureListener {

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private String STORY_NAME = "storyname";
    private Activity activity;

    /**
     * Constructor for PhaseGestureListener
     * @param mActivity the activity so that gestureListener can move to different activities
     */
    public PhaseGestureListener(Activity mActivity) {
        activity = mActivity;
    }

    /**
     * Override the onDown event
     * @param event
     * @return true so that the event can move onto the onFling MotionEvent
     */
    @Override
    public boolean onDown(MotionEvent event) {
        return true;
    }

    /**
     * OnFling event figures out if the touch was a swipe down or up and then moves to the correct activity
     * @param e1
     * @param e2
     * @param velocityX
     * @param velocityY
     * @return true
     */
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH){
            return false;
        }
        if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE      //swipe up
                && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
            if(StoryState.getCurrentPhase().getPhaseType() != Phase.PhaseType.REMOTE_CHECK) { //no swipe in RC
                startNextActivity();
                return true;
            }
        } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE       //swipe down
                && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
            if(StoryState.getCurrentPhase().getPhaseType() != Phase.PhaseType.REMOTE_CHECK) { //no swipe in RC
                startPrevActivity();
                return true;
            }
        }
        return false;
    }

    /**
     * starts the previous phase and starts that activity
     */
    private void startPrevActivity() {
        Phase currPhase = StoryState.getCurrentPhase();
        Phase phase = StoryState.advancePrevPhase();
        if(currPhase.getTitle().equals(phase.getTitle())) return;       //if the same phase don't create new activity
        Intent intent = new Intent(activity.getApplicationContext(), phase.getTheClass());
        intent.putExtra(STORY_NAME, StoryState.getStoryName());
        activity.startActivity(intent);
        activity.finish();
        activity.overridePendingTransition(R.anim.enter_down, R.anim.exit_down);
    }

    /**
     * starts the next phase and starts that activity
     */
    private void startNextActivity() {
        Phase currPhase = StoryState.getCurrentPhase();
        Phase phase = StoryState.advanceNextPhase();
        if(currPhase.getTitle().equals(phase.getTitle())) return;       //if the same phase don't create new activity
        Intent intent = new Intent(activity.getApplicationContext(), phase.getTheClass());
        intent.putExtra(STORY_NAME, StoryState.getStoryName());
        activity.startActivity(intent);
        activity.finish();
        activity.overridePendingTransition(R.anim.enter_up, R.anim.exit_up);
    }

}
