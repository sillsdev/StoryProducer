package org.sil.storyproducer.controller.export;

import android.app.Activity;
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

public class FileArrayAdapter extends ArrayAdapter<Item>{

    private Context c;
    private int id;
    private List<Item>items;

    public FileArrayAdapter(Context context, int textViewResourceId,
                            List<Item> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
    }
    public Item getItem(int i)
    {
        return items.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

               /* create a new view of my activity_file_explorer and inflate it in the row */
        //convertView = ( RelativeLayout ) inflater.inflate( resource, null );

        final Item o = items.get(position);
        if (o != null) {
            TextView t1 = (TextView) v.findViewById(R.id.TextView01);
            TextView t2 = (TextView) v.findViewById(R.id.TextView02);
            TextView t3 = (TextView) v.findViewById(R.id.TextViewDate);
                       /* Take the ImageView from activity_file_explorer and set the city's image */
            ImageView imageCity = (ImageView) v.findViewById(R.id.fd_Icon1);
           // String uri = "drawable/" + o.getImage();
            //int imageResource = c.getResources().getIdentifier(uri, null, c.getPackageName());
            //Drawable image = c.getResources().getDrawable(imageResource);


            int imageID=R.drawable.ic_vec_file;

            if (! o.isFile()){
                if(o.getName().equals("..")) {
                    //set ID to "up one directory" icon
                    imageID= R.drawable.ic_vec_up_folder;
                } else {
                    //set ID to Folder icon
                    imageID = R.drawable.ic_vec_folder;
                }
            }

            //set the image based on the ID
            Drawable image = ContextCompat.getDrawable(c, imageID);
            imageCity.setImageDrawable(image);

            if(t1!=null)
                t1.setText(o.getName());
            if(t2!=null)
                t2.setText(o.getData());
            if(t3!=null)
                t3.setText(o.getDate());
        }
        return v;
    }
}