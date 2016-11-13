package org.sil.storyproducer.tools;

import android.app.Activity;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;

/**
 * GestureListener listens for swipe up and down events and moves to the correct activity based on that
 * it implements the SimpleOnGestureListener
 */
public class GestureListener extends SimpleOnGestureListener {

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private String STORY_NAME = "storyname";
    private Activity activity;

    /**
     * Constructor for GestureListener
     * @param mActivity: the actvity so that gestureListener can move to different activities
     */
    public GestureListener(Activity mActivity) {
        activity = mActivity;
    }

    /**
     * Override the onDown event
     * @param event
     * @return true: so that the event can move onto the onFling MotionEvent
     */
    @Override
    public boolean onDown(MotionEvent event) {
        System.out.println("onDown: " + event.toString());
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
    public boolean onFling(MotionEvent e1, MotionEvent e2,
                           float velocityX, float velocityY) {
        try {
            if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH){
                return false;
            }
            if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE      //swipe up
                    && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {

                startNextActivity();
            } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE       //swipe down
                    && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                startPrevActivity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * starts the previous phase and starts that activity
     */
    private void startPrevActivity() {
        Phase currPhase = StoryState.getPhase();
        Phase phase = StoryState.getPrevPhase();
        if(currPhase.getPhaseTitle().equals(phase.getPhaseTitle())) return;       //if the same phase don't create new activity
        Intent intent = new Intent(activity.getApplicationContext(), phase.getPhaseClass());
        intent.putExtra(STORY_NAME, StoryState.getStoryName());
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.enter_down, R.anim.exit_down);
    }

    /**
     * startrs the next phase and starts that activity
     */
    private void startNextActivity() {
        Phase currPhase = StoryState.getPhase();
        Phase phase = StoryState.getNextPhase();
        if(currPhase.getPhaseTitle().equals(phase.getPhaseTitle())) return;       //if the same phase don't create new activity
        Intent intent = new Intent(activity.getApplicationContext(), phase.getPhaseClass());
        intent.putExtra(STORY_NAME, StoryState.getStoryName());
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.enter_up, R.anim.exit_up);
    }

}
