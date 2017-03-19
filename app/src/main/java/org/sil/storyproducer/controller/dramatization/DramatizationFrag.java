package org.sil.storyproducer.controller.dramatization;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.toolbar.AnimationToolbar;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.ImageFiles;

import java.io.IOException;


public class DramatizationFrag extends Fragment {
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";

    private View rootView;
    private int slideNumber;
    private AnimationToolbar myToolbar;
    private boolean isRecording = false;
    private ImageButton playPauseDraftButton;
    private AudioPlayer draftPlayer;
    private String draftPlayerPath = null;
    private MediaRecorder voiceRecorder;
    private AudioPlayer dramatizationPlayer;
    private String dramatizationRecordingPath = null;


    private TransitionDrawable transitionDrawable;
    private Handler colorHandler;
    private Runnable colorHandlerRunnable;
    private boolean isRed = true;
    private final int RECORDING_ANIMATION_DURATION = 1500;
    private ImageButton recordButton;
    private ImageButton playRecordingButton;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Bundle passedArgs = this.getArguments();
        slideNumber = passedArgs.getInt(SLIDE_NUM);
        if (AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).exists()) {
            draftPlayerPath =  AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).getPath();
        }
        dramatizationRecordingPath = AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber).getPath();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dramatization, container, false);
        setUiColors();
        setPic(rootView.findViewById(R.id.fragment_dramatization_image_view), slideNumber);
        setPlayStopDraftButton(rootView.findViewById(R.id.fragment_dramatization_play_draft_button));
        setToolbar();

        return rootView;
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                stopPlayBackAndRecording();
                if (myToolbar != null && myToolbar.isOpen()) {
                    myToolbar.close();
                }
            }
        }
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors() {
        if (slideNumber == 0) {
            RelativeLayout rl = (RelativeLayout) rootView.findViewById(R.id.fragment_dramatization_root_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function is used to the set the picture per slide.
     *
     * @param aView    The view that will have the picture rendered on it.
     * @param slideNum The respective slide number for the dramatization slide.
     */
    private void setPic(View aView, int slideNum) {
        if (aView == null || !(aView instanceof ImageView)) {
            return;
        }

        ImageView slideImage = (ImageView) aView;
        Bitmap slidePicture = ImageFiles.getBitmap(StoryState.getStoryName(), slideNum);

        if (slidePicture == null) {
            Snackbar.make(rootView, "Could Not Find Picture...", Snackbar.LENGTH_SHORT).show();
        }

        //Get the height of the phone.
        DisplayMetrics phoneProperties = getContext().getResources().getDisplayMetrics();
        int height = phoneProperties.heightPixels;
        double scalingFactor = 0.4;
        height = (int) (height * scalingFactor);

        //scale bitmap
        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture, height);

        //Set the height of the image view
        slideImage.getLayoutParams().height = height;
        slideImage.requestLayout();

        slideImage.setImageBitmap(slidePicture);
    }

    /**
     * This function serves to set the play and stop button for the draft playback button.
     */
    private void setPlayStopDraftButton(View playPauseDraftBut) {
        View button = playPauseDraftBut;
        if (button != null && button instanceof ImageButton) {
            playPauseDraftButton = (ImageButton) button;
        }
        if (draftPlayerPath == null) {
            //draft recording does not exist
            playPauseDraftButton.setAlpha(0.8f);
            playPauseDraftButton.setColorFilter(Color.argb(200, 200, 200, 200));
        }else{
            //remove x mark from Imagebutton play
            playPauseDraftButton.setImageResource(0);
        }

        draftPlayer = new AudioPlayer();

        playPauseDraftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(draftPlayerPath == null){
                    Toast.makeText(getContext(), "Draft recording not available!", Toast.LENGTH_SHORT).show();
                }
                else if (draftPlayer.isAudioPlaying()) {
                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                    draftPlayer.stopAudio();
                    draftPlayer.releaseAudio();
                } else {
                    stopPlayBackAndRecording();
                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                    draftPlayer = new AudioPlayer();
                    draftPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            playPauseDraftButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                            draftPlayer.releaseAudio();
                        }
                    });
                    draftPlayer.playWithPath(draftPlayerPath);
                    Toast.makeText(getContext(), "Playing back draft recording!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private void setToolbar() {
        setupToolbarAndRecordAnim(rootView.findViewById(R.id.fragment_dramatization_fab),
                rootView.findViewById(R.id.fragment_dramatization_animated_toolbar));
        setRecordNPlayback();
    }

    /**
     * This function initializes the animated toobar and click of dummyView to close toolbar.
     */
    private void setupToolbarAndRecordAnim(View fab, View relativeLayout) {
        if (fab == null || relativeLayout == null) {
            return;
        }
        try {
            myToolbar = new AnimationToolbar(fab, relativeLayout, this.getActivity());
        } catch (ClassCastException ex) {
            Log.e(getActivity().toString(), ex.getMessage());
        }

        //The following allows for a touch from user to close the toolbar and make the fab visible.
        //This also stops the recording animation
        LinearLayout dummyView = (LinearLayout) rootView.findViewById(R.id.fragment_dramatization_dummyView);
        dummyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myToolbar != null && myToolbar.isOpen() && !isRecording) {
                    myToolbar.close();
                }
            }
        });

        //This function must be called before using record animations i.e. calling
        //setRecordNPlayback()
        setupRecordingAnimationHandler();
    }

    /**
     * This function sets the recording and playback buttons (The mic and play button) with their
     * respective functionalities.
     */
    private void setRecordNPlayback() {
        View button = rootView.findViewById(R.id.fragment_dramatization_mic_toolbar_button);
        View button2 = rootView.findViewById(R.id.fragment_dramatization_play_toolbar_button);
        dramatizationPlayer = new AudioPlayer();
        if (button instanceof ImageButton && button2 instanceof ImageButton) {
            recordButton = (ImageButton) button;
            playRecordingButton = (ImageButton) button2;
            if ( AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber).exists()) {
                playRecordingButton.setVisibility(View.VISIBLE);
            }

            recordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isRecording) {
                        playRecordingButton.setVisibility(View.VISIBLE);
                        stopAudioRecorder();
                        stopRecordingAnimation();
                        recordButton.setBackgroundResource(R.drawable.ic_mic_white);
                    } else {
                        stopPlayBackAndRecording();
                        recordButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                        startAudioRecorder();
                        startRecordingAnimation(false, 0);
                    }
                }
            });

            playRecordingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dramatizationPlayer.isAudioPlaying()) {
                        dramatizationPlayer.stopAudio();
                        dramatizationPlayer.releaseAudio();
                        playRecordingButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                    } else {
                        stopPlayBackAndRecording();
                        dramatizationPlayer = new AudioPlayer();
                        dramatizationPlayer.playWithPath(dramatizationRecordingPath);
                        playRecordingButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                        //on completion of audio playback without user intervention
                        //set the button to a play button
                        dramatizationPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                playRecordingButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                                dramatizationPlayer.releaseAudio();
                            }
                        });

                    }
                }
            });
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
     * colorHandler.postDelayed(runnable goes here, time delay in MS).
     * <br/>
     * Still confused about handlers, runnables, and the MessageQueue?
     * <br/>
     * <a href="http://stackoverflow.com/questions/12877944/what-is-the-relationship-between-looper-handler-and-messagequeue-in-android">See this excellent SO post for more info.</a>
     */
    private void setupRecordingAnimationHandler() {
        int red = Color.rgb(255, 0, 0);
        int colorOfToolbar = Color.rgb(0, 0, 255); /*Arbitrary color value of blue used initially*/

        RelativeLayout rel = (RelativeLayout) rootView.findViewById(R.id.fragment_dramatization_animated_toolbar);
        Drawable relBackgroundColor = rel.getBackground();
        if (relBackgroundColor instanceof ColorDrawable) {
            colorOfToolbar = ((ColorDrawable) relBackgroundColor).getColor();
        }
        transitionDrawable = new TransitionDrawable(new ColorDrawable[]{
                new ColorDrawable(colorOfToolbar),
                new ColorDrawable(red)
        });
        rel.setBackground(transitionDrawable);

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
        setVoiceRecorder(dramatizationRecordingPath);
        try {
            isRecording = true;
            voiceRecorder.prepare();
            voiceRecorder.start();
            Toast.makeText(getContext(), "Recording voice!", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException | IOException e) {
            Log.e(getActivity().toString(), e.getMessage());
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
            Toast.makeText(getContext(), "Stopped recording!", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException stopException) {
            Toast.makeText(getContext(), "Please record again!", Toast.LENGTH_SHORT).show();
        } catch (InterruptedException e) {
            Log.e(getActivity().toString(), e.getMessage());
        }
        voiceRecorder.release();
        voiceRecorder = null;
    }

    /**
     * This function sets the voice recorder with a new voicerecorder.
     *
     * @param fileName The file to output the voice recordings.
     */
    private void setVoiceRecorder(String fileName) {
        voiceRecorder = new MediaRecorder();

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(),
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

    /**
     * Stops recording and playback streams.
     */
    private void stopPlayBackAndRecording() {
        if (isRecording) {
            playRecordingButton.setVisibility(View.VISIBLE);
            stopAudioRecorder();
            stopRecordingAnimation();
            recordButton.setBackgroundResource(R.drawable.ic_mic_white);
        }
        if (draftPlayer != null && draftPlayer.isAudioPlaying()) {
            draftPlayer.stopAudio();
            draftPlayer.releaseAudio();
            playPauseDraftButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
        }
        if (dramatizationPlayer != null && dramatizationPlayer.isAudioPlaying()) {
            dramatizationPlayer.stopAudio();
            dramatizationPlayer.releaseAudio();
            playRecordingButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
        }
    }
}
