package org.sil.storyproducer.tools;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.sil.storyproducer.controller.MainActivity;

public class DrawerItemClickListener implements ListView.OnItemClickListener {

    private Context context;

    public DrawerItemClickListener(Context con) {
        context = con;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectItem(position);
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        System.out.println("the position is " + position);
        //TODO add more options
        if(position == 0) {
            Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
            context.startActivity(intent);
        }
    }
}
