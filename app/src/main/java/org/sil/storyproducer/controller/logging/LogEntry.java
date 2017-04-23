package org.sil.storyproducer.controller.logging;

import java.util.GregorianCalendar;

import java.io.Serializable;
import java.text.SimpleDateFormat;

public abstract class LogEntry implements Serializable, Comparable<LogEntry> {
    private GregorianCalendar dateTime;
    private Long nanoTime;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd yyyy h:mm a");

    public String getDescription(){

        return "";

    }

    LogEntry(long dateTime){
        this.dateTime=new GregorianCalendar();
        this.dateTime.setTimeInMillis(dateTime);
        this.nanoTime = System.nanoTime();
    }

    public abstract int getColor();

    public abstract String getPhase();

    public int getSlideNum(){
        return -1;
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
