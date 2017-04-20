package org.sil.storyproducer.tools.toolbar;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.RelativeLayout;

import org.sil.storyproducer.controller.draft.Modal;


/**
 *
 */
public class PausingRecordToolbar extends RecordingToolbar{
    private AudioRecord audioRecord;

    public PausingRecordToolbar(Activity activity, View rootViewToolbarLayout, RelativeLayout rootViewLayout,
                                boolean enablePlaybackButton, boolean enableDeleteButton, boolean enableMultiRecordButton,
                                String playbackRecordFilePath, String recordFilePath, Modal multiRecordModal, RecordingListener recordingListener) throws ClassCastException {
        super(activity, rootViewToolbarLayout, rootViewLayout, enablePlaybackButton, enableDeleteButton, enableMultiRecordButton, playbackRecordFilePath, recordFilePath, multiRecordModal, recordingListener);

    }

    @Override
    protected void setupToolbarButtons(){

    }

    @Override
    protected void startAudioRecorder(){

    }

    @Override
    protected void stopAudioRecorder(){

    }

    private void createNewAudioRecord(){
        if (ContextCompat.checkSelfPermission(currentActivity,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(currentActivity,
                    new String[]{ Manifest.permission.RECORD_AUDIO}, 1);
        }



    }


}
