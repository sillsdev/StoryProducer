package org.sil.storyproducer.controller.draft;

import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.Modal;
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.model.logging.Log;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.LogFiles;
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
    private ImageButton currentPlayingButton;

    public DraftListRecordingsModal(Context context, int pos, DraftFrag parentFragment) {
        this.context = context;
        this.slidePosition = pos;
        this.parentFragment = parentFragment;
        audioPlayer = new AudioPlayer();
    }

    public void show() {
        LayoutInflater inflater = parentFragment.getActivity().getLayoutInflater();
        rootView = (LinearLayout) inflater.inflate(R.layout.recordings_list, null);

        createRecordingList();


        Toolbar tb = rootView.findViewById(R.id.toolbar2);
        //Note that user-facing slide number is 1-based while it is 0-based in code.
        tb.setTitle(R.string.draft_recordings_title);
        ImageButton exit = rootView.findViewById(R.id.exitButton);

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setView(rootView);
        dialog = alertDialog.create();
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    /**
     * Updates the list of draft recordings at beginning of fragment creation and after any list change
     */
    private void createRecordingList() {
        ListView listView = rootView.findViewById(R.id.recordings_list);
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
    public void onPlayClick(String recordingTitle, ImageButton buttonClickedNow) {
        parentFragment.stopPlayBackAndRecording();
        if (audioPlayer.isAudioPlaying() && currentPlayingButton.equals(buttonClickedNow)) {
            currentPlayingButton.setImageResource(R.drawable.ic_green_play);
            audioPlayer.stopAudio();
        } else {
            if (audioPlayer.isAudioPlaying()) {
                currentPlayingButton.setImageResource(R.drawable.ic_green_play);
                audioPlayer.stopAudio();
            }
            currentPlayingButton = buttonClickedNow;
            currentPlayingButton.setImageResource(R.drawable.ic_stop_red);
            audioPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    currentPlayingButton.setImageResource(R.drawable.ic_green_play);
                }
            });
            final File draftFile = AudioFiles.getDraft(StoryState.getStoryName(), slidePosition, recordingTitle);
            if (draftFile.exists()) {
                audioPlayer.setPath(draftFile.getPath());
                audioPlayer.playAudio();
                Toast.makeText(parentFragment.getContext(), context.getString(R.string.draft_playing_draft), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(parentFragment.getContext(), context.getString(R.string.draft_no_draft_found), Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public void onDeleteClick(String recordingTitle) {
        AudioFiles.deleteDraft(StoryState.getStoryName(), slidePosition, recordingTitle);
        createRecordingList();
        if (StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(recordingTitle)) {        //deleted the selected file
            if (draftTitles.length > 0) {
                StorySharedPreferences.setDraftForSlideAndStory(draftTitles[draftTitles.length - 1], slidePosition, StoryState.getStoryName());
            } else {
                StorySharedPreferences.setDraftForSlideAndStory("", slidePosition, StoryState.getStoryName());       //no stories to set it to
                parentFragment.hideButtonsToolbar();
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
        if (StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(lastOldName)) {
            StorySharedPreferences.setDraftForSlideAndStory(lastNewName, slidePosition, StoryState.getStoryName());
        }
        parentFragment.updatePlayBackPath();
    }
}
