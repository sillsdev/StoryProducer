package org.sil.storyproducer.controller.logging;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.logging.ComChkEntry;
import org.sil.storyproducer.model.logging.DraftEntry;
import org.sil.storyproducer.model.logging.LearnEntry;
import org.sil.storyproducer.model.logging.Log;
import org.sil.storyproducer.model.logging.LogEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

class LogListAdapter extends BaseAdapter {

    private LogEntry[] displayEntries = null;
    private List<LearnEntry> learnEntries = new ArrayList<>();
    private List<DraftEntry> draftEntries = new ArrayList<>();
    private List<ComChkEntry> comChkEntries = new ArrayList<>();

    private final Context context;

    public LogListAdapter(Context c, Log log, int slide) {
        context = c;
        if (log == null) {
            displayEntries = new LogEntry[0];
        } else {
            if (slide < 0) {
                displayEntries = log.toArray(new LogEntry[0]);
            } else {
                for (LogEntry le : log) {
                    if (le.appliesToSlideNum(slide)) {
                        if (le instanceof LearnEntry) {
                            learnEntries.add((LearnEntry) le);
                        } else if (le instanceof DraftEntry) {
                            draftEntries.add((DraftEntry) le);
                        } else if (le instanceof ComChkEntry) {
                            comChkEntries.add((ComChkEntry) le);
                        }
                    }
                }
                Collection<LogEntry> sorter = new TreeSet<>();
                sorter.addAll(learnEntries);
                sorter.addAll(draftEntries);
                sorter.addAll(comChkEntries);
                displayEntries = sorter.toArray(new LogEntry[0]);
            }
        }
    }

    public void updateList(boolean learn, boolean draft, boolean comCheck) {
        Collection<LogEntry> sorter = new TreeSet<>();
        if (learn) {
            sorter.addAll(learnEntries);
        }
        if (draft) {
            sorter.addAll(draftEntries);
        }
        if (comCheck) {
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

        TextView date = convertView.findViewById(R.id.textView_logging_date);
        TextView info = convertView.findViewById(R.id.textView_logging_type);

        LogEntry entry = getItem(position);
        date.setText(entry.getDateTime());
        info.setText(entry.getPhase() + " - " + entry.getDescription());
        convertView.setBackgroundColor(ContextCompat.getColor(context, entry.getColor()));

        return convertView;
    }
}
