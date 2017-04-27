package org.sil.storyproducer.controller.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.ListFiles;

public class CustomAdapter extends ArrayAdapter<ListFiles> {
    private Context context;
    private int layoutResourceId;
    private ListFiles data[] = null;

    public CustomAdapter(Context context, int layoutResourceId, ListFiles[] data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        FileHolder holder;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new FileHolder();
            holder.imgIcon = (ImageView)row.findViewById(R.id.story_list_image);
            holder.txtTitle = (TextView)row.findViewById(R.id.story_list_title);
            holder.txtSubTitle = (TextView)row.findViewById(R.id.story_list_subtitle);
            ImageView playButton = (ImageView)row.findViewById(R.id.story_list_play);
            playButton.setVisibility(View.INVISIBLE);
            row.setTag(holder);
        } else {
            holder = (FileHolder)row.getTag();
        }

        ListFiles listFiles = data[position];
        holder.txtTitle.setText(listFiles.title);
        holder.imgIcon.setImageBitmap(listFiles.icon);
        holder.txtSubTitle.setText(listFiles.subtitle);

        return row;
    }

    static class FileHolder {
        ImageView imgIcon;
        TextView txtTitle;
        TextView txtSubTitle;
    }

}