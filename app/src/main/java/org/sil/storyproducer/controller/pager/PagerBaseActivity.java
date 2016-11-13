package org.sil.storyproducer.controller.pager;

import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.support.v7.widget.Toolbar;
import android.support.v4.content.res.ResourcesCompat;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.tools.GestureListener;


/**
 * The activty that is the base of the paging views
 */
public class PagerBaseActivity extends AppCompatActivity {

    private PagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private GestureDetectorCompat mDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager_base);

        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        //get the current phase
        Phase phase = StoryState.getPhase();

        Toolbar mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mActionBarToolbar);
        getSupportActionBar().setTitle(phase.getPhaseTitle());
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ResourcesCompat.getColor(getResources(), phase.getPhaseColor(), null)));

        // set up the gesture detector
        mDetector = new GestureDetectorCompat(this, new GestureListener(this));
    }

    /**
     * get the touch event so that it can be passed on to GestureDetector
     * @param event: the MotionEvent
     * @return : the super version of the function
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

}
