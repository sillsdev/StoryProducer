package org.sil.storyproducer.model.messaging;

/**
 * Created by annmcostantino on 4/7/2018.
 */

public class Message {
    private boolean fromTranslator;
    private String message;

    public Message(boolean b, String m){
        fromTranslator = b;
        message = m;
    }

    public boolean isFromTranslator(){
        return fromTranslator;
    }

    public String getMessage(){
        return message;
    }
}
