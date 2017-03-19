package org.sil.storyproducer.controller.logging;

/**
 * Created by user on 1/15/2017.
 */

public class LearnEntry extends LogEntry {
    private double startPosition;
    private double endPosition;

    public LearnEntry(long dateTime, double start, double end){
        super(dateTime, Phase.Learn);
        startPosition=start;
        endPosition=end;
    }
}
