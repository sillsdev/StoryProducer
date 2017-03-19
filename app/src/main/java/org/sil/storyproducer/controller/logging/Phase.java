package org.sil.storyproducer.controller.logging;

import java.io.Serializable;

/**
 * Created by user on 1/15/2017.
 */

public enum Phase implements Serializable{
    Learn("Learn"),
    Draft("Draft"),
    CommCheck("Community Check");

    private String displayName;

    private Phase(String displayName){
        this.displayName=displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
