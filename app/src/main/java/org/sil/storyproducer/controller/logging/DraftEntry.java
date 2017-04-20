package org.sil.storyproducer.controller.logging;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;

/**
 * Created by user on 1/16/2017.
 */

public class DraftEntry extends LogEntry {

    private int slideNum;
    private Type type;

    public DraftEntry(long dateTime, Type type, int slideNum) {
        super(dateTime, Phase.Draft, R.color.draft_phase);
        this.slideNum=slideNum;
        this.type=type;
    }

    @Override
    public int getSlideNum() {
        return slideNum;
    }

    public String getDescription(){
        return type.toString();
    }

    public enum Type {
        LWC_pb("LWC Playback"), draft_rec("Draft Recording"),
        draft_pb("Draft Playback");

        private String displayName;

        public DraftEntry makeEntry(){
            return new DraftEntry(System.currentTimeMillis(), this,
                    StoryState.getCurrentStorySlide());
        }

        private Type(String displayName){
            this.displayName=displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
