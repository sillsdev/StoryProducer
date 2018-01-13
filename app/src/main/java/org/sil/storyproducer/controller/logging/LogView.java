package org.sil.storyproducer.controller.logging;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.model.logging.Log;
import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.LogFiles;

public class LogView {

    //TODO: figure out versioning on serialized classes?
    public static void makeModal(Context c){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(c);
        int slide = StoryState.getCurrentStorySlide();
        LayoutInflater linf = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogLayout = linf.inflate(R.layout.activity_log_view, null);

        ListView listView = (ListView) dialogLayout.findViewById(R.id.log_list_view);
        Log log = LogFiles.getLog(FileSystem.getLanguage(), StoryState.getStoryName());
        final LogListAdapter lla = new LogListAdapter(c, log, StoryState.getCurrentStorySlide());
        listView.setAdapter(lla);
        Toolbar tb = (Toolbar) dialogLayout.findViewById(R.id.toolbar2);
        //Note that user-facing slide number is 1-based while it is 0-based in code.
        tb.setTitle(c.getString(R.string.logging_slide_log_view_title, slide));
        ImageButton exit = (ImageButton) dialogLayout.findViewById(R.id.exitButton);
        final CheckBox learnCB = (CheckBox) dialogLayout.findViewById(R.id.LearnCheckBox);
        final CheckBox draftCB = (CheckBox) dialogLayout.findViewById(R.id.DraftCheckBox);
        final CheckBox comChkCB = (CheckBox) dialogLayout.findViewById(R.id.CommunityCheckCheckBox);
        learnCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                lla.updateList(checked, draftCB.isChecked(), comChkCB.isChecked());
            }
        });
        draftCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                lla.updateList(learnCB.isChecked(), checked, comChkCB.isChecked());
            }
        });
        comChkCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                lla.updateList(learnCB.isChecked(), draftCB.isChecked(), checked);
            }
        });
        alertDialog.setView(dialogLayout);
        final AlertDialog t = alertDialog.create();
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                t.dismiss();
            }
        });
        t.show();

    }
}
