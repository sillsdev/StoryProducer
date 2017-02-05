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
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd yyyy h:m a");

    LogEntry(long dateTime, Phase phase){
        this.dateTime=new GregorianCalendar();
        this.dateTime.setTimeInMillis(dateTime);
        this.phase = phase;
        this.nanoTime = System.nanoTime();
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
