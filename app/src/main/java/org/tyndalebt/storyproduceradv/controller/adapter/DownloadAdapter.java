package org.tyndalebt.storyproduceradv.controller.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import org.tyndalebt.storyproduceradv.R;

import java.util.ArrayList;

public class DownloadAdapter extends ArrayAdapter<DownloadDS> implements View.OnClickListener {
    Context mContext;
    private ArrayList<DownloadDS> dataSetArray;
    String apos;

    private static class ViewHolder {
        CheckedTextView chkItem;
    }

    public DownloadAdapter(ArrayList<DownloadDS> data, Context context) {
        super(context, R.layout.bloom_list_item, data);
        this.mContext = context;
        this.dataSetArray = data;
        // Special Apostrophe (not single quote) doesn't transfer in a URL, encode it along with spaces
        apos = new Character((char) 226).toString();
        apos = apos + new Character((char) 128).toString();
        apos = apos + new Character((char) 153).toString();

    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        DownloadDS dataModel=(DownloadDS) object;

        switch (v.getId())
        {
            case R.id.checkedTextView:
                CheckedTextView ctv = (CheckedTextView) v;
                dataModel.setChecked(!ctv.isChecked());   // toggle check
                setCheckmark(ctv, dataModel.getChecked());
                break;
        }
    }

    private int lastPosition = -1;

    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        DownloadDS dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.bloom_list_item, parent,false);
            viewHolder.chkItem = (CheckedTextView) convertView.findViewById(R.id.checkedTextView);

            result = convertView;
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.enter_up : R.anim.enter_down);
        result.startAnimation(animation);
        lastPosition = position;

        String tmpString;
        tmpString = dataModel.getName();
        tmpString = tmpString.replaceAll(apos, "’");

        viewHolder.chkItem.setText(tmpString);
        setCheckmark(viewHolder.chkItem, dataModel.getChecked());
        viewHolder.chkItem.setOnClickListener(this);
        viewHolder.chkItem.setTag(position);
        // Return the completed view to render on screen
        return convertView;
    }


    void setCheckmark(CheckedTextView ctv, Boolean checked) {
        ctv.setChecked(checked);
        if (!ctv.isChecked()) {
            ctv.setCompoundDrawables(null, null, null, null);
//            ctv.setCheckMarkDrawable(null);
        } else {
            Drawable img = getContext().getResources().getDrawable(R.drawable.ic_checkmark_green);
            img.setBounds(0, 0, 120, 120);
            ctv.setCompoundDrawables(img, null, null, null);
//            ctv.setCheckMarkDrawable(R.drawable.ic_checkmark_green);
        }
    }

}

