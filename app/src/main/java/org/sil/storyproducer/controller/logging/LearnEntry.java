package org.sil.storyproducer.controller.logging;

/**
 * Created by user on 1/15/2017.
 */

public class LearnEntry extends LogEntry {
    private int startPosition;
    private int endPosition;

    public LearnEntry(long dateTime, int start, int end){
        super(dateTime, Phase.Learn);
        startPosition=start;
        endPosition=end;
    }

    public static LearnEntry makeEntry(int start, int end){
        return new LearnEntry(System.currentTimeMillis(), start, end);
    }
}
