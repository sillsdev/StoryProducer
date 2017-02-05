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

public class LogView extends AppCompatActivity {


    private ListView listView = null;

    private class LogListAdapter extends BaseAdapter {

        private LogEntry[] logEntries = new LogEntry[0];

        private final Context context;

        public LogListAdapter(Context c, Log log){
            this.logEntries=log.toArray(logEntries);
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
            TextView text2 = (TextView) convertView.findViewById(R.id.textView02);

            LogEntry entry = getItem(position);
            date.setText(entry.getDateTime());
            text2.setText(entry.getPhase().toString());

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setTitle("Logging!!!");
        listView = (ListView) findViewById(R.id.log_list_view);
        Logging.deleteLog("Spanglish", "NotAStory.com");
        Logging.createFakeLogEntries("Spanglish", "NotAStory.com", 3);
        Log log = Logging.getLog("Spanglish", "NotAStory.com");
        System.out.println("log, broha: "+log);
        LogListAdapter lla = new LogListAdapter(getApplicationContext(), log);
        listView.setAdapter(lla);

    }
}
