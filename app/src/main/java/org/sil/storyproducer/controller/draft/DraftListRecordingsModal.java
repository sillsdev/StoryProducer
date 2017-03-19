package org.sil.storyproducer.controller.draft;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter;

public class DraftListRecordingsModal {

    private Context context;
    private int slidePosition;
    private DraftFrag parentFragment;

    public DraftListRecordingsModal(Context context, int pos, DraftFrag parentFragment) {
        this.context = context;
        this.slidePosition = pos;
        this.parentFragment = parentFragment;
    }

    public void show() {
        LayoutInflater inflater = parentFragment.getActivity().getLayoutInflater();
        LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.draft_recordings_list, null);
        ListView listView = (ListView) layout.findViewById(R.id.recordings_list);
        String[] blah = {"blah1", "blah2", "blah3"};
        RecordingsListAdapter listAdapter = new RecordingsListAdapter(context, blah, slidePosition, parentFragment);
        listView.setAdapter(listAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.draft_recordings_title)
                .setNegativeButton(R.string.cancel, null)
                .setView(layout);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
