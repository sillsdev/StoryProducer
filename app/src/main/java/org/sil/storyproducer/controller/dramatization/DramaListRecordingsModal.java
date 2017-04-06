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
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;

import java.io.File;

public class DramaListRecordingsModal implements RecordingsListAdapter.ClickListeners {

    private Context context;
    private int slidePosition;
    private DramatizationFrag parentFragment;
    LinearLayout rootView;
    AlertDialog dialog;

    String[] dramaTitles;
    String lastNewName;
    String lastOldName;

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
        dialog = builder.create();
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

    @Override
    public void onRowClickListener(String recordingTitle) {
        StorySharedPreferences.setDramatizationForSlideAndStory(recordingTitle, slidePosition, StoryState.getStoryName());
        parentFragment.setRecordingsList();
        parentFragment.setPlayBackPath();
        dialog.dismiss();
    }

    @Override
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

    @Override
    public void onDeleteClickListener(String recordingTitle) {
        AudioFiles.deleteDramatization(StoryState.getStoryName(), slidePosition, recordingTitle);
        createRecordingList();
        if(StorySharedPreferences.getDramatizationForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(recordingTitle)) {        //deleted the selected file
            if(dramaTitles.length > 0) {
                StorySharedPreferences.setDramatizationForSlideAndStory(dramaTitles[dramaTitles.length - 1], slidePosition, StoryState.getStoryName());
            } else {
                StorySharedPreferences.setDramatizationForSlideAndStory("", slidePosition, StoryState.getStoryName());       //no stories to set it to
            }

        }
        parentFragment.setRecordingsList();
        parentFragment.setPlayBackPath();
    }

    @Override
    public AudioFiles.RenameCode onRenameClickListener(String name, String newName) {
        lastOldName = name;
        lastNewName = newName;
        return AudioFiles.renameDramatization(StoryState.getStoryName(), slidePosition, name, newName);
    }

    @Override
    public void onRenameSuccess() {
        createRecordingList();
        if(StorySharedPreferences.getDramatizationForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(lastOldName)) {
            StorySharedPreferences.setDramatizationForSlideAndStory(lastNewName, slidePosition, StoryState.getStoryName());
        }
        parentFragment.setRecordingsList();
        parentFragment.setPlayBackPath();
    }
}
