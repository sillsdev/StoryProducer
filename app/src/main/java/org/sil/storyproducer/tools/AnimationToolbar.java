package org.sil.storyproducer.tools;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.RelativeLayout;

/**
 * Class requires three things: <br/>
 * <ul>
 *     <li>Floating Action Button (Must be 16dp away from edges)</li>
 *     <li>Relative Layout (Toolbar that will be animated)</li>
 *     <li> Highly recommended to wrap your View in a dummyView <br/>
 *          to listen for click to close toolbar </li>
 * </ul>
 * This class takes a relative layout and a floating action button and
 * adds cool animations to both so that when you press the floating action button
 * a toolbar shows up. Similar to this animation:
 * <a href="https://material.io/guidelines/components/buttons-floating-action-button.html#buttons-floating-action-button-transitions">Fab reference</a>
 * <br/>
 * The floating action button must be 16dp away from the edges:
 * <a href="https://material.io/guidelines/components/buttons-floating-action-button.html#buttons-floating-action-button-floating-action-button">Fab guidelines</a>
 * <br/>
 * <br/>
 * The animation of the toolbar (relativeLayout) depends on the current version of android: <br/>
 * Version 4.4 - Animation is used for the toolbar (relativeLayout) recycled animations are used when toggling animation. <br/>
 * See <a href = "https://developer.android.com/guide/topics/graphics/view-animation.html">Animation Overview</a> <br/>
 * See <a href = "https://developer.android.com/reference/android/view/animation/Animation.html">Animation Class</a> <br/>
 * Version 5.0 and up - Animator (Circular Reveal) is used for the toolbar (relativeLayout) new instances are used when toggling animator.<br/>
 * See <a href = "https://developer.android.com/reference/android/view/ViewAnimationUtils.html#createCircularReveal(android.view.View, int, int, float, float)">Circular Reveal</a>
 * <br/>
 * See <a href = "https://developer.android.com/reference/android/animation/Animator.html">Animator Class</a>
 * <br/>
 * Animation sets are used to couple the rotation and alpha animation for the floating action button.
 *
 *
 */
public final class AnimationToolbar {

    private boolean toolBarOpen = false;
    private FloatingActionButton fab = null;
    private RelativeLayout toolBar = null;
    private AnimationSet fabPress = null;
    private AnimationSet fabUnPress = null;
    private Animation openToolBar = null;
    private Animation closeToolBar = null;
    private boolean usingNewAnimators = false;
    private DisplayMetrics displayMetrics = null;

    private final float DP_CONVERSION_FACTOR;
    private final int OPEN_TOOLBAR_DELAY = 60;


