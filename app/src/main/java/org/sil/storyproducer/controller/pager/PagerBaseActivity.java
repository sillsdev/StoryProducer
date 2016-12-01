package org.sil.storyproducer.controller.pager;

import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.support.v7.widget.Toolbar;
import android.support.v4.content.res.ResourcesCompat;
import android.graphics.drawable.ColorDrawable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.support.v4.view.MenuItemCompat;

import org.sil.storyproducer.R;
import org.sil.storyproducer.tools.PhaseMenuItemListener;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.tools.PhaseGestureListener;


/**
 * The activty that is the base of the paging views
 */
public class PagerBaseActivity extends AppCompatActivity {

    private PagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private GestureDetectorCompat mDetector;
    private static boolean getInitialPosition = false;
    private static int previousPosition = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager_base);

        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
//        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            public void onPageScrollStateChanged(int state) {}
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//                if(!getInitialPosition){
//                    getInitialPosition = true;
//                    previousPosition = position;
//                }
//            }
//            public void onPageSelected(int position) {
//                if(position != previousPosition){
//                    TransFrag transFrag = ((PagerAdapter)mViewPager.getAdapter()).getDraftFrag(previousPosition);
//                    transFrag.stopNarrationRecording();
//                    previousPosition = position;
//                }
//            }
//        });
//        return view;);

        //get the current phase
        Phase phase = StoryState.getCurrentPhase();

        Toolbar mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mActionBarToolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ResourcesCompat.getColor(getResources()
                                                                                    , phase.getColor(), null)));

        // set up the gesture detector
        mDetector = new GestureDetectorCompat(this, new PhaseGestureListener(this));
    }

    /**
     * get the touch event so that it can be passed on to GestureDetector
     * @param event the MotionEvent
     * @return the super version of the function
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    /**
     * sets the Menu spinner_item object
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_phases, menu);

        MenuItem item = menu.findItem(R.id.spinner);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);
        spinner.setOnItemSelectedListener(new PhaseMenuItemListener(this));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.phases_menu_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setSelection(StoryState.getCurrentPhaseIndex());
        return true;
    }

}
