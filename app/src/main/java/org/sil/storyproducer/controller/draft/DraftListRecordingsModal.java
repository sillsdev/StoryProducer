package org.sil.storyproducer.controller.draft;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;

import java.io.File;

public class DraftListRecordingsModal implements RecordingsListAdapter.ClickListeners {

    private Context context;
    private int slidePosition;
    private DraftFrag parentFragment;
    LinearLayout rootView;

    String[] draftTitles;

    private static AudioPlayer audioPlayer;

    public DraftListRecordingsModal(Context context, int pos, DraftFrag parentFragment) {
        this.context = context;
        this.slidePosition = pos;
        this.parentFragment = parentFragment;
    }

    public void show() {
        LayoutInflater inflater = parentFragment.getActivity().getLayoutInflater();
        rootView = (LinearLayout)inflater.inflate(R.layout.recordings_list, null);

        createRecordingList();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.draft_recordings_title)
                .setNegativeButton(R.string.cancel, null)
                .setView(rootView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Updates the list of draft recordings at beginning of fragment creation and after any list change
     */
    private void createRecordingList() {
        ListView listView = (ListView) rootView.findViewById(R.id.recordings_list);
        listView.setScrollbarFadingEnabled(false);
        draftTitles = AudioFiles.getDraftTitles(StoryState.getStoryName(), slidePosition);
        ListAdapter adapter = new RecordingsListAdapter(context, draftTitles, slidePosition, this);
        listView.setAdapter(adapter);
    }

    public void onPlayClickListener(String recordingTitle) {
        final File draftFile = AudioFiles.getDraft(StoryState.getStoryName(), slidePosition, recordingTitle);
        parentFragment.stopPlayBackAndRecording();
        if (draftFile.exists()) {
            audioPlayer = new AudioPlayer();
            audioPlayer.playWithPath(draftFile.getPath());
            Toast.makeText(parentFragment.getContext(), "Playing Draft...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(parentFragment.getContext(), "No Draft Found...", Toast.LENGTH_SHORT).show();
        }
    }

    public void onDeleteClickListener(int slidePos, String recordingTitle) {
        AudioFiles.deleteDraft(StoryState.getStoryName(), slidePos, recordingTitle);
        createRecordingList();
    }

    public AudioFiles.RenameCode onRenameClickListener(int slidePos, String name, String newName) {
        return AudioFiles.renameDraft(StoryState.getStoryName(), slidePos, name, newName);
    }

    public void onRenameSuccess() {
        createRecordingList();
    }
}
