package org.sil.storyproducer.controller.pager;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.phase.PhaseBaseActivity;
import org.sil.storyproducer.model.StoryState;

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
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(StoryState.getCurrentStorySlide());           //sets view pager to the current slide from the story state
    }

    @Override
    public void onPause() {
        super.onPause();
        StoryState.setCurrentStorySlide( mViewPager.getCurrentItem());      //sets the story state to the current item before the activity is left
    }

}
