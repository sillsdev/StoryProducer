package org.sil.storyproducer.model.messaging;

/**
 * Created by annmcostantino on 4/7/2018.
 */

public class Message {
    private boolean fromTranslator;
    private String message;

    Message(boolean b, String m){
        fromTranslator = b;
        message = m;
    }

    protected boolean isFromTranslator(){
        return fromTranslator;
    }

    protected String getMessage(){
        return message;
    }
}
