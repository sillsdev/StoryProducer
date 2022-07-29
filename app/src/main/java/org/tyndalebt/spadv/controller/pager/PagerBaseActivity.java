package org.tyndalebt.spadv.controller.pager;

import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;

import org.tyndalebt.spadv.R;
import org.tyndalebt.spadv.controller.phase.PhaseBaseActivity;
import org.tyndalebt.spadv.model.Workspace;

/**
 * The activty that is the base of the paging views
 */
public class PagerBaseActivity extends PhaseBaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager_base);

        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        PagerAdapter mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
        ViewPager mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
        //See if the slide number is valid for the new phase.  If not, set it to zero.
        if (!Workspace.INSTANCE.getActivePhase().checkValidDisplaySlideNum(
                Workspace.INSTANCE.getActiveSlideNum()))
            Workspace.INSTANCE.setActiveSlideNum(0);
        int slideNum = Workspace.INSTANCE.getActiveSlideNum();
        //If a slide other than 0 is chosen, use it.  This is to help recalling the last active slide.
        if(slideNum > 0) mViewPager.setCurrentItem(slideNum);

        mViewPager.addOnPageChangeListener(new CircularViewPagerHandler(mViewPager));       //sets the change listener to be the circular handler
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
