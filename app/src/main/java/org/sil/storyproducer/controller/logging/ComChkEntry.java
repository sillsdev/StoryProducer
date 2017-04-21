package org.sil.storyproducer.controller.logging;

import android.content.Context;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;

/**
 * Created by Michael D. Baxter on 1/16/2017.
 *
 */

public class ComChkEntry extends LogEntry {
    private int slideNum;
    private Type type;

    private ComChkEntry(long dateTime, Type type, int slideNum) {
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
        COMMENT_PLAYBACK, COMMENT_RECORDING,
        DRAFT_PLAYBACK;

        private String displayName;

        private void setDisplayName(String str){
            this.displayName = str;
        }

        public static void init(Context context){
            COMMENT_PLAYBACK.setDisplayName(context.getString(R.string.COMMENT_PLAYBACK));
            COMMENT_RECORDING.setDisplayName(context.getString(R.string.COMMENT_RECORDING));
            DRAFT_PLAYBACK.setDisplayName(context.getString(R.string.DRAFT_PLAYBACK));
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
