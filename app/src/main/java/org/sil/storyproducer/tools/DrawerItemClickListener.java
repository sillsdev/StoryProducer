package org.sil.storyproducer.tools;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.sil.storyproducer.controller.MainActivity;
import org.sil.storyproducer.controller.RegistrationActivity;
import org.sil.storyproducer.controller.logging.LogView;
import org.sil.storyproducer.model.StoryState;

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
        Intent intent;
        //TODO add more options
        switch(position) {
            case 0:
                intent = new Intent(context.getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                break;
            case 1:
                intent = new Intent(context, RegistrationActivity.class);
                context.startActivity(intent);
                break;
            default:
        }
    }
}
