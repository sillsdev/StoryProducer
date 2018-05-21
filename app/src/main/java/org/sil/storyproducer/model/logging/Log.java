package org.sil.storyproducer.model.logging;

import java.util.TreeSet;

//use composition or inheritance w/ a TreeSet to add some extra data to it, like its lang & story

public class Log extends TreeSet<LogEntry> {
    private static final String TAG = "SPLog";

    private String lang;
    private String story;

    public Log(String lang, String story){
        this.lang = lang;
        this.story= story;

        //TODO: consider using some other mechanism of handling nulls
        if(lang == null) {
            android.util.Log.e(TAG, "Missing language parameter. Using language-agnostic log");
            this.lang = "NO_LANG";
        }
        if(story == null) {
            android.util.Log.e(TAG, "Missing story parameter. Using story-agnostic log");
            this.story = "NO_STORY";
        }
    }

    public String getLang(){
        return lang;
    }

    public String getStory(){
        return story;
    }

}
