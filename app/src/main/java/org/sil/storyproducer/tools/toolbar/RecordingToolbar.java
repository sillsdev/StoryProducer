package org.sil.storyproducer.tools.toolbar;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.Space;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.tools.AudioPlayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class RecordingToolbar extends AnimationToolbar {
    private FloatingActionButton fab;
    private LinearLayout toolbar;

    private LinearLayout rootViewToolbarLayout;
    private View rootView;
    private String recordFilePath;
    private Context appContext;
    private Activity activity;

    private ImageButton micButton;
    private ImageButton playButton;
    private ImageButton deleteButton;


    private boolean isRecording;
    private boolean enablePlaybackButton;
    private boolean enableDeleteButton;

    private final int RECORDING_ANIMATION_DURATION = 1500;
    private TransitionDrawable transitionDrawable;
    private Handler colorHandler;
    private Runnable colorHandlerRunnable;
    private boolean isRed = true;
    private MediaRecorder voiceRecorder;
    private AudioPlayer audioPlayer;

    private AuxiliaryMedia auxiliaryMedia;
    private ArrayList<AuxiliaryMedia> list;

    public static class AuxiliaryMedia {
        View viewThatIsPlayingButton;
        int setButtonToDrawableOnStop;
        AudioPlayer playingAudio;

        void stopPlaying() {
            if (playingAudio != null && playingAudio.isAudioPlaying()) {
                playingAudio.stopAudio();
                playingAudio.releaseAudio();
                if (viewThatIsPlayingButton != null) {
                    viewThatIsPlayingButton.setBackgroundResource(setButtonToDrawableOnStop);
                }
            }
        }
    }


    /**
     * @param activity
     * @param rootViewToolbar
     * @param rootView
     * @param enablePlaybackButton
     * @param enableDeleteButton
     * @param recordFilePath
     */
    public RecordingToolbar(Activity activity, View rootViewToolbar, View rootView, boolean enablePlaybackButton, boolean enableDeleteButton, String recordFilePath) {
        super(activity);
        super.initializeToolbar(rootViewToolbar.findViewById(R.id.toolbar_for_recording_fab), rootViewToolbar.findViewById(R.id.toolbar_for_recording_toolbar));

        this.activity = activity;
        this.appContext = activity.getApplicationContext();
        this.rootViewToolbarLayout = (LinearLayout) rootViewToolbar;
        this.rootView = rootView;
        fab = (FloatingActionButton) rootViewToolbar.findViewById(R.id.toolbar_for_recording_fab);
        this.toolbar = (LinearLayout) rootViewToolbar.findViewById(R.id.toolbar_for_recording_toolbar);
        this.enablePlaybackButton = enablePlaybackButton;
        this.enableDeleteButton = enableDeleteButton;
        this.recordFilePath = recordFilePath;
        createToolbar();
        setupRecordingAnimationHandler();
    }

    /**
     * This function is used to stop all the media sources on the toolbar from playing or recording.
     */
    public void stopPlayBackAndRecording() {
        if (isRecording) {
            stopRecording();
            micButton.setBackgroundResource(R.drawable.ic_mic_white);
            //set playback button visible
            if (enableDeleteButton) {
                deleteButton.setVisibility(View.VISIBLE);
            }
            if (enablePlaybackButton) {
                playButton.setVisibility(View.VISIBLE);
            }
        }
        if (audioPlayer != null && audioPlayer.isAudioPlaying()) {
            playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
            audioPlayer.stopAudio();
            audioPlayer.releaseAudio();
        }
    }

    /**
     * This function stops all playback and all auxiliary media.
     */
    private void stopAllPlayBackAndRecording() {
        stopPlayBackAndRecording();
        stopAuxilaryMedia();
    }

    /**
     * This function is used so that other potential media sources outside the toolbar can be
     * stopped if the toolbar is pressed.
     *
     * @param viewThatIsPlayingButton The button that is pressed to activate the media playback.
     * @param setButtonToDrawable     The drawable to set the trigger button to a different drawable.
     * @param playingAudio            The audio source of playbaack.
     */
    public void onToolbarTouchStopAudio(View viewThatIsPlayingButton, int setButtonToDrawable, AudioPlayer playingAudio) {
        auxiliaryMedia = new AuxiliaryMedia();
        auxiliaryMedia.playingAudio = playingAudio;
        auxiliaryMedia.setButtonToDrawableOnStop = setButtonToDrawable;
        auxiliaryMedia.viewThatIsPlayingButton = viewThatIsPlayingButton;
    }

    /**
     * This is like onToolbarTouchStopAudio function, but this takes in multiple audio sources.
     *
     * @param list
     */
    public void onToolbarTouchStopAudio(ArrayList<AuxiliaryMedia> list) {
        this.list = list;
    }

    public void stopAuxilaryMedia() {
        if (list != null) {
            for (AuxiliaryMedia am : list) {
                am.stopPlaying();
            }
        }
        if (auxiliaryMedia != null) {
            auxiliaryMedia.stopPlaying();
        }
    }

    public boolean isOpen() {
        return super.isOpen();
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void closeToolbar() {
        stopPlayBackAndRecording();
        super.close();
    }

    private void startRecording() {
        startAudioRecorder();
        startRecordingAnimation(false, 0);
    }

    private void stopRecording() {
        stopAudioRecorder();
        stopRecordingAnimation();
    }

    private boolean deleteRecording() {
        if (enableDeleteButton) {
            return false;
        } else {
            return false;
        }
    }

    private void createToolbar() {
        setupToolbarButtons();
        setupToolbar();
    }

    private void setupToolbarButtons() {
        rootViewToolbarLayout.removeAllViews();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams spaceLayoutParams = new LinearLayout.LayoutParams(0, 0, 1f);
        spaceLayoutParams.width = 0;
        int[] drawables;
        ImageButton[] imageButtons = new ImageButton[]{new ImageButton(appContext), new ImageButton(appContext), new ImageButton(appContext)};
        //imageButtons = new ImageButton[]{new ImageButton(appContext), new ImageButton(appContext), new ImageButton(appContext)};
        //Index of the button corresponds to the drawables array
        micButton = imageButtons[0];

        if (!enablePlaybackButton && !enableDeleteButton) {
            drawables = new int[]{R.drawable.ic_mic_white};
        } else if (!enablePlaybackButton) {
            drawables = new int[]{R.drawable.ic_mic_white, R.drawable.ic_delete_forever_white_48dp};
            deleteButton = imageButtons[1];
        } else if (!enableDeleteButton) {
            drawables = new int[]{R.drawable.ic_mic_white, R.drawable.ic_play_arrow_white_48dp};
            playButton = imageButtons[1];
        } else {
            drawables = new int[]{R.drawable.ic_mic_white, R.drawable.ic_play_arrow_white_48dp, R.drawable.ic_delete_forever_white_48dp};
            playButton = imageButtons[1];
            deleteButton = imageButtons[2];
        }

        Space spaces = new Space(appContext);
        spaces.setLayoutParams(spaceLayoutParams);
        toolbar.addView(spaces);

        for (int i = 0; i < drawables.length; i++) {
            imageButtons[i].setBackgroundResource(drawables[i]);
            imageButtons[i].setVisibility(View.VISIBLE);
            imageButtons[i].setLayoutParams(layoutParams);
            toolbar.addView(imageButtons[i]);

            spaces = new Space(appContext);
            spaces.setLayoutParams(spaceLayoutParams);
            toolbar.addView(spaces);
        }

        setOnClickListeners();
    }

    private void setupToolbar() {
        RelativeLayout.LayoutParams[] myParams =
                new RelativeLayout.LayoutParams[]{new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT),
                        new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)};
        int[] myRules = new int[]{RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_END, RelativeLayout.ALIGN_PARENT_RIGHT};
        View[] myView = new View[]{fab, toolbar};

        for (int i = 0; i < myParams.length; i++) {
            for (int j = 0; j < myRules.length; j++) {
                myParams[i].addRule(myRules[j], rootView.getId());
            }
            myView[i].setLayoutParams(myParams[i]);
            ((RelativeLayout) rootView).addView(myView[i]);
        }
        //Index corresponds to the myView array
        fab = (FloatingActionButton) myView[0];
        toolbar = (LinearLayout) myView[1];
    }

    private void setOnClickListeners() {
        View.OnClickListener micList = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    stopRecording();
                    micButton.setBackgroundResource(R.drawable.ic_mic_white);
                    if (enableDeleteButton) {
                        deleteButton.setVisibility(View.VISIBLE);
                    }
                    if (enablePlaybackButton) {
                        playButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    stopAllPlayBackAndRecording();
                    startRecording();
                    micButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                    if (enableDeleteButton) {
                        deleteButton.setVisibility(View.INVISIBLE);
                    }
                    if (enablePlaybackButton) {
                        playButton.setVisibility(View.INVISIBLE);
                    }
                }
            }
        };
        View.OnClickListener playList = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioPlayer != null && audioPlayer.isAudioPlaying()) {
                    audioPlayer.releaseAudio();
                    playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                } else {
                    stopAllPlayBackAndRecording();
                    if (new File(recordFilePath).exists()) {
                        audioPlayer = new AudioPlayer();
                        audioPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                audioPlayer.releaseAudio();
                                playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                            }
                        });
                        audioPlayer.playWithPath(recordFilePath);
                        Toast.makeText(appContext, "Playing back recording!", Toast.LENGTH_SHORT).show();
                        playButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                    } else {
                        Toast.makeText(appContext, "No translation recorded!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        View.OnClickListener deleteList = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAllPlayBackAndRecording();

            }
        };
        micButton.setOnClickListener(micList);
        if (enablePlaybackButton) {
            if (new File(recordFilePath).exists()) {
                playButton.setVisibility(View.VISIBLE);
            } else {
                playButton.setVisibility(View.INVISIBLE);
            }
            playButton.setOnClickListener(playList);
        }
        if (enableDeleteButton) {
            deleteButton.setOnClickListener(deleteList);
        }
    }

    /**
     * <a href="https://developer.android.com/reference/android/os/Handler.html">See for handler</a>
     * <br/>
     * <a href="https://developer.android.com/reference/java/lang/Runnable.html">See for runnable</a>
     * <br/>
     * <a href="https://developer.android.com/reference/android/graphics/drawable/TransitionDrawable.html">See for transition Drawable</a>
     * <br/>
     * <br/>
     * Call this function prior to setting the button listener of the record button. E.g.: <br/>
     * setupRecordAnimationHandler();<br/>
     * button.Handler(){}
     * <br/>
     * Essentially the function utilizes a Transition Drawable to interpolate between the red and
     * the toolbar color. (The colors are defined in an array and used in the transition drawable)
     * To schedule the running of the transition drawable a handler and runnable are used.<br/>
     * The handler takes a runnable which schedules the transitiondrawable. The handler function
     * called postDelayed will delay the running of the next Runnable by the passed in value e.g.:
     * colorHandler.postDelayed(runnable goes here, time delay in MS). Make sure that isRed is set
     * to true initially.
     * <br/>
     * Still confused about handlers, runnables, and the MessageQueue?
     * <br/>
     * <a href="http://stackoverflow.com/questions/12877944/what-is-the-relationship-between-looper-handler-and-messagequeue-in-android">See this excellent SO post for more info.</a>
     */
    private void setupRecordingAnimationHandler() {
        int red = Color.rgb(255, 0, 0);
        int colorOfToolbar = Color.rgb(0, 0, 255); /*Arbitrary color value of blue used initially*/

        LinearLayout lin = toolbar;
        Drawable relBackgroundColor = lin.getBackground();
        if (relBackgroundColor instanceof ColorDrawable) {
            colorOfToolbar = ((ColorDrawable) relBackgroundColor).getColor();
        }
        transitionDrawable = new TransitionDrawable(new ColorDrawable[]{
                new ColorDrawable(colorOfToolbar),
                new ColorDrawable(red)
        });
        lin.setBackground(transitionDrawable);

        colorHandler = new Handler();
        colorHandlerRunnable = new Runnable() {
            @Override
            public void run() {
                //Animation to change the toolbar's color while recording
                if (isRed) {
                    transitionDrawable.startTransition(RECORDING_ANIMATION_DURATION);
                    isRed = false;

                } else {
                    transitionDrawable.reverseTransition(RECORDING_ANIMATION_DURATION);
                    isRed = true;
                }
                startRecordingAnimation(true, RECORDING_ANIMATION_DURATION);
            }
        };
    }

    /**
     * This function is used to start the handler to run the runnable.
     * setupRecordinganimationHandler() should be called first before calling this function
     * to initialize the colorHandler and colorHandlerRunnable().
     *
     * @param isDelayed Used to signify that the runnable will be delayed in running.
     * @param delay     The time that will be delayed in ms if isDelayed is true.
     */
    private void startRecordingAnimation(boolean isDelayed, int delay) {
        if (colorHandler != null && colorHandlerRunnable != null) {
            if (isDelayed) {
                colorHandler.postDelayed(colorHandlerRunnable, delay);
            } else {
                colorHandler.post(colorHandlerRunnable);
            }
        }
    }

    /**
     * Stops the animation from continuing. The removeCallbacks function removes all
     * colorHandlerRunnable from the MessageQueue and also resets the toolbar to its original color.
     * (transitionDrawable.resetTransition();)
     */
    private void stopRecordingAnimation() {
        if (colorHandler != null && colorHandlerRunnable != null) {
            colorHandler.removeCallbacks(colorHandlerRunnable);
        }
        if (transitionDrawable != null) {
            transitionDrawable.resetTransition();
        }
    }

    /**
     * The function that aids in starting an audio recorder.
     */
    private void startAudioRecorder() {
        setVoiceRecorder(recordFilePath);
        try {
            isRecording = true;
            voiceRecorder.prepare();
            voiceRecorder.start();
            Toast.makeText(appContext, "Recording voice!", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException | IOException e) {
            Log.e(activity.toString(), e.getMessage());
        }
    }

    /**
     * The function that aids in stopping an audio recorder.
     */
    private void stopAudioRecorder() {
        try {
            isRecording = false;
            //Delay stopping of voiceRecorder to capture all of the voice recorded.
            Thread.sleep(500);
            voiceRecorder.stop();
            Toast.makeText(appContext, "Stopped recording!", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException stopException) {
            Toast.makeText(appContext, "Please record again!", Toast.LENGTH_SHORT).show();
        } catch (InterruptedException e) {
            Log.e(appContext.toString(), e.getMessage());
        }
        voiceRecorder.release();
        voiceRecorder = null;
        //ConcatenateAudioFiles();
    }

    /**
     * This function sets the voice recorder with a new voicerecorder.
     *
     * @param fileName The file to output the voice recordings.
     */
    private void setVoiceRecorder(String fileName) {
        voiceRecorder = new MediaRecorder();

        if (ContextCompat.checkSelfPermission(appContext,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }

        voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        voiceRecorder.setAudioEncodingBitRate(16);
        voiceRecorder.setAudioSamplingRate(44100);
        voiceRecorder.setOutputFile(fileName);
    }
}
