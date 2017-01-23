package org.sil.storyproducer.controller.logging;

import java.io.Serializable;

/**
 * Created by user on 1/15/2017.
 */

public abstract class LogEntry implements Serializable, Comparable<LogEntry> {
    private long dateTime;
    private Phase phase;

    public String getStoryName() {
        return storyName;
    }

    public void setStoryName(String storyName) {
        this.storyName = storyName;
    }

    private String storyName;

    LogEntry(long dateTime, Phase phase){
        this.dateTime=dateTime;
        this.phase = phase;
    }



    public int compareTo(LogEntry o){
        if (this.dateTime>o.dateTime){
            return 1;
        } else if (this.dateTime<o.dateTime){
            return -1;
        } else {
            return 0;
        }
    }
}
