package org.sil.storyproducer.model.logging;

import android.content.Context;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;

public class DraftEntry extends LogEntry {

    private int slideNum;
    private Type type;
    private static String mPhase;

    public static void init(Context context){
        mPhase = context.getString(R.string.draftTitle);
        Type.init(context);
    }

    private DraftEntry(long dateTime, Type type, int slideNum) {
        super(dateTime);
        this.slideNum=slideNum;
        this.type=type;
    }

    @Override
    public int getColor(){
        return R.color.draft_phase;
    }

    @Override
    public String getPhase(){
        return mPhase;
    }

    @Override
    public boolean appliesToSlideNum(int slideNum) {
        return slideNum == this.slideNum;
    }

    @Override
    public String getDescription(){
        return type.toString();
    }

    public enum Type {
        LWC_PLAYBACK, DRAFT_RECORDING,
        DRAFT_PLAYBACK;

        private String displayName;

        public DraftEntry makeEntry(){
            return new DraftEntry(System.currentTimeMillis(), this,
                    StoryState.getCurrentStorySlide());
        }

        private void setDisplayName(String str){
            this.displayName = str;
        }

        public static void init(Context context){
            LWC_PLAYBACK.setDisplayName(context.getString(R.string.LWC_PLAYBACK));
            DRAFT_RECORDING.setDisplayName(context.getString(R.string.DRAFT_RECORDING));
            DRAFT_PLAYBACK.setDisplayName(context.getString(R.string.DRAFT_PLAYBACK));
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
