package org.sil.storyproducer.controller.logging;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.sil.storyproducer.R;

public class LogView extends AppCompatActivity {


    private ListView listView = null;

    private class LogListAdapter extends BaseAdapter {

        private LogEntry[] logEntries;

        public LogListAdapter(Log log){
            this.logEntries=log.toArray(logEntries);
        }

        @Override
        public int getCount() {
            return logEntries.length;
        }

        @Override
        public Object getItem(int position) {
            return logEntries[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setTitle("Logging!!!");
        listView = (ListView) findViewById(R.id.log_list_view);
        listView.setAdapter(null);
    }
}
