package org.sil.storyproducer.controller.logging;

/**
 * Created by user on 1/16/2017.
 */

public class ComChkEntry extends LogEntry {
    private int slideNum;
    private Type type;

    ComChkEntry(long dateTime, Type type, int slideNum) {
        super(dateTime, Phase.CommCheck);
        this.type=type;
        this.slideNum=slideNum;
    }

    public enum Type{
        cmt_pb("Comment Playback"), cmt_rec("Comment Recording"),
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