    /**
     * The constructor of the AnimationToolbar class.
     * @param floatingActionBut The floating action to be passed in.
     * @param relativeLayout The relative layout that will act as the toolbar.
     * @param currentActivity
     * @throws ClassCastException Will be thrown if either floatingActionBut
     * or relativeLayout are not of the correct type of FloatingActionButton or RelativeLayout.
     */
    public AnimationToolbar(View floatingActionBut, View relativeLayout, final Activity currentActivity) throws ClassCastException {
        if(!(floatingActionBut instanceof FloatingActionButton)) {
            throw new ClassCastException("The floatingActionBut is not of type FloatingActionButton!");
        }else if(!(relativeLayout instanceof RelativeLayout)){
            throw new ClassCastException("The relativeLayout is not of type RelativeLayout!");
        }

        usingNewAnimators = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        displayMetrics = currentActivity.getResources().getDisplayMetrics();
        DP_CONVERSION_FACTOR = ((displayMetrics.densityDpi / (float) DisplayMetrics.DENSITY_DEFAULT) * 1.0f);

        this.fab = (FloatingActionButton) floatingActionBut;
        this.toolBar = (RelativeLayout) relativeLayout;
        setToolBarHeight(currentActivity);

        //The addonLayoutChangeListener is used to prevent accessing the relativelayout
        //before the relativelayout has been drawn.
        toolBar.addOnLayoutChangeListener(new View.OnLayoutChangeListener(){
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                setAnimators();
                setListeners();
            }
        });
        //Hide the relativelayout on start
        toolBar.setVisibility(View.INVISIBLE);
    }

    /**
     * This function can be called to close the toolbar and allow the floating action button to
     * reappear in the activity/view.
     */
    @SuppressLint("NewApi")
    public void close(){
        if(toolBarOpen){
            fab.startAnimation(fabUnPress);
            if(usingNewAnimators){
                createCloseToolBarAnimator().start();
            }else{
                toolBar.startAnimation(closeToolBar);
            }
        }
        toolBarOpen = false;
    }

    public boolean isOpen(){
        return (toolBar.getVisibility() == View.VISIBLE);
    }


    private void setToolBarHeight(Activity currentActivity) {
        //Get appropriate classes to discover the size of the screen.
        Display display = currentActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        //An arbitrary size of 10% of the screen size was found
        //to be an appropriate height for the size of the toolbar.
        int height = (int)(size.y * .1);

        toolBar.getLayoutParams().height = height;
    }

    private void setAnimators(){
        fabPress = new AnimationSet(true);
        fabUnPress = new AnimationSet(true);

        //This set of animations is responsible for pivoting the floating action button 90 degrees
        //and also applies an alpha animation to slowly make the floating action button disappear
        fabPress.addAnimation(new RotateAnimation(0.0f,90, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0.5f));
        fabPress.addAnimation(new AlphaAnimation(1.0f, 0.0f));
        fabPress.setDuration(100);
        fabPress.setFillAfter(true);

        //These animations is opposite animation of above.
        //(Make floating action button reappear and pivot up 90 degree)
        fabUnPress.addAnimation(new RotateAnimation(90f, 0.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0.5f));
        fabUnPress.addAnimation(new AlphaAnimation(0.0f, 1.0f));
        fabUnPress.setDuration(50);
        fabUnPress.setStartOffset(75);
        fabUnPress.setFillAfter(true);

        if(!usingNewAnimators){
            float relWidthDp = toolBar.getWidth() / DP_CONVERSION_FACTOR;
            float relHeightDp = toolBar.getHeight() / DP_CONVERSION_FACTOR;
                                                                          /*pivot x*/                        /*pivot y*/
            openToolBar = new ScaleAnimation(0.0f, 1.0f, .5f, 1.0f, Animation.RELATIVE_TO_SELF,(relWidthDp - 72)/relWidthDp,  Animation.RELATIVE_TO_SELF,(relHeightDp - 16)/relHeightDp);
            openToolBar.setDuration(300);
            openToolBar.setStartOffset(OPEN_TOOLBAR_DELAY);
                                                                      /*pivot x*/                        /*pivot y*/
            closeToolBar = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.1f, Animation.RELATIVE_TO_SELF, (relWidthDp - 44)/relWidthDp, Animation.RELATIVE_TO_SELF,(relHeightDp - 44)/relHeightDp);
            closeToolBar.setDuration(150);

            //set fill after function allows an animation to persist its animation after
            //the animation has proceeded. Basically freezes the view in the finish animation state.
            //http://stackoverflow.com/questions/5886624/animation-setfillafter-before-do-they-work-what-are-they-for
            openToolBar.setFillAfter(true);
            closeToolBar.setFillAfter(true);
        }
    }

    private void setListeners(){
        if(!usingNewAnimators){
            openToolBar.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    toolBar.setVisibility(View.VISIBLE);}

                public void onAnimationEnd(Animation animation) {}

                public void onAnimationRepeat(Animation animation) {}
            });
            closeToolBar.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                public void onAnimationEnd(Animation animation) {
                    toolBar.setVisibility(View.INVISIBLE);}

                public void onAnimationRepeat(Animation animation) {}
            });
        }

        fabPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            public void onAnimationEnd(Animation animation) {
                fab.setVisibility(View.INVISIBLE);
                fab.clearAnimation();
            }

            public void onAnimationRepeat(Animation animation) {}
        });

        fabUnPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {fab.setVisibility(View.VISIBLE);}

            public void onAnimationEnd(Animation animation) {}

            public void onAnimationRepeat(Animation animation) {}
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                if(!toolBarOpen){
                    fab.startAnimation(fabPress);
                    if(usingNewAnimators){
                        createOpenToolBarAnimator().start();
                    }else{
                        toolBar.startAnimation(openToolBar);
                    }
                    toolBarOpen = true;
                }
            }});
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Animator createOpenToolBarAnimator(){
        Animator toReturn = null;
        //prepare stuff for Opening the relative layout animations
        //convert from pixels (toolBar.getWidth() || toolBar.getHeight()) to dp's
        float dpWidthOpen = (toolBar.getWidth() / DP_CONVERSION_FACTOR) -72;
        float dpHeightOpen = (toolBar.getHeight() / DP_CONVERSION_FACTOR) -16;
        int cx = (int) (dpWidthOpen * DP_CONVERSION_FACTOR);
        int cy = (int) (dpHeightOpen * DP_CONVERSION_FACTOR);

        float finalRadius = (float) Math.hypot(cx, cy);
        // create the animator for this view (the start radius is zero)
        toReturn =
                ViewAnimationUtils.createCircularReveal(toolBar, cx, cy, 0, finalRadius);
        toReturn.setStartDelay(100);
        toReturn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                toolBar.setVisibility(View.VISIBLE);
            }
        });

        return toReturn;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Animator createCloseToolBarAnimator(){
        Animator toReturn = null;
        //prepare stuff for closing the relative layout animations
        //convert from pixels (toolBar.getWidth() || toolBar.getHeight()) to dp's
        float dpWidthClose = (toolBar.getWidth()/ DP_CONVERSION_FACTOR) - 44;
        float dpHeightClose = (toolBar.getHeight()/ DP_CONVERSION_FACTOR) -44;
        int cx = (int) (dpWidthClose * DP_CONVERSION_FACTOR);
        int cy = (int) (dpHeightClose * DP_CONVERSION_FACTOR);
        float initialRadius = (float) Math.hypot(cx, cy);
        toReturn =
                ViewAnimationUtils.createCircularReveal(toolBar,cx, cy, initialRadius,0);
        toReturn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                toolBar.setVisibility(View.INVISIBLE);
            }
        });

        return toReturn;
    }
}
