package org.sil.storyproducer.controller.draft;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.Modal;
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;

import java.io.File;

public class DraftListRecordingsModal implements RecordingsListAdapter.ClickListeners, Modal {

    private Context context;
    private int slidePosition;
    private DraftFrag parentFragment;
    private LinearLayout rootView;
    private AlertDialog dialog;

    private String[] draftTitles;
    private String lastNewName;
    private String lastOldName;

    private AudioPlayer audioPlayer;

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
        dialog = builder.create();
        dialog.show();
    }

    /**
     * Updates the list of draft recordings at beginning of fragment creation and after any list change
     */
    private void createRecordingList() {
        ListView listView = (ListView) rootView.findViewById(R.id.recordings_list);
        listView.setScrollbarFadingEnabled(false);
        draftTitles = AudioFiles.getDraftTitles(StoryState.getStoryName(), slidePosition);
        RecordingsListAdapter adapter = new RecordingsListAdapter(context, draftTitles, slidePosition, this);
        adapter.setDeleteTitle(context.getResources().getString(R.string.delete_draft_title));
        adapter.setDeleteMessage(context.getResources().getString(R.string.delete_draft_message));
        listView.setAdapter(adapter);
    }

    @Override
    public void onRowClick(String recordingTitle) {
        StorySharedPreferences.setDraftForSlideAndStory(recordingTitle, slidePosition, StoryState.getStoryName());
        parentFragment.updatePlayBackPath();
        dialog.dismiss();
    }

    @Override
    public void onPlayClick(String recordingTitle) {
        final File draftFile = AudioFiles.getDraft(StoryState.getStoryName(), slidePosition, recordingTitle);
        parentFragment.stopPlayBackAndRecording();
        if (draftFile.exists()) {
            audioPlayer = new AudioPlayer();
            audioPlayer.playWithPath(draftFile.getPath());
            Toast.makeText(parentFragment.getContext(), context.getString(R.string.draft_playing_draft), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(parentFragment.getContext(), context.getString(R.string.draft_no_draft_found), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(String recordingTitle) {
        AudioFiles.deleteDraft(StoryState.getStoryName(), slidePosition, recordingTitle);
        createRecordingList();
        if(StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(recordingTitle)) {        //deleted the selected file
            if(draftTitles.length > 0) {
                StorySharedPreferences.setDraftForSlideAndStory(draftTitles[draftTitles.length - 1], slidePosition, StoryState.getStoryName());
            } else {
                StorySharedPreferences.setDraftForSlideAndStory("", slidePosition, StoryState.getStoryName());       //no stories to set it to
            }

        }
        parentFragment.updatePlayBackPath();
    }

    @Override
    public AudioFiles.RenameCode onRenameClick(String name, String newName) {
        lastOldName = name;
        lastNewName = newName;
        return AudioFiles.renameDraft(StoryState.getStoryName(), slidePosition, name, newName);
    }

    @Override
    public void onRenameSuccess() {
        createRecordingList();
        if(StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(lastOldName)) {
            StorySharedPreferences.setDraftForSlideAndStory(lastNewName, slidePosition, StoryState.getStoryName());
        }
        parentFragment.updatePlayBackPath();
    }
}
