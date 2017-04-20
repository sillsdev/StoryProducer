package org.sil.storyproducer.controller.logging;

import java.util.GregorianCalendar;

import java.io.Serializable;
import java.text.SimpleDateFormat;

/**
 * Created by user on 1/15/2017.
 */

public abstract class LogEntry implements Serializable, Comparable<LogEntry> {
    private GregorianCalendar dateTime;
    private Phase phase;
    private Long nanoTime;
    private int color;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd yyyy h:mm a");

    public String getDescription(){

        return "";

    }

    LogEntry(long dateTime, Phase phase, int color){
        this.dateTime=new GregorianCalendar();
        this.dateTime.setTimeInMillis(dateTime);
        this.phase = phase;
        this.nanoTime = System.nanoTime();
        this.color = color;
    }

    public int getColor(){
        return color;
    }

    public int getSlideNum(){
        return -1;
    }

    public Phase getPhase(){
        return phase;
    }

    public String getDateTime(){
        return dateFormat.format(dateTime.getTime());
    }


    public int compareTo(LogEntry o){
        int ret = this.dateTime.compareTo(o.dateTime);
        if (ret!=0){
            return ret;
        } else {
            return this.nanoTime.compareTo(o.nanoTime);
        }
    }
}
