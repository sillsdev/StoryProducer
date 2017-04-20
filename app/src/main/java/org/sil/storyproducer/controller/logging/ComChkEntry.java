package org.sil.storyproducer.controller.logging;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;

/**
 * Created by user on 1/16/2017.
 */

public class ComChkEntry extends LogEntry {
    private int slideNum;
    private Type type;

    public ComChkEntry(long dateTime, Type type, int slideNum) {
        super(dateTime, Phase.CommCheck, R.color.comunity_check_phase);
        this.type=type;
        this.slideNum=slideNum;
    }

    @Override
    public int getSlideNum(){
        return slideNum;
    }

    @Override
    public String getDescription(){
        return type.toString();
    }

    public enum Type{
        cmt_pb("Comment Playback"), cmt_rec("Comment Recording"),
        draft_pb("Draft Playback");

        private String displayName;

        private Type(String displayName){
            this.displayName=displayName;
        }

        public ComChkEntry makeEntry(){
            return new ComChkEntry(System.currentTimeMillis(), this,
                    StoryState.getCurrentStorySlide());
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
