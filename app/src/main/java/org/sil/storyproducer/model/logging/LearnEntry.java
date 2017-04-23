package org.sil.storyproducer.model.logging;

import android.content.Context;
import android.content.res.Resources;

import org.sil.storyproducer.R;
import org.sil.storyproducer.tools.file.LogFiles;

public class LearnEntry extends LogEntry {
    private static long MIN_DURATION = 500;

    private int startSlide;
    private int endSlide;
    private long duration;
    private static Resources mResources;

    public static void init(Context context){
        mResources = context.getResources();
    }

    private LearnEntry(long dateTime, int start, int end, long duration){
        super(dateTime);
        startSlide =start;
        endSlide =end;
        this.duration=duration;
    }

    public String getPhase(){
        return mResources.getString(R.string.learnTitle);
    }

    public int getColor(){
        return R.color.learn_phase;
    }

    @Override
    public String getDescription(){
        String ret;
        if(startSlide ==endSlide ){
            ret = mResources.getQuantityString(R.plurals.logging_numSlides, 1)+" "+(startSlide+1);
        } else {
            ret = mResources.getQuantityString(R.plurals.logging_numSlides, 2)+" "+(startSlide+1)+"-"+(endSlide+1);
        }
        ret+=" ("+formatDuration(duration)+")";
        return ret;
    }

    private String formatDuration(long duration){
        String secUnit = mResources.getString(R.string.SECONDS_ABBREVIATION);
        String minUnit = mResources.getString(R.string.MINUTES_ABBREVIATION);
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
        LogFiles.saveLogEntry(new LearnEntry(System.currentTimeMillis(), start, end, duration));
        return true;
    }

}
