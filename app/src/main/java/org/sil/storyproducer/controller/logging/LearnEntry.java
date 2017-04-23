package org.sil.storyproducer.controller.logging;

import android.content.Context;

import org.sil.storyproducer.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class LearnEntry extends LogEntry {
    private static long MIN_DURATION = 500;

    private int startSlide;
    private int endSlide;
    private long duration;
    private static Context mContext = null;

    public static void init(Context context){
        mContext = context;
    }

    private LearnEntry(long dateTime, int start, int end, long duration){
        super(dateTime);
        startSlide =start;
        endSlide =end;
        this.duration=duration;
    }

    public String getPhase(){
        return mContext.getString(R.string.learnTitle);
    }

    public int getColor(){
        return R.color.learn_phase;
    }

    @Override
    public String getDescription(){
        String ret;
        if(startSlide ==endSlide ){
            ret = mContext.getResources().getQuantityString(R.plurals.logging_numSlides, 1)+" "+(startSlide+1);
        } else {
            ret = mContext.getResources().getQuantityString(R.plurals.logging_numSlides, 2)+" "+(startSlide+1)+"-"+(endSlide+1);
        }
        ret+=" ("+formatDuration(duration)+")";
        return ret;
    }

    private String formatDuration(long duration){
        String secUnit = mContext.getString(R.string.SECONDS_ABBREVIATION);
        String minUnit = mContext.getString(R.string.MINUTES_ABBREVIATION);
        if(duration<1000){
            return ("<1 "+secUnit);
        }
        int roundedSecs = (int) ((duration/1000.0)+0.5);
        int mins = roundedSecs / 60;
        String minString = "";
        if(mins > 0){
            minString = mins+" "+minUnit+" ";
        }
        return minString+(roundedSecs % 60)+" "+secUnit;
    }

    /**
     * Saves the log entry and returns true, if and only if it passes the filter.
     * @param start
     * @param end
     * @param duration
     * @return
     */
    public static boolean saveFilteredLogEntry(int start, int end, long duration){
        if (duration < MIN_DURATION){
            return false;
        }
        Logging.saveLogEntry(new LearnEntry(System.currentTimeMillis(), start, end, duration));
        return true;
    }

}
