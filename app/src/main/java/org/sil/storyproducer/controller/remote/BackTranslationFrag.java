package org.sil.storyproducer.controller.remote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sil.storyproducer.R;
import org.sil.storyproducer.tools.Network.VolleySingleton;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.toolbar.PausingRecordingToolbar;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by alexamhepner on 10/23/17.
 */

public class BackTranslationFrag extends Fragment {

//    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
//    public static final String R_CONSULTANT_PREFS = "Consultant_Checks";
//    public static final String IS_R_CONSULTANT_APPROVED = "isApproved";
//    private static final String IS_CHECKED = "isCheckedi";
//    private static final String TRANSCRIPTION_TEXT = "TranscriptionText";
//
//    private View rootView;
//    private View rootViewToolbar;
//    private int slideNumber;
//    private EditText slideText;
//    private String storyName;
//    private boolean phaseUnlocked = false;
//    private AudioPlayer draftPlayer;
//    private boolean draftAudioExists;
//    private File backTranslationRecordingFile = null;
//    private ImageButton draftPlayButton;
//    private boolean isSlidesChecked = false;
//
//    private EditText transcriptionText;
//
//    private JSONObject obj;
//    private String resp;
//    private Map<String, String> js;
//
//    private PausingRecordingToolbar recordingToolbar;
//
//    @Override
//    public void onCreate(Bundle savedState) {
//        super.onCreate(savedState);
//        /* FIXME
//        Bundle passedArgs = this.getArguments();
//        slideNumber = passedArgs.getInt(SLIDE_NUM);
//        storyName = StoryState.getStoryName();
//        setRecordFilePath();
//        setHasOptionsMenu(true);
//        */
//
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        rootView = inflater.inflate(R.layout.fragment_backtranslation, container, false);
//
//        draftPlayButton = rootView.findViewById(R.id.fragment_backtranslation_play_draft_button);
//        setUiColors();
//        setPic((ImageView)rootView.findViewById(R.id.fragment_backtranslation_image_view), slideNumber);
//        setCheckmarkButton((ImageButton)rootView.findViewById(R.id.fragment_backtranslation_r_concheck_checkmark_button));
//        TextView slideNumberText = rootView.findViewById(R.id.slide_number_text);
//        slideNumberText.setText(slideNumber + "");
//
//        rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false);
//        closeKeyboardOnTouch(rootView);
//        transcriptionText = rootView.findViewById(R.id.transcription);
//        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
//        final String prefsKeyString = storyName + slideNumber + TRANSCRIPTION_TEXT;
//        String savedTranscriptionText = prefs.getString(prefsKeyString, "");
//        transcriptionText.setText(savedTranscriptionText);
//        transcriptionText.addTextChangedListener(new TextWatcher()
//        {
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count){
//
//            }
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int aft )
//            {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable s)
//            {
//                addTranscription();
//            }
//        });
//        /*transcriptionText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//
//            public void onFocusChange(View view, boolean hasFocus) {
//                if (!hasFocus) {
//                    addTranscription();
//                }
//
//            }
//        });*/
//        return rootView;
//    }
//
//    //TODO: new icon for sbs backtranslation
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        MenuItem item =  menu.getItem(0);
//        super.onCreateOptionsMenu(menu, inflater);
//        item.setIcon(R.drawable.ic_headset_mic_white_48dp);
//    }
//
//    public void onStart() {
//        super.onStart();
//        /*FIXME
//        setToolbar(rootViewToolbar);
//
//        draftPlayer = new AudioPlayer();
//        File draftAudioFile = AudioFiles.INSTANCE.getDraft(storyName, slideNumber);
//        if (draftAudioFile.exists()) {
//            draftAudioExists = true;
//            //FIXME
//            //draftPlayer.setSource(draftAudioFile.getPath());
//        } else {
//            draftAudioExists = false;
//        }
//        draftPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                draftPlayButton.setBackgroundResource(R.drawable.ic_play_gray);
//            }
//        });
//
//        setPlayStopDraftButton((ImageButton)rootView.findViewById(R.id.fragment_backtranslation_play_draft_button));
//
//        //dramatize phase not unlocked yet
//        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
//        final String prefsKeyString = storyName + IS_R_CONSULTANT_APPROVED;
//        if(!prefs.getBoolean(prefsKeyString, false)) {
//            //TODO: remove call to create story here and make global var that stores whether story has been created in DB
//            //TODO: put all the volley functions into a separate class? (have redundancy between some classes)
//            requestRemoteReview(getContext(),FileSystem.getContentSlideAmount(storyName));
//            getSlidesStatus();
//            setCheckmarkButton((ImageButton) rootView.findViewById(R.id.fragment_backtranslation_r_concheck_checkmark_button));
//            phaseUnlocked = checkAllMarked();
//            if (phaseUnlocked) {
//                unlockDramatizationPhase();
//            }
//        }
//        */
//    }
//
//    /**
//     * This function serves to stop the audio streams from continuing after dramatization has been
//     * put on pause.
//     */
//    @Override
//    public void onPause() {
//        super.onPause();
//        if (recordingToolbar != null) {
//            recordingToolbar.onPause();
//        }
//        closeKeyboard(rootView);
//    }
//
//    /**
//     * This function serves to stop the audio streams from continuing after dramatization has been
//     * put on stop.
//     */
//    @Override
//    public void onStop()  {
//        super.onStop();
//        draftPlayer.release();
//        if(recordingToolbar != null){
//            recordingToolbar.onPause();
//        }
//
//        closeKeyboard(rootView);
//    }
//
//    /**
//     * This function serves to handle draft page changes and stops the audio streams from
//     * continuing.
//     *
//     * @param isVisibleToUser whether fragment is visible to user
//     */
//    @Override
//    public void setUserVisibleHint(boolean isVisibleToUser) {
//        super.setUserVisibleHint(isVisibleToUser);
//
//        // Make sure that we are currently visible
//        if (this.isVisible()) {
//            // If we are becoming invisible, then...
//            if (!isVisibleToUser) {
//                if (recordingToolbar != null) {
//                    recordingToolbar.onPause();
//                }
//                closeKeyboard(rootView);
//            }
//        }
//    }
//
//    /**
//     * Used to stop playing and recording any media. The calling class should be responsible for
//     */
//    public void stopPlayBackAndRecording() {
//        recordingToolbar.stopToolbarMedia();
//    }
//
//    /**
//     * Used to hide the play and multiple recordings button.
//     */
//    public void hideButtonsToolbar(){
//        recordingToolbar.hideButtons();
//    }
//
//    /**
//     * sets the playback path
//     */
//    public void updatePlayBackPath() {
//        //FIXME
//        //String playBackFilePath = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber).getPath();
//        //recordingToolbar.setPlaybackRecordFilePath(playBackFilePath);
//    }
//
//    /*
//      Stop the toolbar from continuing the appending session.
//     */
//  //  public void stopAppendingRecordingFile(){
//   //     recordingToolbar.stopAppendingSession();
//   // }
//
//    /**
//     * This function sets the first slide of each story to the blue color in order to prevent
//     * clashing of the grey starting picture.
//     */
//    private void setUiColors() {
//        if (slideNumber == 0) {
//            RelativeLayout rl = rootView.findViewById(R.id.fragment_backtranslation_root_layout);
//            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
//        }
//    }
//
//    /**
//     * This function is used to the set the picture per slide.
//     *
//     * @param slideImage    The view that will have the picture rendered on it.
//     * @param slideNum The respective slide number for the dramatization slide.
//     */
//    private void setPic(final ImageView slideImage, int slideNum) {
//
//        /*FIXME
//
//        Bitmap slidePicture = ImageFiles.getBitmap(storyName, slideNum);
//
//        if (slidePicture == null) {
//            Snackbar.make(rootView, R.string.backTranslation_draft_no_picture, Snackbar.LENGTH_SHORT).show();
//        }
//
//        //Get the height of the phone.
//        DisplayMetrics phoneProperties = getContext().getResources().getDisplayMetrics();
//        int height = phoneProperties.heightPixels;
//        double scalingFactor = 0.4;
//        height = (int) (height * scalingFactor);
//
//        //scale bitmap
//        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture, height);
//
//        //Set the height of the image view
//        slideImage.getLayoutParams().height = height;
//        slideImage.requestLayout();
//
//        slideImage.setImageBitmap(slidePicture);
//        */
//    }
//
//    /**
//     * sets the playback path
//     */
//    public void setPlayBackPath() {
//        //FIXME
//        //String playBackFilePath = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber).getPath();
//        //recordingToolbar.setPlaybackRecordFilePath(playBackFilePath);
//    }
//
//    /**
//     * This function serves to set the play and stop button for the draft playback button.
//     */
//
//    private void setPlayStopDraftButton(final ImageButton playPauseDraftButton) {
//
//        if (!draftAudioExists) {
//            //draft recording does not exist
//            playPauseDraftButton.setAlpha(0.8f);
//            playPauseDraftButton.setColorFilter(Color.argb(200, 200, 200, 200));
//        } else {
//            //remove x mark from ImageButton play
//            playPauseDraftButton.setImageResource(0);
//        }
//        playPauseDraftButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(!draftAudioExists){
//                    Toast.makeText(getContext(), R.string.backTranslation_no_draft_recording_available, Toast.LENGTH_SHORT).show();
//                }
//                else if (draftPlayer.isAudioPlaying()) {
//                    draftPlayer.pauseAudio();
//                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_play_circle_outline_white_36dp);
//                } else {
//                    recordingToolbar.stopToolbarMedia();
//                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_pause_circle_outline_white_36dp);
//                    draftPlayer.playAudio();
//
//                    if(draftPlayer != null){ //if there is a draft available to play
//                        recordingToolbar.onToolbarTouchStopAudio(playPauseDraftButton, R.drawable.ic_play_circle_outline_white_36dp, draftPlayer);
//                    }
//                    Toast.makeText(getContext(), R.string.backTranslation_playback_draft_recording, Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }
//
//    private void setRecordFilePath() {
//        /*FIXME
//        int nextDraftIndex = AudioFiles.INSTANCE.getBackTranslationTitles(StoryState.getStoryName(), slideNumber).length + 1;
//        File recordFile = AudioFiles.INSTANCE.getBackTranslation(StoryState.getStoryName(), slideNumber,getString(R.string.backTranslation_record_file_backT_name, nextDraftIndex));
//        while (recordFile.exists()) {
//            nextDraftIndex++;
//            recordFile = AudioFiles.INSTANCE.getBackTranslation(StoryState.getStoryName(), slideNumber, getString(R.string.backTranslation_record_file_backT_name, nextDraftIndex));
//        }
//        backTranslationRecordingFile = recordFile;
//        */
//    }
//
//    /**
//     * Initializes the toolbar and toolbar buttons.
//     */
//    private void setToolbar(View toolbar) {
//        /*FIXME
//        if (rootView instanceof RelativeLayout) {
//            String playBackFilePath = AudioFiles.INSTANCE.getBackTranslation(StoryState.getStoryName(), slideNumber).getPath();
//            RecordingToolbar.RecordingListener recordingListener = new RecordingToolbar.RecordingListener() {
//                @Override
//                public void onStoppedRecording() {
//                    //update to new recording path
//                    setRecordFilePath();
//                }
//                @Override
//                public void onStartedRecordingOrPlayback(boolean isRecording) {
//                    if(isRecording) {
//                        String title = AudioFiles.INSTANCE.getBackTranslationTitle(backTranslationRecordingFile);
//                        StorySharedPreferences.setBackTranslationForSlideAndStory(title, slideNumber, StoryState.getStoryName());
//                        //update to old recording or whatever was set by StorySharedPreferences.setDramatizationForSlideAndStory(title, slideNumber, StoryState.getStoryName());
//                        setPlayBackPath();
//                    }
//                }
//            };
//            BackTranslationListRecordingsModal modal = new BackTranslationListRecordingsModal(getContext(), slideNumber, this);
//
//            //TODO fix with proper slide num
//            recordingToolbar = new PausingRecordingToolbar(getActivity(), toolbar, (RelativeLayout)rootView,
//                    true, false, true, true, modal,recordingListener, 0);
//            recordingToolbar.keepToolbarVisible();
//        }
//        */
//    }
//
//    /**
//     * This function will set a listener to the passed in view so that when the passed in view
//     * is touched the keyboard close function will be called see: {@link #closeKeyboard(View)}.
//     *
//     * @param touchedView The view that will have an on touch listener assigned so that a touch of
//     *                    the view will close the softkeyboard.
//     */
//    private void closeKeyboardOnTouch(final View touchedView) {
//        if (touchedView != null) {
//            touchedView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    closeKeyboard(touchedView);
//                }
//            });
//        }
//    }
//
//    /**
//     * This function closes the keyboard. The passed in view will gain focus after the keyboard is
//     * hidden. The reestablished focus allows the removal of a cursor or any other focus indicator
//     * from the previously focused view.
//     *
//     * @param viewToFocus The view that will gain focus after the keyboard is hidden.
//     *
//     */
//    private void closeKeyboard(View viewToFocus) {
//        if(viewToFocus != null){
//            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(viewToFocus.getWindowToken(), 0);
//            viewToFocus.requestFocus();
//        }
//    }
//
//    //save remote consultant approval
//    private void saveConsultantApproval(){
//        SharedPreferences.Editor prefsEditor = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE).edit();
//        prefsEditor.putBoolean(storyName + IS_R_CONSULTANT_APPROVED, true);
//        prefsEditor.apply();
//    }
//
//
//    //initializes the checkmark button
//    private void setCheckmarkButton(final ImageButton button){
//        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
//        final String prefsKeyString = storyName + slideNumber + IS_CHECKED;
//        int isChecked = prefs.getInt(prefsKeyString, 0);
//        switch (isChecked) {
//            case 1:
//                //TODO: use non-deprecated method; currently used to support older devices
//                button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_checkmark_green, null));
//                break;
//            case -1:
//                //TODO: use non-deprecated method; currently used to support older devices
//                button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_checkmark_red, null));
//                break;
//            default:
//                button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_checkmark_yellow, null));
//                break;
//        }
//    }
//
//    //TODO: check to see if all slides have been approved.
//    public boolean checkAllMarked(){
//        /* FIXME
//        int marked;
//        SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
//        int numStorySlides = FileSystem.getContentSlideAmount(storyName);
//        for (int i = 0; i < numStorySlides; i ++) {
//            marked = prefs.getInt(storyName + i + IS_CHECKED, 0);
//            if (marked != 1) {
//                return false;
//            }
//        }
//        */
//        return true;
//    }
//
//    private void unlockDramatizationPhase(){
//        /* FIXME
//        Toast.makeText(getContext(), "Congrats!", Toast.LENGTH_SHORT).show();
//        saveConsultantApproval();
//        int dramatizationPhaseIndex = 6;
//        Phase[] phases = StoryState.getPhases();
//        StoryState.setCurrentPhase(phases[dramatizationPhaseIndex]);
//        Intent intent = new Intent(getContext(), StoryState.getCurrentPhase().getTheClass());
//        intent.putExtra(SLIDE_NUM, 0);
//        getActivity().startActivity(intent);
//        */
//    }
//
//    public void getSlidesStatus() {
//        /* FIXME
//
//        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
//        final SharedPreferences.Editor prefsEditor = prefs.edit();
//        String phone_id = Settings.Secure.getString(getContext().getContentResolver(),
//                Settings.Secure.ANDROID_ID);
//        js = new HashMap<>();
//
//
//        js.put("Key", getString(R.string.api_token));
//        js.put("PhoneId", phone_id);
//        js.put("TemplateTitle" , StoryState.getStoryName());
//
//        StringRequest req = new StringRequest(Request.Method.POST, getString(R.string.url_get_slide_status), new Response.Listener<String>() {
//
//
//
//            @Override
//            public void onResponse(String response) {
//                try {
//                     obj = new JSONObject(response);
//                }
//                catch(JSONException e){
//                    e.printStackTrace();
//                }
//
//                JSONArray arr = null;
//                try {
//
//                    arr = obj.getJSONArray("Status");
//                }
//                catch(JSONException e){
//                    e.printStackTrace();
//                }
//
//                //int[] arr = {-1,1,0,1,1,1};
//                if(arr != null) {
//                    for (int i = 0; i < arr.length(); i++) {
//                        //-1 not approved, 0 pending, 1 approved
//
//                        try {
//                            prefsEditor.putInt(storyName + i + IS_CHECKED, arr.getInt(i));
//                            prefsEditor.apply();
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//
//                    }
//                }
//
//                Log.i("LOG_VOLEY", response);
//
//                resp  = response;
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.e("LOG_VOLLEY_ERR_STATUS", error.toString());
//                Log.e("LOG_VOLLEY", "HIT ERROR");
//                //testErr = error.toString();
//
//            }
//
//        }) {
//            @Override
//            public Map<String, String> getParams() throws AuthFailureError {
//
//                return js;
//            }
//        };
//
//        VolleySingleton.getInstance(getActivity().getApplicationContext()).addToRequestQueue(req);
//        */
//    }
//
//    public void requestRemoteReview(Context con, int numSlides){
//
//        /*
//        final String phone_id = Settings.Secure.getString(con.getContentResolver(),
//                Settings.Secure.ANDROID_ID);
//        js = new HashMap<>();
//        js.put("Key", getString(R.string.api_token));
//        js.put("PhoneId", phone_id);
//        js.put("TemplateTitle", StoryState.getStoryName());
//        js.put("NumberOfSlides", Integer.toString(numSlides));
//
//        StringRequest req = new StringRequest(Request.Method.POST, getString(R.string.url_request_review), new Response.Listener<String>() {
//            @Override
//            public void onResponse(String response) {
//                Log.i("LOG_VOLLEY_RESP_RR", response);
//                resp  = response;
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.e("LOG_VOLLEY_ERR_RR", error.toString());
//                Log.e("LOG_VOLLEY", "HIT ERROR");
//                //testErr = error.toString();
//
//            }
//
//        }) {
//            @Override
//            protected Map<String, String> getParams()
//            {
//                return js;
//            }
//        };
//
//
//        VolleySingleton.getInstance(getActivity().getApplicationContext()).addToRequestQueue(req);
//        */
//    }
//
//    private void addTranscription() {
//        String transcript = transcriptionText.getText().toString();
//        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
//        final SharedPreferences.Editor prefsEditor = prefs.edit();
//        prefsEditor.putString(storyName + slideNumber + TRANSCRIPTION_TEXT, transcript);
//        prefsEditor.apply();
//
//    }

}
    
    

