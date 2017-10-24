package org.sil.storyproducer.controller.remote;

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
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;

import java.io.File;

/**
 * modal class for a backtranslation phase. part of remote consultant phase
 *
 */

public class BackTranslationListRecordingsModal implements RecordingsListAdapter.ClickListeners, Modal {

    private Context context;
    private int slidePosition;
    private BackTranslationFrag parentFragment;
    private LinearLayout rootView;
    private AlertDialog dialog;

    private String[] backT_Titles;
    private String lastNewName;
    private String lastOldName;

    private static AudioPlayer audioPlayer;
    private ImageButton currentPlayingButton;


    public BackTranslationListRecordingsModal(Context context, int pos, BackTranslationFrag parentFragment) {
        this.context = context;
        this.slidePosition = pos;
        this.parentFragment = parentFragment;
        audioPlayer = new AudioPlayer();
    }

    public void show() {
        LayoutInflater inflater = parentFragment.getActivity().getLayoutInflater();
        rootView = (LinearLayout) inflater.inflate(R.layout.recordings_list, null);

        createRecordingList();

        Toolbar tb = (Toolbar) rootView.findViewById(R.id.toolbar2);
        //Note that user-facing slide number is 1-based while it is 0-based in code.
        tb.setTitle(R.string.backTranslation_recordings_title);
        ImageButton exit = (ImageButton) rootView.findViewById(R.id.exitButton);


        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
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
     * Updates the list of backtranslation recordings at beginning of fragment creation and after any list change
     */
    private void createRecordingList() {
        ListView listView = (ListView) rootView.findViewById(R.id.recordings_list);
        listView.setScrollbarFadingEnabled(false);
        backT_Titles = AudioFiles.getBackTranslationTitles(StoryState.getStoryName(), slidePosition);
        RecordingsListAdapter adapter = new RecordingsListAdapter(context, backT_Titles, slidePosition, this);
        adapter.setDeleteTitle(context.getResources().getString(R.string.delete_backT_message));
        adapter.setDeleteMessage(context.getResources().getString(R.string.delete_backT_message));
        listView.setAdapter(adapter);
    }


    @Override
    public void onRowClick(String recordingTitle) {
        StorySharedPreferences.setBackTranslationForSlideAndStory(recordingTitle, slidePosition, StoryState.getStoryName());
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
            final File backT_file = AudioFiles.getBackTranslation(StoryState.getStoryName(), slidePosition, recordingTitle);
            if (backT_file.exists()) {
                audioPlayer.setPath(backT_file.getPath());
                audioPlayer.playAudio();
                Toast.makeText(parentFragment.getContext(), context.getString(R.string.backTranslation_playing_backT), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(parentFragment.getContext(), context.getString(R.string.backTranslation_no_backT_found), Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public void onDeleteClick(String recordingTitle) {
        AudioFiles.deleteBackTranslation(StoryState.getStoryName(), slidePosition, recordingTitle);
        createRecordingList();
        if (StorySharedPreferences.getBackTranslationForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(recordingTitle)) {        //deleted the selected file
            if (backT_Titles.length > 0) {
                StorySharedPreferences.setBackTranslationForSlideAndStory(backT_Titles[backT_Titles.length - 1], slidePosition, StoryState.getStoryName());
            } else {
                StorySharedPreferences.setBackTranslationForSlideAndStory("", slidePosition, StoryState.getStoryName());       //no stories to set it to
                parentFragment.hideButtonsToolbar();
            }

        }
        parentFragment.setPlayBackPath();
    }


    @Override
    public AudioFiles.RenameCode onRenameClick(String name, String newName) {
        lastOldName = name;
        lastNewName = newName;
        return AudioFiles.renameBackTranslation(StoryState.getStoryName(), slidePosition, name, newName);
    }

    @Override
    public void onRenameSuccess() {
        createRecordingList();
        if (StorySharedPreferences.getBackTranslationForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(lastOldName)) {
            StorySharedPreferences.setBackTranslationForSlideAndStory(lastNewName, slidePosition, StoryState.getStoryName());
        }
        parentFragment.setPlayBackPath();
    }
}


