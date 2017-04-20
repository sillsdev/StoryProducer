package org.sil.storyproducer.controller.logging;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.file.FileSystem;

import java.util.ArrayList;
import java.util.TreeSet;

public class LogView extends AppCompatActivity {

 //   private int slide=-1;

    private ListView listView = null;

    private static class LogListAdapter extends BaseAdapter {

        private LogEntry[] displayEntries = null;
        private ArrayList<LearnEntry> learnEntries = new ArrayList<>();
        private ArrayList<DraftEntry> draftEntries = new ArrayList<>();
        private ArrayList<ComChkEntry> comChkEntries = new ArrayList<>();

        private final Context context;

        public LogListAdapter(Context c, Log log, int slide){
            context = c;
            if(log == null) {
                displayEntries = new LogEntry[0];
            } else {
                if (slide < 0) {
                    displayEntries = log.toArray(new LogEntry[0]);
                } else {
                    for (LogEntry le : log) {
                        if (slide == le.getSlideNum() || le.getPhase().equals(Phase.Learn)) {
                            if(le.getPhase().equals(Phase.Learn)){
                                learnEntries.add((LearnEntry) le);
                            } else if (le.getPhase().equals(Phase.Draft)){
                                draftEntries.add((DraftEntry) le);
                            } else if (le.getPhase().equals(Phase.CommCheck)){
                                comChkEntries.add((ComChkEntry) le);
                            }
                        }
                    }
                    TreeSet<LogEntry> sorter = new TreeSet<>();
                    sorter.addAll(learnEntries);
                    sorter.addAll(draftEntries);
                    sorter.addAll(comChkEntries);
                    displayEntries = sorter.toArray(new LogEntry[0]);
                }
            }
        }

        public void updateList(boolean learn, boolean draft, boolean comCheck){
            TreeSet<LogEntry> sorter = new TreeSet<>();
            if(learn){
                sorter.addAll(learnEntries);
            }
            if(draft){
                sorter.addAll(draftEntries);
            }
            if(comCheck){
                sorter.addAll(comChkEntries);
            }
            displayEntries = sorter.toArray(new LogEntry[0]);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return displayEntries.length;
        }

        @Override
        public LogEntry getItem(int position) {
            return displayEntries[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context)
                        .inflate(R.layout.log_list_item, parent, false);
            }

            TextView date = (TextView) convertView.findViewById(R.id.textView01);
            TextView info = (TextView) convertView.findViewById(R.id.textView02);

            LogEntry entry = getItem(position);
            date.setText(entry.getDateTime());
            info.setText(entry.getPhase().toString() +" - " + entry.getDescription());
            convertView.setBackgroundColor(ContextCompat.getColor(context, entry.getColor()));

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int slide = -1;
        System.out.println("Creating LogView activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        slide = getIntent().getIntExtra("slide", -1);
        setTitle("Logging - Slide " + slide);
        listView = (ListView) findViewById(R.id.log_list_view);
        Logging.deleteLog("Spanglish", "NotAStory.com");
        Log log = Logging.getLog(FileSystem.getLanguage(), StoryState.getStoryName());
        System.out.println("is log null? "+ (log==null ? "yes" : "no")); //TODO: figure out versioning on serialized classes
        LogListAdapter lla = new LogListAdapter(getApplicationContext(), log, slide);
        listView.setAdapter(lla);

    }

    public static void makeModal(Context c){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(c);
        int slide = StoryState.getCurrentStorySlide();
        LayoutInflater linf = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialoglayout = linf.inflate(R.layout.activity_log_view, null);
        //alertDialog.setTitle("Logs: Slide "+(slide+1)+"\n");

        ListView listView = (ListView) dialoglayout.findViewById(R.id.log_list_view);
        Log log = Logging.getLog(FileSystem.getLanguage(), StoryState.getStoryName());
        final LogListAdapter lla = new LogListAdapter(c, log, StoryState.getCurrentStorySlide());
        listView.setAdapter(lla);
        Toolbar tb = (Toolbar) dialoglayout.findViewById(R.id.toolbar2);
        tb.setTitle("Logs: Slide "+(slide+1));
        ImageButton exit = (ImageButton) dialoglayout.findViewById(R.id.exitButton);
        final CheckBox learnCB = (CheckBox) dialoglayout.findViewById(R.id.LearnCheckBox);
        final CheckBox draftCB = (CheckBox) dialoglayout.findViewById(R.id.DraftCheckBox);
        final CheckBox comChkCB = (CheckBox) dialoglayout.findViewById(R.id.CommunityCheckCheckBox);
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
        alertDialog.setView(dialoglayout);
        final AlertDialog t = alertDialog.create();
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                t.hide();
            }
        });
        t.show();

    }
}
