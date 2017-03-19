package org.sil.storyproducer.controller.logging;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.file.FileSystem;

import java.util.ArrayList;

public class LogView extends AppCompatActivity {

    private int slide=-1;

    private ListView listView = null;

    private class LogListAdapter extends BaseAdapter {

        private LogEntry[] logEntries = null;

        private final Context context;

        public LogListAdapter(Context c, Log log){
            if(log != null) {
                if (slide >= 0) {
                    ArrayList<LogEntry> filteredEntries = new ArrayList<>();
                    for (LogEntry l : log) {
                        if (slide == l.getSlideNum()) {
                            filteredEntries.add(l);
                        }
                    }
                    this.logEntries = filteredEntries.toArray(new LogEntry[0]);
                } else {
                    this.logEntries = log.toArray(new LogEntry[0]);
                }
            } else {
                logEntries = new LogEntry[0];
            }
            context = c;
        }

        public void updateList(Log l){
            this.logEntries = l.toArray(logEntries);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return logEntries.length;
        }

        @Override
        public LogEntry getItem(int position) {
            return logEntries[position];
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

            //ImageView bananaView = (ImageView) convertView.findViewById(R.id.banana);
            TextView date = (TextView) convertView.findViewById(R.id.textView01);
            TextView info = (TextView) convertView.findViewById(R.id.textView02);

            LogEntry entry = getItem(position);
            date.setText(entry.getDateTime());
            info.setText(entry.getPhase().toString() +" - " + entry.getTypeString());

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        LogListAdapter lla = new LogListAdapter(getApplicationContext(), log);
        listView.setAdapter(lla);

    }
}
