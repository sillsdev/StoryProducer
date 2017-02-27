package org.sil.storyproducer.controller.export;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.sil.storyproducer.R;

import java.util.List;

/**
 * TODO: Put javadoc here
 */
public class FileArrayAdapter extends ArrayAdapter<Item>{

    private Context context;
    private int id;
    private List<Item>items;

    public FileArrayAdapter(Context context, int textViewResourceId,
                            List<Item> objects) {
        super(context, textViewResourceId, objects);
        this.context = context;
        id = textViewResourceId;
        items = objects;
    }


    public Item getItem(int i)
    {
        return items.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = li.inflate(id, null);
        }

        final Item item = items.get(position);
        if (item != null) {
            TextView nameField = (TextView) view.findViewById(R.id.TextViewName);
            TextView dataField = (TextView) view.findViewById(R.id.TextViewData);
            TextView dateField = (TextView) view.findViewById(R.id.TextViewDate);

            ImageView imageView = (ImageView) view.findViewById(R.id.fd_Icon1);


            int imageID=R.drawable.ic_vec_file;

            if (! item.isFile()){
                if(item.getName().equals("..")) {
                    //set ID to "up one directory" icon
                    imageID= R.drawable.ic_vec_up_folder;
                } else {
                    //set ID to Folder icon
                    imageID = R.drawable.ic_vec_folder;
                }
            }

            //set the image based on the ID
            Drawable image = ContextCompat.getDrawable(context, imageID);
            imageView.setImageDrawable(image);

            if(nameField != null)
                nameField.setText(item.getName());
            if(dataField != null)
                dataField.setText(item.getData());
            if(dateField != null)
                dateField.setText(item.getDate());
        }
        return view;
    }
}