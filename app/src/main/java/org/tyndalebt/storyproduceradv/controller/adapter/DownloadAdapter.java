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
import org.tyndalebt.storyproduceradv.activities.DownloadActivity;

import java.util.ArrayList;

public class DownloadAdapter extends ArrayAdapter<DownloadDS> implements View.OnClickListener {
    DownloadActivity dla;
    Context mContext;
    private ArrayList<DownloadDS> dataSetArray;
    private static class ViewHolder {
        CheckedTextView chkItem;
    }

    public DownloadAdapter(ArrayList<DownloadDS> data, DownloadActivity pDownloadActivity) {
        super(pDownloadActivity, R.layout.bloom_list_item, data);
        dla = pDownloadActivity;
        this.mContext = pDownloadActivity;
        this.dataSetArray = data;
    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        DownloadDS dataModel=(DownloadDS) object;
        // The chosenLanguage variable set below is coming directly from the display list. So,
        //   if the order or format of the display list of languages changes, this code needs to change
        //   to reflect chosenLanguage to be set to the English equivalent of the language only.

        if (dataModel.URL.equals("Language")) {  // first pass, now show stories for chosen language
            String DisplayLine[] = dataModel.fileName.split("/");
            dla.chosenLanguage = DisplayLine[0].trim();
            dla.copyFile(DownloadActivity.BLOOM_LIST_FILE);
        } else {  // List of stories, toggle checkmark
            switch (v.getId()) {
                case R.id.checkedTextView:
                    CheckedTextView ctv = (CheckedTextView) v;
                    dataModel.setChecked(!ctv.isChecked());   // toggle check
                    setCheckmark(ctv, dataModel.getChecked());
                    break;
            }
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

