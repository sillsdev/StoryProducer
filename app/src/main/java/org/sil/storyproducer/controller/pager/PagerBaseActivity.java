package org.sil.storyproducer.controller.pager;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.phase.PhaseBaseActivity;
import org.sil.storyproducer.model.Workspace;

/**
 * The activty that is the base of the paging views
 */
public class PagerBaseActivity extends PhaseBaseActivity {

    private PagerAdapter mPagerAdapter;
    private ViewPager mViewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager_base);

        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
        //If a slide other than 0 is chosen, use it.  This is to help recalling the last active slide.
        int slideNum = Workspace.INSTANCE.getActiveSlideNum();
        if(slideNum > 0) mViewPager.setCurrentItem(slideNum);

        mViewPager.setOnPageChangeListener(new CircularViewPagerHandler(mViewPager));       //sets the change listener to be the circular handler
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
