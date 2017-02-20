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
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = vi.inflate(id, null);
        }

        final Item item = items.get(position);
        if (item != null) {
            TextView t1 = (TextView) view.findViewById(R.id.TextView01);
            TextView t2 = (TextView) view.findViewById(R.id.TextView02);
            TextView t3 = (TextView) view.findViewById(R.id.TextViewDate);

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

            if(t1!=null)
                t1.setText(item.getName());
            if(t2!=null)
                t2.setText(item.getData());
            if(t3!=null)
                t3.setText(item.getDate());
        }
        return view;
    }
}