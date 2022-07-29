package org.tyndalebt.spadv.tools;

import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import org.tyndalebt.spadv.controller.phase.PhaseBaseActivity;
import org.tyndalebt.spadv.model.PhaseType;
import org.tyndalebt.spadv.model.Workspace;

/**
 * PhaseGestureListener listens for swipe up and down events and moves to the correct activity based on that
 * it implements the SimpleOnGestureListener
 */
public class PhaseGestureListener extends SimpleOnGestureListener {

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private PhaseBaseActivity pbactivity;

    /**
     * Constructor for PhaseGestureListener
     * @param mActivity the activity so that gestureListener can move to different activities
     */
    public PhaseGestureListener(PhaseBaseActivity mActivity) {
        pbactivity = mActivity;
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
            if(Workspace.INSTANCE.getActivePhase().getPhaseType() != PhaseType.REMOTE_CHECK) { //no swipe in RC
                pbactivity.startNextActivity();
                return true;
            }
        } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE       //swipe down
                && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
            if(Workspace.INSTANCE.getActivePhase().getPhaseType() != PhaseType.REMOTE_CHECK) { //no swipe in RC
                pbactivity.startPrevActivity();
                return true;
            }
        }
        return false;
    }
}
