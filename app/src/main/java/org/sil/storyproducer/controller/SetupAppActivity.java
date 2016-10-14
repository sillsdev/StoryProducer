package org.sil.storyproducer.controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ActionMenuView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.MainActivity;

public class SetupAppActivity extends AppCompatActivity {
    private GestureDetectorCompat gestureDetector;
    private ViewFlipper settingFlipper;
    private LinearLayout pageIndicatorLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_setup_app);
        setupPrefViews();
    }
    public void flip(int direction){
        //Write Settings Text to Shared Preferences
        EditText settingsText = (EditText) settingFlipper.getCurrentView();
        if(!settingsText.getText().toString().isEmpty()){
            writePreference(settingsText.getHint().toString(), settingsText.getText().toString());
        }
        //0 for Next Slide, 1 for Previous Slide
        if(direction == 0) {
            if(settingFlipper.getDisplayedChild() < settingFlipper.getChildCount()-1) {
                settingFlipper.setInAnimation(this, R.anim.slide_in_from_right);
                settingFlipper.setOutAnimation(this, R.anim.slide_out_to_left);
                settingFlipper.showNext();
                pageIndicatorLayout.getChildAt(settingFlipper.getDisplayedChild()).setBackground(getDrawable(R.drawable.setup_indicatorcircle_current));
                pageIndicatorLayout.getChildAt(settingFlipper.getDisplayedChild() - 1).setBackground(getDrawable(R.drawable.setup_indicatorcircle));
            }
        } else if (direction == 1) {
            if(settingFlipper.getDisplayedChild() != 0) {
                settingFlipper.setInAnimation(this, R.anim.slide_in_from_left);
                settingFlipper.setOutAnimation(this, R.anim.slide_out_to_right);
                settingFlipper.showPrevious();
                pageIndicatorLayout.getChildAt(settingFlipper.getDisplayedChild()).setBackground(getDrawable(R.drawable.setup_indicatorcircle_current));
                pageIndicatorLayout.getChildAt(settingFlipper.getDisplayedChild() + 1).setBackground(getDrawable(R.drawable.setup_indicatorcircle));
            }
        }
    }
    private void setupPrefViews(){
        settingFlipper = (ViewFlipper) findViewById(R.id.setup_settingstext);
        pageIndicatorLayout = (LinearLayout) findViewById(R.id.setup_indicator);
        gestureDetector = new GestureDetectorCompat(this, new SwipeDetector());
        for (int i = 0; i < settingFlipper.getChildCount(); i++){
            //Add Page Indicators
            ImageView pageIndicator = new ImageView(this);
            LinearLayout.LayoutParams lp = new ActionMenuView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(10,0,10,0);
            pageIndicator.setLayoutParams(lp);
            if(i == 0) {
                pageIndicator.setBackground(getDrawable(R.drawable.setup_indicatorcircle_current));
            } else {
                pageIndicator.setBackground(getDrawable(R.drawable.setup_indicatorcircle));
            }
            pageIndicatorLayout.addView(pageIndicator);

            EditText settingsText = (EditText)settingFlipper.getChildAt(i);
            settingsText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        flip(0);
                    }
                    if(actionId == EditorInfo.IME_ACTION_DONE){
                        Intent intent;
                        intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                    }
                    return false;
                }
            });
        }
    }
    private void writePreference(String prefKey, String prefValue){
        SharedPreferences sPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor sEditor = sPref.edit();
        sEditor.putString(prefKey,prefValue);
        sEditor.apply();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
            //Swipe Left
            if (vX < 1) {
                flip(0);
            }
            //Swipe Right
            if (vX > 1) {
                flip(1);
            }
            return true;
        }
    }

}
