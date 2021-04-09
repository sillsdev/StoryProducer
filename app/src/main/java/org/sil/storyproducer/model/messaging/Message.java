package org.sil.storyproducer.model.messaging;

/**
 * Created by annmcostantino on 4/7/2018.
 */

public class Message {
    private final boolean fromPhone;
    private final String message;

    public Message(boolean b, String m){
        fromPhone = b;
        message = m;
    }

    public boolean isFromPhone(){
        return fromPhone;
    }

    public String getMessage(){
        return message;
    }
}
