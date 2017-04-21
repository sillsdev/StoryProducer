package org.sil.storyproducer.controller.export;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import org.sil.storyproducer.R;

import java.util.ArrayList;
import java.util.List;

public class ExportedVideosAdapter extends BaseAdapter {

    private List<String> videoPaths = new ArrayList<>();

    private Context context;
    private LayoutInflater mInflater;

    public ExportedVideosAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.context = context;
    }

    public void setVideoPaths(List<String> paths) {
        videoPaths = paths;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return videoPaths.size();
    }

    @Override
    public String getItem(int position) {
        return videoPaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View rowView, ViewGroup parent) {
        RowViewHolder holder = null;

        final String path = videoPaths.get(position);

        //split the path so we can get just the file name witch will be used in the view
        String[] splitPath = path.split("/");
        final String fileName = splitPath[splitPath.length - 1];

        //recreate the holder every time because the views are changing around
        holder = new RowViewHolder();
        rowView = mInflater.inflate(R.layout.exported_video_row, null);
        holder.textView = (TextView) rowView.findViewById(R.id.video_title);
        holder.fileViewButton = (ImageButton) rowView.findViewById(R.id.file_view_button);
        holder.shareButton = (ImageButton) rowView.findViewById(R.id.file_share_button);

        //set the two different button listners
        holder.fileViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pathNotName = path.replace("/" + fileName, "");
                showViewFileChooser(pathNotName);
            }
        });
        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShareFileChooser(path, fileName);
            }
        });
        rowView.setTag(holder);

        holder.textView.setText(fileName);

        return rowView;
    }

    public static class RowViewHolder {
        public TextView textView;
        public ImageButton fileViewButton;
        public ImageButton shareButton;
    }

    private void showViewFileChooser(String path) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_VIEW);
        shareIntent.setDataAndType(Uri.parse("file://" + path), "resource/folder");
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.file_view)));
    }

    private void showShareFileChooser(String path, String fileName) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("video/*");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, fileName);
        shareIntent.putExtra(android.content.Intent.EXTRA_TITLE, fileName);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + path));
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.send_video)));
    }

}
