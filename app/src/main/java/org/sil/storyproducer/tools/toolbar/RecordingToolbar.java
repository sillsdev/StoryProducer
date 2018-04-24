package org.sil.storyproducer.tools.toolbar;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.Space;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.apache.commons.io.IOUtils;
import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.Modal;
import org.sil.storyproducer.controller.remote.BackTranslationFrag;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.model.logging.ComChkEntry;
import org.sil.storyproducer.model.logging.DraftEntry;
import org.sil.storyproducer.model.logging.LearnEntry;
import org.sil.storyproducer.model.logging.LogEntry;
import org.sil.storyproducer.tools.Network.VolleySingleton;
import org.sil.storyproducer.tools.Network.paramStringRequest;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.LogFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.media.AudioRecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The purpose of this class is to extend the animationToolbar while adding the recording animation
 * to the toolbar. <br/><br/>
 * This class utilizes an empty layout for the toolbar and floating action button found in this layout:
 * (toolbar_for_recording.xml). <br/>
 * The toolbar is where buttons are added to.<br/> The toolbar is then placed at the
 * bottom of the rootViewLayout that is passed in to the this class' constructor. So, the rootViewLayout
 * must be of type RelativeLayout because the code related to placing the toolbar in the
 * rootViewLayout requires the rootViewLayout to be of type RelativeLayout. See: {@link #setupToolbar()}<br/><br/>
 * This class also saves the recording and allows playback <br/> from the toolbar. see: {@link #createToolbar()}
 * <br/><br/>
 */
public class RecordingToolbar extends AnimationToolbar {
    private final int RECORDING_ANIMATION_DURATION = 1500;
    private final int STOP_RECORDING_DELAY = 0;
    private final String TAG = "AnimationToolbar";
    public static final String R_CONSULTANT_PREFS = "Consultant_Checks";
    private static final String TRANSCRIPTION_TEXT = "TranscriptionText";

    //private FloatingActionButton fabPlus;
    protected LinearLayout toolbar;
    private Modal multiRecordModal;

    protected LinearLayout rootViewToolbarLayout;
    private View rootViewToEmbedToolbarIn;
    protected String recordFilePath;
    protected String playbackRecordFilePath;
    protected Context appContext;

    protected ImageButton micButton;
    protected ImageButton playButton;
    protected ImageButton deleteButton;
    protected ImageButton multiRecordButton;
    protected ImageButton sendAudioButton;
    private List<AuxiliaryMedia> auxiliaryMediaList;


    protected boolean isRecording;
    protected boolean enablePlaybackButton;
    protected boolean enableDeleteButton;
    protected boolean enableMultiRecordButton;
    protected boolean enableSendAudioButton;

    private TransitionDrawable transitionDrawable;
    private Handler colorHandler;
    private Runnable colorHandlerRunnable;
    private boolean isToolbarRed = false;
    private MediaRecorder voiceRecorder;
    protected AudioPlayer audioPlayer;

    protected RecordingListener recordingListener;
    private boolean canOverwrite = false;

    private Map<String,String> js;
    private String resp;
    private String testErr;

    /**
     * The ctor.
     *
     * @param activity              The activity from the calling class.
     * @param rootViewToolbarLayout The rootViewToEmbedToolbarIn of the Toolbar layout called toolbar_for_recording.
     *                              must be of type LinearLayout so that buttons can be
     *                              evenly spaced.
     * @param rootViewLayout        The rootViewToEmbedToolbarIn of the layout that you want to embed the toolbar in.
     * @param enablePlaybackButton  Enable playback of recording.
     * @param enableDeleteButton    Enable the delete button, does not work as of now.
     * @param enableSendAudioButton Enable the sending of audio to the server
     * @param recordFilePath        The filepath that the recording will be saved under.
     */
    public RecordingToolbar(Activity activity, View rootViewToolbarLayout, RelativeLayout rootViewLayout,
                            boolean enablePlaybackButton, boolean enableDeleteButton, boolean enableMultiRecordButton, boolean enableSendAudioButton, String playbackRecordFilePath, String recordFilePath, Modal multiRecordModal, RecordingListener recordingListener) throws ClassCastException {
        super(activity);
        super.initializeToolbar(rootViewToolbarLayout.findViewById(R.id.toolbar_for_recording_fab), rootViewToolbarLayout.findViewById(R.id.toolbar_for_recording_toolbar));

        this.activity = activity;
        this.appContext = activity.getApplicationContext(); //This is calling getApplicationContext because activity.getContext() cannot be accessed publicly.
        this.rootViewToolbarLayout = (LinearLayout) rootViewToolbarLayout;
        this.rootViewToEmbedToolbarIn = rootViewLayout;
        //fabPlus = (FloatingActionButton) this.rootViewToolbarLayout.findViewById(R.id.toolbar_for_recording_fab);
        this.toolbar = (LinearLayout) this.rootViewToolbarLayout.findViewById(R.id.toolbar_for_recording_toolbar);
        this.enablePlaybackButton = enablePlaybackButton;
        this.enableDeleteButton = enableDeleteButton;
        this.enableSendAudioButton = enableSendAudioButton;
        this.enableMultiRecordButton = enableMultiRecordButton;
        this.playbackRecordFilePath = playbackRecordFilePath;
        this.recordFilePath = recordFilePath;
        this.recordingListener = recordingListener;
        this.multiRecordModal = multiRecordModal;
        auxiliaryMediaList = new ArrayList<>();
        createToolbar();
        setupRecordingAnimationHandler();
        audioPlayer = new AudioPlayer();
        audioPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
            }
        });
    }

    public interface RecordingListener {
        void onStoppedRecording();
        void onStartedRecordingOrPlayback(boolean isRecording);
    }

    /**
     * This function is used to show the floating button
     */
    /*public void showFloatingActionButton() {
        fabPlus.show();
    }*/

    /**
     * This function is used to hide the floating button
     */
   /* public void hideFloatingActionButton() {
        fabPlus.hide();
    }*/

    /**
     * This function can be called so that the toolbar is automatically opened, without animation,
     * when the fragment is drawn.
     */
    public void keepToolbarVisible() {
        //hideFloatingActionButton();
        toolbar.setVisibility(View.VISIBLE);
    }

    /**
     * This function is used to stop all the media sources on the toolbar from playing or recording.
     * The auxiliary medias are not stopped because the calling class should be responsible for
     * those.
     */
    public void stopToolbarMedia() {
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
            if(enableMultiRecordButton){
                multiRecordButton.setVisibility(View.VISIBLE);
            }
            if(enableSendAudioButton){
                sendAudioButton.setVisibility(View.VISIBLE);
            }
        }
        if (audioPlayer.isAudioPlaying()) {
            playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
            audioPlayer.stopAudio();
        }
    }

    public void releaseToolbarAudio() {
        audioPlayer.release();
    }

    /**
     * This function is used so that other potential media sources outside the toolbar can be
     * stopped if the toolbar is pressed.
     *
     * @param viewThatIsPlayingButton The button that is pressed to activate the media playback.
     * @param setButtonToDrawable     The drawable to set the trigger button to a different drawable
     *                                when the toolbar is touched and the media is stopped.
     *                                (The reset drawable for the button) Like pause to play button.
     * @param playingAudio            The audio source of playback.
     */
    public void onToolbarTouchStopAudio(View viewThatIsPlayingButton, int setButtonToDrawable, AudioPlayer playingAudio) {
        AuxiliaryMedia auxiliaryMedia = new AuxiliaryMedia();
        auxiliaryMedia.playingAudio = playingAudio;
        auxiliaryMedia.setButtonToDrawableOnStop = setButtonToDrawable;
        auxiliaryMedia.viewThatIsPlayingButton = viewThatIsPlayingButton;
        auxiliaryMediaList.add(auxiliaryMedia);
    }

    public boolean isRecording() {
        return isRecording;
    }
    /**
     * set the recording file path
     *
     * @param path to set the recording path to
     */
    public void setRecordFilePath(String path) {
        recordFilePath = path;
    }

    /**
     * set the recording file path for playing back audio
     *
     * @param path to set the recording path to
     */
    public void setPlaybackRecordFilePath(String path) {
        playbackRecordFilePath = path;
    }

    /**
     * Calling class should be responsible for all other media
     * so {@link #stopPlayBackAndRecording()} is not being used here.
     */
    public void onClose() {
        stopToolbarMedia();
    }


    public void closeToolbar(){
        if(toolbar != null){
            super.close();
        }
    }

    public void hideButtons(){
        if(enablePlaybackButton){
            playButton.setVisibility(View.INVISIBLE);
        }
        if(enableMultiRecordButton){
            multiRecordButton.setVisibility(View.INVISIBLE);
        }
        if(enableDeleteButton){
            deleteButton.setVisibility(View.INVISIBLE);
        }
        if(enableSendAudioButton){
            sendAudioButton.setVisibility(View.INVISIBLE);
        }
    }

    protected void startRecording() {
        //TODO: make this logging more robust and encapsulated
        if(StoryState.getCurrentPhase().getType() == Phase.Type.DRAFT){
            LogFiles.saveLogEntry(DraftEntry.Type.DRAFT_RECORDING.makeEntry());
        }
            startAudioRecorder();
            startRecordingAnimation(false, 0);
            recordingListener.onStartedRecordingOrPlayback(true);

    }

    protected void stopRecording() {
        stopAudioRecorder();
        stopRecordingAnimation();
        recordingListener.onStoppedRecording();
    }

    //TODO finish adding deletion functionality.
    protected boolean deleteRecording() {
        if (enableDeleteButton) {
            return false;
        } else {
            return false;
        }
    }

    private void createToolbar() {
        setupToolbar();
        setupToolbarButtons();
    }

    /**
     * This function formats and aligns the buttons to the toolbar.
     */
    protected void setupToolbarButtons() {
        rootViewToolbarLayout.removeAllViews();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams spaceLayoutParams = new LinearLayout.LayoutParams(0, 0, 1f);
        spaceLayoutParams.width = 0;
        int[] drawables = new int[]{R.drawable.ic_mic_white, R.drawable.ic_play_arrow_white_48dp, R.drawable.ic_delete_forever_white_48dp, R.drawable.ic_playlist_play_white_48dp, R.drawable.ic_send_audio_48dp};
        ImageButton[] imageButtons = new ImageButton[]{new ImageButton(appContext), new ImageButton(appContext), new ImageButton(appContext), new ImageButton(appContext), new ImageButton(appContext)};
        boolean[] buttonToDisplay = new boolean[]{true/*enable mic*/, enablePlaybackButton, enableDeleteButton, enableMultiRecordButton, enableSendAudioButton};

        Space buttonSpacing = new Space(appContext);
        buttonSpacing.setLayoutParams(spaceLayoutParams);
        toolbar.addView(buttonSpacing); //Add a space to the left of the first button.
        for (int i = 0; i < drawables.length; i++) {
            if (buttonToDisplay[i]) {
                imageButtons[i].setBackgroundResource(drawables[i]);
                imageButtons[i].setVisibility(View.VISIBLE);
                imageButtons[i].setLayoutParams(layoutParams);
                toolbar.addView(imageButtons[i]);

                buttonSpacing = new Space(appContext);
                buttonSpacing.setLayoutParams(spaceLayoutParams);
                toolbar.addView(buttonSpacing);
                if (i == 0) {
                    micButton = imageButtons[i];
                } else if (i == 1) {
                    playButton = imageButtons[i];
                } else if (i == 2) {
                    deleteButton = imageButtons[i];
                } else if (i == 3) {
                    multiRecordButton = imageButtons[i];
                } else if (i == 4) {
                    sendAudioButton = imageButtons[i];
                }
            }
        }

        boolean playBackFileExist = new File(playbackRecordFilePath).exists();
        if (enablePlaybackButton) {
            playButton.setVisibility((playBackFileExist) ? View.VISIBLE : View.INVISIBLE);
        }
        if(enableMultiRecordButton){
            multiRecordButton.setVisibility((playBackFileExist) ? View.VISIBLE : View.INVISIBLE);
        }
        if (enableDeleteButton) {
            deleteButton.setVisibility((playBackFileExist) ? View.VISIBLE : View.INVISIBLE);
        }
        if(enableSendAudioButton){
            sendAudioButton.setVisibility((playBackFileExist) ? View.VISIBLE : View.INVISIBLE);
        }
        setOnClickListeners();
    }

    /**
     * This function formats and aligns the toolbar and floating action button to the bottom of the relative layout of the
     * calling class.
     */
    protected void setupToolbar() {
        RelativeLayout.LayoutParams[] myParams =
                new RelativeLayout.LayoutParams[]{/*new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT),*/
                        new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)};
        int[] myRules = new int[]{RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_END, RelativeLayout.ALIGN_PARENT_RIGHT};
        View[] myView = new View[]{ /*fabPlus,*/toolbar};

        //Must remove all children of the layout, before appending them to the new rootView
        rootViewToolbarLayout.removeAllViews();
        for (int i = 0; i < myParams.length; i++) {
            for (int j = 0; j < myRules.length; j++) {
                myParams[i].addRule(myRules[j], rootViewToEmbedToolbarIn.getId());
            }
            myView[i].setLayoutParams(myParams[i]);
            ((RelativeLayout) rootViewToEmbedToolbarIn).addView(myView[i]);
        }
        //Index corresponds to the myView array
        //fabPlus = (FloatingActionButton) myView[0];
        toolbar = (LinearLayout) myView[0];
    }

    /**
     * Enables the buttons to have the appropriate onClick listeners.
     */
    protected void setOnClickListeners() {
        View.OnClickListener micListener = new View.OnClickListener() {
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
                    if (enableMultiRecordButton) {
                        multiRecordButton.setVisibility(View.VISIBLE);
                    }
                    if(enableSendAudioButton){
                        sendAudioButton.setVisibility(View.VISIBLE);

                    }
                }
                else {
                    //learn phase overwrite dialog
                    if(StoryState.getCurrentPhase().getType() == Phase.Type.LEARN
                            || StoryState.getCurrentPhase().getType() == Phase.Type.WHOLE_STORY){
                        boolean recordingExists = new File(recordFilePath).exists();
                        if(recordingExists) {
                            AlertDialog dialog = new AlertDialog.Builder(activity)
                                    .setTitle(activity.getString(R.string.overwrite))
                                    .setMessage(activity.getString(R.string.learn_phase_overwrite))
                                    .setNegativeButton(activity.getString(R.string.no), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //do nothing
                                        }
                                    })
                                    .setPositiveButton(activity.getString(R.string.yes), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //overwrite audio
                                           recordAudio();
                                        }
                                    }).create();

                            dialog.show();

                        }
                        else{
                            recordAudio();
                        }
                    }
                    else{
                      recordAudio();
                    }
                }
            }
        };
        micButton.setOnClickListener(micListener);

        if (enablePlaybackButton) {
            View.OnClickListener playListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (audioPlayer.isAudioPlaying()) {
                        audioPlayer.stopAudio();
                        playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                    } else {
                        stopPlayBackAndRecording();
                        if (new File(playbackRecordFilePath).exists()) {
                            audioPlayer.setPath(playbackRecordFilePath);
                            audioPlayer.playAudio();
                            Toast.makeText(appContext, R.string.recording_toolbar_play_back_recording, Toast.LENGTH_SHORT).show();
                            playButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                            recordingListener.onStartedRecordingOrPlayback(false);
                            //TODO: make this logging more robust and encapsulated
                            if(StoryState.getCurrentPhase().getType() == Phase.Type.DRAFT) {
                                LogFiles.saveLogEntry(DraftEntry.Type.DRAFT_PLAYBACK.makeEntry());
                            }
                        } else {
                            Toast.makeText(appContext, R.string.recording_toolbar_no_recording, Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            };

            playButton.setOnClickListener(playListener);
        }
        if (enableDeleteButton) {
            View.OnClickListener deleteListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopPlayBackAndRecording();
                }
            };
            deleteButton.setOnClickListener(deleteListener);
        }
        if (enableMultiRecordButton) {
            if(multiRecordModal != null){
                    View.OnClickListener multiRecordModalButtonListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            stopPlayBackAndRecording();
                            multiRecordModal.show();
                        }
                    };

                    multiRecordButton.setOnClickListener(multiRecordModalButtonListener);
            }

        }
        if(enableSendAudioButton){
            View.OnClickListener sendAudioListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopPlayBackAndRecording();
                    sendAudio();
                }
            };
                sendAudioButton.setOnClickListener(sendAudioListener);
            }
        }

        /*
        *Send single audio file to remote consultant
         */
        private void sendAudio(){
            Toast.makeText(appContext, R.string.audio_pre_send, Toast.LENGTH_SHORT).show();
            Phase phase = StoryState.getCurrentPhase();
            int slideNum;
            File slide;
            int totalSlides = FileSystem.getContentSlideAmount(StoryState.getStoryName());
            if(phase.getType() == Phase.Type.BACKT) {
                slideNum = StoryState.getCurrentStorySlide();
                slide = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNum);
            }
            //Whole story bt audio will be uploaded as one slide past the final slide for the story
            else{
                slideNum = FileSystem.getContentSlideAmount(StoryState.getStoryName());
                slide = AudioFiles.getWholeStory(StoryState.getStoryName());
            }

            requestRemoteReview(appContext, totalSlides);
            postABackTranslation(slideNum, slide);
        }

        //Posts a single BT or WSBT
        public void postABackTranslation(int slideNum, File slide){
            try {
                Upload(slide, appContext, slideNum);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }

    //First time request for review
    public void requestRemoteReview(Context con, int numSlides){

        Context myContext = con;
        final String phone_id = Settings.Secure.getString(myContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        js = new HashMap<String,String>();
        js.put("Key", myContext.getResources().getString(R.string.api_token));
        js.put("PhoneId", phone_id);
        js.put("TemplateTitle", StoryState.getStoryName());
        js.put("NumberOfSlides", Integer.toString(numSlides));

        StringRequest req = new StringRequest(Request.Method.POST, myContext.getString(R.string.url_request_review), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("LOG_VOLLEY_RESP_RR", response.toString());
                resp  = response;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG_VOLLEY_ERR_RR", error.toString());
                Log.e("LOG_VOLLEY", "HIT ERROR");
                testErr = error.toString();

            }

        }) {
            @Override
            protected Map<String, String> getParams()
            {
                return js;
            }
        };


        VolleySingleton.getInstance(activity.getApplicationContext()).addToRequestQueue(req);

    }

    //Subroutine to upload a single audio file
    public void Upload ( final File fileName, Context con, int slide) throws IOException {

        Context myContext = con;
        String phone_id = Settings.Secure.getString(myContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String templateTitle = StoryState.getStoryName();

        String currentSlide = Integer.toString(slide);
        InputStream input = new FileInputStream(fileName);
        byte[] audioBytes = IOUtils.toByteArray(input);

        //get transcription text if it's there
        SharedPreferences prefs = con.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        String transcription = prefs.getString(templateTitle + slide + TRANSCRIPTION_TEXT,"");
        String byteString = Base64.encodeToString( audioBytes ,Base64.DEFAULT);

        js = new HashMap<String,String>();


        org.sil.storyproducer.model.logging.Log log = LogFiles.getLog(FileSystem.getLanguage(), StoryState.getStoryName());
        String logString = "";

        Object[] logs = log.toArray();

            //Grabs all applicable logs for this slide num
            String[] slideLogs = new String[logs.length];

            for (int i = 0; i < logs.length; i++) {
                if (logs[i] instanceof ComChkEntry) {
                    ComChkEntry tempLog = (ComChkEntry) logs[i];
                    if (tempLog.appliesToSlideNum(slide)) {
                        logString += tempLog.getPhase() + " " + tempLog.getDescription()
                                + " " + tempLog.getDateTime() + "\n";
                    }

                } else if (logs[i] instanceof LearnEntry) {
                    LearnEntry tempLog = (LearnEntry) logs[i];
                    if (tempLog.appliesToSlideNum(slide)) {
                        logString += tempLog.getPhase() + " " + tempLog.getDescription()
                                + " " + tempLog.getDateTime() + "\n";
                    }
                } else if (logs[i] instanceof DraftEntry) {
                    DraftEntry tempLog = (DraftEntry) logs[i];
                    if (tempLog.appliesToSlideNum(slide)) {
                        logString += tempLog.getPhase() + " " + tempLog.getDescription()
                                + " " + tempLog.getDateTime() + "\n";
                    }
                } else {
                    LogEntry tempLog = (LogEntry) logs[i];
                    if (tempLog.appliesToSlideNum(slide)) {
                        logString += tempLog.getPhase() + " " + tempLog.getDescription()
                                + " " + tempLog.getDateTime() + "\n";
                    }

                }

            }
            js.put("Log", logString);
            js.put("Key", myContext.getResources().getString(R.string.api_token));
            js.put("PhoneId", phone_id);
            js.put("TemplateTitle", templateTitle);
            js.put("SlideNumber", currentSlide);
            js.put("Data", byteString);
            js.put("BacktranslationText", transcription);


        paramStringRequest req = new paramStringRequest(Request.Method.POST, myContext.getResources().getString(R.string.url_upload_audio), js, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("LOG_VOLLEY_RESP_UPL", response.toString());
                resp  = response;
                Toast.makeText(appContext, R.string.audio_Sent, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG_VOLLEY_ERR_UPL", error.toString());
                Log.e("LOG_VOLLEY", "HIT ERROR");
                testErr = error.toString();
                Toast.makeText(appContext, R.string.audio_Send_Failed, Toast.LENGTH_SHORT).show();

            }

        }) {
            @Override
            protected Map<String, String> getParams()
            {
                return this.mParams;
            }
        };


        VolleySingleton.getInstance(activity.getApplicationContext()).addToRequestQueue(req);

    }

    /*
    * Start recording audio and hide buttons
     */
    private void recordAudio(){
        stopPlayBackAndRecording();
        startRecording();
        micButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
        if (enableDeleteButton) {
            deleteButton.setVisibility(View.INVISIBLE);
        }
        if (enablePlaybackButton) {
            playButton.setVisibility(View.INVISIBLE);
        }
        if (enableMultiRecordButton) {
            multiRecordButton.setVisibility(View.INVISIBLE);
        }
        if(enableSendAudioButton){
            sendAudioButton.setVisibility(View.INVISIBLE);
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
     * Call this function prior to calling the function to start the animation.  E.g.: <br/>
     * {@link #setupRecordingAnimationHandler()}, should be called once<br/>
     * {@link #startRecordingAnimation(boolean, int)}{}
     * <br/><br/>
     * Essentially the function utilizes a Transition Drawable to interpolate between the red and
     * the toolbar color. (The colors are defined in an array and used in the transition drawable)
     * To schedule the running of the transition drawable a handler and runnable are used.<br/><br/>
     * The handler takes a runnable which schedules the transitionDrawable. The handler function
     * called postDelayed will delay the running of the next Runnable by the passed in value e.g.:
     * colorHandler.postDelayed(runnable goes here, time delay in MS). Make sure that isToolbarRed is set
     * to false initially.
     * <br/><br/>
     * Still confused about handlers, runnables, and the MessageQueue?
     * <br/>
     * <a href="http://stackoverflow.com/questions/12877944/what-is-the-relationship-between-looper-handler-and-messagequeue-in-android">See this excellent SO post for more info.</a>
     */
    private void setupRecordingAnimationHandler() {
        int red = Color.rgb(255, 0, 0);
        int colorOfToolbar = Color.rgb(0, 0, 255); /*Arbitrary color value of blue used initially*/

        Drawable relBackgroundColor = toolbar.getBackground();
        if (relBackgroundColor instanceof ColorDrawable) {
            colorOfToolbar = ((ColorDrawable) relBackgroundColor).getColor();
        }
        transitionDrawable = new TransitionDrawable(new ColorDrawable[]{
                new ColorDrawable(colorOfToolbar),
                new ColorDrawable(red)
        });
        toolbar.setBackground(transitionDrawable);

        colorHandler = new Handler();
        colorHandlerRunnable = new Runnable() {
            @Override
            public void run() {
                //Animation to change the toolbar's color while recording
                if (isToolbarRed) {
                    transitionDrawable.reverseTransition(RECORDING_ANIMATION_DURATION);
                    isToolbarRed = false;
                } else {
                    transitionDrawable.startTransition(RECORDING_ANIMATION_DURATION);
                    isToolbarRed = true;
                }
                startRecordingAnimation(true, RECORDING_ANIMATION_DURATION);
            }
        };
    }

    /**
     * This function is used to start the handler to run the runnable. <br/>
     * {@link #setupRecordingAnimationHandler()} should be called first before calling this function
     * to initialize the colorHandler and colorHandlerRunnable().
     *
     * @param isDelayed Used to signify that the runnable will be delayed in running.
     * @param delay     The time that will be delayed in ms if isDelayed is true.
     */
    protected void startRecordingAnimation(boolean isDelayed, int delay) {
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
    protected void stopRecordingAnimation() {
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
    protected void startAudioRecorder() {
        setVoiceRecorder(recordFilePath);
        try {
            isRecording = true;
            voiceRecorder.prepare();
            voiceRecorder.start();
            Toast.makeText(appContext, R.string.recording_toolbar_recording_voice, Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException | IOException e) {
            Log.e(TAG, "Could not record voice.", e);
        }
    }

    /**
     * The function that aids in stopping an audio recorder.
     */
    protected void stopAudioRecorder() {
        try {
            isRecording = false;
            //Delay stopping of voiceRecorder to capture all of the voice recorded.
            Thread.sleep(STOP_RECORDING_DELAY);
            voiceRecorder.stop();
            Toast.makeText(appContext, R.string.recording_toolbar_stop_recording_voice, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException stopException) {
            Toast.makeText(appContext, R.string.recording_toolbar_error_recording, Toast.LENGTH_SHORT).show();
        } catch (InterruptedException e) {
            Log.e(TAG, "Voice recorder interrupted!", e);
        }
        voiceRecorder.release();
        voiceRecorder = null;
    }

    /**
     * This function sets the voice recorder with a new voiceRecorder.
     *
     * @param fileName The file to output the voice recordings.
     */
    protected void setVoiceRecorder(String fileName) {
        voiceRecorder = new AudioRecorder(fileName, activity);
    }

    //TODO The arraylist is being populated by null objects. This is because the other classes are releasing too much. Will be taken care of once lockeridge's branch Audio Player fix is merged into dev
    /**
     * This function stops all playback and all auxiliary media.
     */
    protected void stopPlayBackAndRecording() {
        stopToolbarMedia();
        if (auxiliaryMediaList != null) {
            for (AuxiliaryMedia am : auxiliaryMediaList) {
                am.stopPlaying();
            }
        }
    }

    /**
     * Use this class to hold media that should be stopped when a toolbar button is pressed.
     * <br/>Refer to function {@link #onToolbarTouchStopAudio(View, int, AudioPlayer)} for more information.
     */
    protected class AuxiliaryMedia {
        View viewThatIsPlayingButton;
        int setButtonToDrawableOnStop;
        AudioPlayer playingAudio;

        void stopPlaying() {
            if (playingAudio != null && playingAudio.isAudioPlaying()) {
                playingAudio.stopAudio();
                //playingAudio.releaseAudio();
                if (viewThatIsPlayingButton != null) {
                    viewThatIsPlayingButton.setBackgroundResource(setButtonToDrawableOnStop);
                }
            }
        }
    }
}
