package org.sil.storyproducer.controller.logging;

import java.io.Serializable;
import java.util.TreeSet;

/**
 * Created by user on 1/23/2017.
 */

//use composition or inheritance w/ a TreeSet to add some extra data to it, like its lang & story

public class Log extends TreeSet<LogEntry> {


    private String lang;
    private String story;

    public Log(String lang, String story){
        this.lang = lang;
        this.story= story;
        assert (this.lang != null);
        assert (this.story != null);

    }

    public String getLang(){
        return lang;
    }

    public String getStory(){
        return story;
    }

}
