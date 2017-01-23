package org.sil.storyproducer.controller.logging;

/**
 * Created by user on 1/16/2017.
 */

public class DraftEntry extends LogEntry {
    private int slideNum;
    private Type type;

    DraftEntry(long dateTime, Type type, int slideNum) {
        super(dateTime, Phase.Draft);
        this.slideNum=slideNum;
        this.type=type;
    }

    public enum Type{
        LWC_pb("LWC Playback"), MT_rec("Mother Tongue Recording"),
        MT_pb("Mother Tongue Playback");

        private String displayName;

        private Type(String displayName){
            this.displayName=displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
