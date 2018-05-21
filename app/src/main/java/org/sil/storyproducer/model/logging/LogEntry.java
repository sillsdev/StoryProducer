package org.sil.storyproducer.model.logging;

import java.util.GregorianCalendar;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Locale;

public abstract class LogEntry implements Serializable, Comparable<LogEntry> {
    private GregorianCalendar dateTime;
    private Long nanoTime;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd yyyy h:mm a", Locale.US);

    public String getDescription(){

        return "";

    }

    LogEntry(long timestamp){
        this.dateTime=new GregorianCalendar();
        this.dateTime.setTimeInMillis(timestamp);
        this.nanoTime = System.nanoTime();
    }

    public abstract int getColor();

    public abstract String getPhase();

    public abstract boolean appliesToSlideNum(int slideNum);

    public String getDateTime(){
        return DATE_FORMAT.format(dateTime.getTime());
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
