package org.sil.storyproducer.controller.dramatization;

import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter;
import org.sil.storyproducer.controller.Modal;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;

import java.io.File;

public class DramaListRecordingsModal implements RecordingsListAdapter.ClickListeners, Modal {

    private Context context;
    private int slidePosition;
    private DramatizationFrag parentFragment;
    private LinearLayout rootView;
    private AlertDialog dialog;

    private String[] dramaTitles;
    private String lastNewName;
    private String lastOldName;

    private static AudioPlayer audioPlayer;
    private ImageButton currentPlayingButton;

    public DramaListRecordingsModal(Context context, int pos, DramatizationFrag parentFragment) {
        this.context = context;
        this.slidePosition = pos;
        this.parentFragment = parentFragment;
        audioPlayer = new AudioPlayer();
    }

    @Override
    public void show() {
        LayoutInflater inflater = parentFragment.getActivity().getLayoutInflater();
        rootView = (LinearLayout) inflater.inflate(R.layout.recordings_list, null);

        createRecordingList();

        Toolbar tb = (Toolbar) rootView.findViewById(R.id.toolbar2);
        //Note that user-facing slide number is 1-based while it is 0-based in code.
        tb.setTitle(R.string.dramatization_recordings_title);
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
     * Updates the list of dramatization recordings at beginning of fragment creation and after any list change
     */
    private void createRecordingList() {
        ListView listView = (ListView) rootView.findViewById(R.id.recordings_list);
        listView.setScrollbarFadingEnabled(false);
        dramaTitles = AudioFiles.getDramatizationTitles(StoryState.getStoryName(), slidePosition);
        RecordingsListAdapter adapter = new RecordingsListAdapter(context, dramaTitles, slidePosition, this);
        adapter.setDeleteTitle(context.getResources().getString(R.string.delete_dramatize_title));
        adapter.setDeleteMessage(context.getResources().getString(R.string.delete_dramatize_message));
        listView.setAdapter(adapter);
    }

    @Override
    public void onRowClick(String recordingTitle) {
        String previousTitle = StorySharedPreferences.getDramatizationForSlideAndStory(slidePosition, StoryState.getStoryName());
        if(!previousTitle.equals(recordingTitle)){
            StorySharedPreferences.setDramatizationForSlideAndStory(recordingTitle, slidePosition, StoryState.getStoryName());
            parentFragment.setPlayBackPath();
            parentFragment.stopAppendingRecordingFile();
        }

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
            final File dramaFile = AudioFiles.getDramatization(StoryState.getStoryName(), slidePosition, recordingTitle);
            if (dramaFile.exists()) {
                audioPlayer.setPath(dramaFile.getPath());
                audioPlayer.playAudio();
                Toast.makeText(parentFragment.getContext(), context.getString(R.string.dramatization_playing_dramatize), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(parentFragment.getContext(), context.getString(R.string.dramatization_no_drama_found), Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public void onDeleteClick(String recordingTitle) {
        AudioFiles.deleteDramatization(StoryState.getStoryName(), slidePosition, recordingTitle);
        createRecordingList();
        if (StorySharedPreferences.getDramatizationForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(recordingTitle)) {        //deleted the selected file
            if (dramaTitles.length > 0) {
                StorySharedPreferences.setDramatizationForSlideAndStory(dramaTitles[dramaTitles.length - 1], slidePosition, StoryState.getStoryName());
            } else {
                StorySharedPreferences.setDramatizationForSlideAndStory("", slidePosition, StoryState.getStoryName());       //no stories to set it to
                parentFragment.hideButtonsToolbar();
            }

        }
        parentFragment.setPlayBackPath();
    }

    @Override
    public AudioFiles.RenameCode onRenameClick(String name, String newName) {
        lastOldName = name;
        lastNewName = newName;
        return AudioFiles.renameDramatization(StoryState.getStoryName(), slidePosition, name, newName);
    }

    @Override
    public void onRenameSuccess() {
        createRecordingList();
        if (StorySharedPreferences.getDramatizationForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(lastOldName)) {
            StorySharedPreferences.setDramatizationForSlideAndStory(lastNewName, slidePosition, StoryState.getStoryName());
        }
        parentFragment.setPlayBackPath();
    }
}
