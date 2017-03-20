package org.sil.storyproducer.controller.draft;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.SlideText;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.AnimationToolbar;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.TextFiles;

import java.io.File;
import java.io.IOException;

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
public final class DraftFrag extends Fragment {
    private View rootView;
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    private int slideNumber;
    private SlideText slideText;
    private AudioPlayer narrationAudioPlayer;
    private AudioPlayer voiceAudioPlayer;
    private String narrationFilePath;
    private String recordFilePath;
    //private String tempRecordFilePath = null;
    private MediaRecorder voiceRecorder;
    private boolean isRecording = false;
    private AnimationToolbar myToolbar = null;
    private TransitionDrawable transitionDrawable;
    private Handler colorHandler;
    private Runnable colorHandlerRunnable;
    private boolean isRed = true;
    private final int RECORDING_ANIMATION_DURATION = 1500;
    private ImageButton narrationPlayButton;

    //Toolbar buttons
    private View toolbarMicButton;
    private View toolbarPlayButton;


    public DraftFrag() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle passedArgs = this.getArguments();

        slideNumber = passedArgs.getInt(SLIDE_NUM);
        slideText = TextFiles.getSlideText(StoryState.getStoryName(), slideNumber);
        recordFilePath = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).getPath();
        //tempRecordFilePath = AudioFiles.getDraftTemp(StoryState.getStoryName()).getPath();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(R.layout.fragment_draft, container, false);
        toolbarMicButton = rootView.findViewById(R.id.fragment_draft_mic_toolbar_button);
        toolbarPlayButton = rootView.findViewById(R.id.fragment_draft_play_toolbar_button);

        setUiColors();
        setPic(rootView.findViewById(R.id.fragment_draft_image_view), slideNumber);
        setScriptureText(rootView.findViewById(R.id.fragment_draft_scripture_text));
        setReferenceText(rootView.findViewById(R.id.fragment_draft_reference_text));
        setNarrationButton(rootView.findViewById(R.id.fragment_draft_narration_button));
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
                if (myToolbar != null) {
                    myToolbar.close();
                }
            }
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    @Override
    public void onPause() {
        super.onPause();
        stopPlayBackAndRecording();
        if (myToolbar != null) {
            myToolbar.close();
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on stop.
     */
    @Override
    public void onStop() {
        super.onStop();
        stopPlayBackAndRecording();
        if(myToolbar != null){
            myToolbar.close();
        }
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors() {
        if (slideNumber == 0) {
            RelativeLayout rl = (RelativeLayout) rootView.findViewById(R.id.fragment_draft_root_relayout_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            rl = (RelativeLayout) rootView.findViewById(R.id.fragment_draft_Relative_Layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));

            TextView tv = (TextView) rootView.findViewById(R.id.fragment_draft_scripture_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            tv = (TextView) rootView.findViewById(R.id.fragment_draft_reference_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param aView    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
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
     * Sets the main text of the layout.
     *
     * @param aView The text view that will be filled with the verse's text.
     */
    private void setScriptureText(View aView) {
        if (aView == null || !(aView instanceof TextView)) {
            return;
        }
        TextView textView = (TextView) aView;
        textView.setText(slideText.getContent());
    }

    /**
     * This function sets the reference text.
     *
     * @param aView The view that will be populated with the reference text.
     */
    private void setReferenceText(View aView) {
        if (aView == null || !(aView instanceof TextView)) {
            return;
        }
        TextView textView = (TextView) aView;

        String[] titleNamePriority = new String[]{slideText.getReference(),
                slideText.getSubtitle(), slideText.getTitle()};

        for (String title : titleNamePriority) {
            if (title != null && !title.equals("")) {
                textView.setText(title);
                return;
            }
        }

        //TODO Reference a string constant
        textView.setText("Bible Story!");
    }

    /**
     * This function sets the narration playback to the correct audio file. Also, the narration
     * button will have a listener added to it in order to detect playback when pressed.
     *
     * @param aView
     */
    private void setNarrationButton(View aView) {
        if (aView == null || !(aView instanceof ImageButton)) {
            return;
        }
        narrationFilePath = AudioFiles.getLWC(StoryState.getStoryName(), slideNumber).getPath();
        narrationPlayButton = (ImageButton)aView;
        narrationPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (narrationFilePath == null) {
                    Snackbar.make(rootView, "Could Not Find Narration Audio...", Snackbar.LENGTH_SHORT).show();
                } else {
                    if(narrationAudioPlayer != null && narrationAudioPlayer.isAudioPlaying()){
                        narrationAudioPlayer.stopAudio();
                        narrationAudioPlayer.releaseAudio();
                        narrationPlayButton.setBackgroundResource(R.drawable.ic_menu_play);
                    }else{
                        //stop other playback streams.
                        stopPlayBackAndRecording();
                        narrationAudioPlayer = new AudioPlayer();
                        narrationAudioPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                narrationAudioPlayer.releaseAudio();
                                narrationPlayButton.setBackgroundResource(R.drawable.ic_menu_play);
                            }
                        });
                        narrationAudioPlayer.playWithPath(narrationFilePath);
                        narrationPlayButton.setBackgroundResource(R.drawable.ic_stop_white_36dp);
                        Toast.makeText(getContext(), "Playing Narration Audio...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private void setToolbar(){
        setupToolbarAndRecordAnim(rootView.findViewById(R.id.fragment_draft_fab),
                rootView.findViewById(R.id.fragment_draft_animated_toolbar));
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
        LinearLayout dummyView = (LinearLayout) rootView.findViewById(R.id.fragment_draft_dummyView);
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
        setVoicePlayBackButton( new File(recordFilePath).exists());

        toolbarMicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    //stop all other playback streams.
                    stopPlayBackAndRecording();
                    startRecordingAnimation(false, 0);
                    startAudioRecorder();
                    toolbarPlayButton.setVisibility(View.INVISIBLE);
                    toolbarMicButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                }else{
                    stopRecordingAnimation();
                    stopAudioRecorder();
                    //set playback button visible
                    toolbarPlayButton.setVisibility(View.VISIBLE);
                    toolbarMicButton.setBackgroundResource(R.drawable.ic_mic_white);
                }
            }
        });
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
            Toast.makeText(getContext(), "Recording voice!", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException | IOException e) {
            Log.e(getActivity().toString(), e.getMessage());
        }
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
        //ConcatenateAudioFiles();
    }

    /**
     * This function sets the voice play back function. This function is called
     * in private void setRecordNPlayback(). This serves to set the visibility if the audio file
     * already exists.
     *
     * @param audioFileExists The boolean to check if the recording file exists.
     */
    private void setVoicePlayBackButton( boolean audioFileExists) {
        if (audioFileExists) {
            toolbarPlayButton.setVisibility(View.VISIBLE);
        }



        toolbarPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(voiceAudioPlayer != null && voiceAudioPlayer.isAudioPlaying()){
                    stopPlayBackAndRecording();
                    toolbarPlayButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                }else{
                    //Stops all other playback streams.
                    stopPlayBackAndRecording();
                    if (new File(recordFilePath).exists()) {
                        voiceAudioPlayer = new AudioPlayer();
                        voiceAudioPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                voiceAudioPlayer.releaseAudio();
                                toolbarPlayButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                            }
                        });
                        voiceAudioPlayer.playWithPath(recordFilePath);
                        Toast.makeText(getContext(), "Playing back recording!", Toast.LENGTH_SHORT).show();
                        toolbarPlayButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                    } else {
                        Toast.makeText(getContext(), "No translation recorded!", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
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

        RelativeLayout rel = (RelativeLayout) rootView.findViewById(R.id.fragment_draft_animated_toolbar);
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

//    /**
//     * This function adds two different audio files together to make one audio file into an
//     * .mp3 file. More comments will be added to this function later.
//     */
//    private void ConcatenateAudioFiles() {
//        Movie finalFile = new Movie();
//        String writtenToAudioFile = String.format(recordFilePath.substring(0, recordFilePath.indexOf(".m4a")) + "final.m4a");
//        Movie movieArray[];
//
//        try {
//            if (!new File(recordFilePath).exists()) {
//                movieArray = new Movie[]{MovieCreator.build(tempRecordFilePath)};
//            } else {
//                movieArray = new Movie[]{MovieCreator.build(recordFilePath),
//                        MovieCreator.build(tempRecordFilePath)};
//            }
//
//            List<Track> audioTrack = new ArrayList<>();
//
//            for (int i = 0; i < movieArray.length; i++)
//                for (Track t : movieArray[i].getTracks()) {
//                    if (t.getHandler().equals("soun")) {
//                        audioTrack.add(t);
//                    }
//                }
//
//            if (!audioTrack.isEmpty()) {
//                finalFile.addTrack(new AppendTrack(audioTrack.toArray(new Track[audioTrack.size()])));
//            }
//
//            Container out = new DefaultMp4Builder().build(finalFile);
//
//            FileChannel fc = new RandomAccessFile(writtenToAudioFile, "rwd").getChannel();
//            out.writeContainer(fc);
//            fc.close();
//
//            tryDeleteFile(recordFilePath);
//            boolean renamed = (new File(writtenToAudioFile).renameTo(tryCreateFile(recordFilePath)));
//            if (renamed) {
//                //delete old file
//                tryDeleteFile(writtenToAudioFile);
//            }
//
//        } catch (IOException e) {
//            Log.e(getActivity().toString(), e.getMessage());
//        }
//    }
//
//    /**
//     * Tries to create a new file.
//     *
//     * @param filePath The file path where a file should be created at.
//     * @return The file instantiation of the file that was created at the filePath.
//     */
//    private File tryCreateFile(String filePath) {
//        File toReturnFile = new File(filePath);
//        if (!toReturnFile.exists()) {
//            try {
//                toReturnFile.setExecutable(true);
//                toReturnFile.setReadable(true);
//                toReturnFile.setWritable(true);
//                toReturnFile.createNewFile();
//            } catch (IOException e) {
//                Log.w(getActivity().toString(), "Could not create file for recording!");
//            }
//        }
//
//        return toReturnFile;
//    }

    /**
     * Stops all playback streams and stops recording as well.
     */
    private void stopPlayBackAndRecording(){
        if (isRecording) {
            stopAudioRecorder();
            stopRecordingAnimation();
            //set playback button visible
            toolbarMicButton.setVisibility(View.VISIBLE);
            toolbarMicButton.setBackgroundResource(R.drawable.ic_mic_white);
            toolbarPlayButton.setVisibility(View.VISIBLE);
            toolbarPlayButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
        }
        if (narrationAudioPlayer != null && narrationAudioPlayer.isAudioPlaying()) {
            narrationPlayButton.setBackgroundResource(R.drawable.ic_menu_play);
            narrationAudioPlayer.stopAudio();
            narrationAudioPlayer.releaseAudio();
        }
        if (voiceAudioPlayer != null && voiceAudioPlayer.isAudioPlaying()) {
            toolbarPlayButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
            voiceAudioPlayer.stopAudio();
            voiceAudioPlayer.releaseAudio();
        }
    }
}
