package org.sil.storyproducer.controller.dramatization;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter;
import org.sil.storyproducer.controller.draft.DraftFrag;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.file.AudioFiles;

import java.io.File;

public class DramaListRecordingsModal implements RecordingsListAdapter.ClickListeners {

    private Context context;
    private int slidePosition;
    private DramatizationFrag parentFragment;
    LinearLayout rootView;

    String[] dramaTitles;

    private static AudioPlayer audioPlayer;

    public DramaListRecordingsModal(Context context, int pos, DramatizationFrag parentFragment) {
        this.context = context;
        this.slidePosition = pos;
        this.parentFragment = parentFragment;
    }

    public void show() {
        LayoutInflater inflater = parentFragment.getActivity().getLayoutInflater();
        rootView = (LinearLayout)inflater.inflate(R.layout.recordings_list, null);

        createRecordingList();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dramatization_recordings_title)
                .setNegativeButton(R.string.cancel, null)
                .setView(rootView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Updates the list of dramatization recordings at beginning of fragment creation and after any list change
     */
    private void createRecordingList() {
        ListView listView = (ListView) rootView.findViewById(R.id.recordings_list);
        listView.setScrollbarFadingEnabled(false);
        dramaTitles = AudioFiles.getDramatizationTitles(StoryState.getStoryName(), slidePosition);
        ListAdapter adapter = new RecordingsListAdapter(context, dramaTitles, slidePosition, this);
        listView.setAdapter(adapter);
    }

    public void onPlayClickListener(String recordingTitle) {
        final File dramaFile = AudioFiles.getDramatization(StoryState.getStoryName(), slidePosition, recordingTitle);
        parentFragment.stopPlayBackAndRecording();
        if (dramaFile.exists()) {
            audioPlayer = new AudioPlayer();
            audioPlayer.playWithPath(dramaFile.getPath());
            Toast.makeText(parentFragment.getContext(), "Playing Dramatization...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(parentFragment.getContext(), "No Dramatization Found...", Toast.LENGTH_SHORT).show();
        }
    }

    public void onDeleteClickListener(int slidePos, String recordingTitle) {
        AudioFiles.deleteDramatization(StoryState.getStoryName(), slidePos, recordingTitle);
        createRecordingList();
    }

    public AudioFiles.RenameCode onRenameClickListener(int slidePos, String name, String newName) {
        return AudioFiles.renameDramatization(StoryState.getStoryName(), slidePos, name, newName);
    }

    public void onRenameSuccess() {
        createRecordingList();
    }
}
