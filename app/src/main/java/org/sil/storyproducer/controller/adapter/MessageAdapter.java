package org.sil.storyproducer.controller.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.messaging.Message;
import org.sil.storyproducer.model.messaging.MessageROCC;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by annmcostantino on 4/8/2018.
 */

public class MessageAdapter extends BaseAdapter {

    List<MessageROCC> messages = new ArrayList<>();
    List<MessageROCC> queuedMessages = new ArrayList<>();
    Context con;
    int lastID = -1;

    public MessageAdapter(Context context){
        this.con = context;
    }

    public void add(MessageROCC m){
        this.messages.add(m);
        notifyDataSetChanged();
    }

    public void setMessageHistory(List<MessageROCC> m){
        messages = m;
    }

    public void addQueuedMessage(MessageROCC m) {
        queuedMessages.add(m);
        notifyDataSetChanged();
    }

    public void setQueuedMessages(ArrayDeque<MessageROCC> qm) {
        queuedMessages = new ArrayList(qm);
    }

    public List<MessageROCC> getMessageHistory(){
        return this.messages;
    }

    public void setLastID(int n){
        lastID = n;
    }

    public int getLastID(){
        return lastID;
    }

    @Override
    public int getCount(){
        return messages.size();
    }

    @Override
    public Object getItem(int i){
        return messages.get(i);
    }

    @Override
    public long getItemId(int i){
        return i;
    }

    //Creates a row
    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        MessageViewHolder holder = new MessageViewHolder();
        LayoutInflater inflater = (LayoutInflater) con.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        MessageROCC message;
        if (i < messages.size()) {
            message = messages.get(i);
            if (message.isTranscript()) {
                convertView = inflater.inflate(R.layout.phone_back_translation_message_layout, null);
            } else if (message.isConsultant()) {
                convertView = inflater.inflate(R.layout.rocc_message_layout, null);
            } else {
                convertView = inflater.inflate(R.layout.phone_message_layout, null);
            }
            holder.messageBody = convertView.findViewById(R.id.message_body);
            convertView.setTag(holder);
            holder.messageBody.setText(message.getMessage());
        } else if (i == messages.size()) {
            convertView = inflater.inflate(R.layout.unread_messages_divider_layout, null);
        } else {
            message = queuedMessages.get(i - messages.size() - 1);
            convertView = inflater.inflate(R.layout.phone_queued_message_layout, null);
            holder.messageBody = convertView.findViewById(R.id.message_body);
            convertView.setTag(holder);
            holder.messageBody.setText(message.getMessage());
        }

        return convertView;
    }


}

class MessageViewHolder {
    public TextView messageBody;
}
