package org.sil.storyproducer.controller.remote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.adapter.MessageAdapter;
import org.sil.storyproducer.controller.dramatization.DramaListRecordingsModal;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.model.messaging.Message;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.Network.VolleySingleton;
import org.sil.storyproducer.tools.Network.paramStringRequest;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.toolbar.PausingRecordingToolbar;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by annmcostantino on 2/19/2018.
 */

public class RemoteCheckFrag extends Fragment {

    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    public static final String R_CONSULTANT_PREFS = "Consultant_Checks";
    public static final String IS_R_CONSULTANT_APPROVED = "isApproved";
    private static final String IS_CHECKED = "isCheckedi";
    private static final String RECEIVED_MESSAGE = "REC_MSG";
    private static final String WAS_RECEIVED = "WAS_RECVD";
    private static final String TO_SEND_MESSAGE = "SND_MSG";
    private static final String WAS_SENT = "WAS_SENT";

    private View rootView;
    private View rootViewToolbar;
    private int slideNumber;
    private EditText slideText;
    private String storyName;
    private boolean phaseUnlocked = false;
    private AudioPlayer draftPlayer;
    private boolean draftAudioExists;
    private File backTranslationRecordingFile = null;
    //private ImageButton draftPlayButton;
    private Button sendMessageButton;
    private TextView messageReceieved;
    private EditText messageSent;
    private boolean isSlidesChecked = false;

    private JSONObject obj;
    private String resp;
    private Map<String, String> js;

    private MessageAdapter msgAdapter;
    private ListView messagesView;
    private PausingRecordingToolbar recordingToolbar;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Bundle passedArgs = this.getArguments();
        slideNumber = passedArgs.getInt(SLIDE_NUM);
        storyName = StoryState.getStoryName();
        //setRecordFilePath();
        setHasOptionsMenu(true);
        msgAdapter = new MessageAdapter(getContext());


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_remote_check_layout, container, false);

        //draftPlayButton = (ImageButton)rootView.findViewById(R.id.fragment_remote_check_play_draft_button);

        messagesView = (ListView) rootView.findViewById(R.id.message_history);
        messagesView.setAdapter(msgAdapter);
        sendMessageButton = (Button)rootView.findViewById(R.id.button_send_msg);

        //messageReceieved = (TextView)rootView.findViewById(R.id.message_history);
        messageSent = (EditText)rootView.findViewById(R.id.sendMessage);

        //setUiColors();
        //setPic((ImageView)rootView.findViewById(R.id.fragment_remote_check_image_view), slideNumber);
        //setCheckmarkButton((ImageButton)rootView.findViewById(R.id.fragment_remote_check_r_concheck_checkmark_button));
        //TextView slideNumberText = (TextView) rootView.findViewById(R.id.slide_number_text);
        //slideNumberText.setText(slideNumber + "");

        //rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false);
        closeKeyboardOnTouch(rootView);


        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item =  menu.getItem(0);
        super.onCreateOptionsMenu(menu, inflater);
        item.setIcon(R.drawable.ic_message);
    }

    public void onStart() {
        super.onStart();

        //setToolbar(rootViewToolbar);

        /*draftPlayer = new AudioPlayer();
        File draftAudioFile = AudioFiles.getDraft(storyName, slideNumber);
        if (draftAudioFile.exists()) {
            draftAudioExists = true;
            draftPlayer.setPath(draftAudioFile.getPath());
        } else {
            draftAudioExists = false;
        }
        draftPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                draftPlayButton.setBackgroundResource(R.drawable.ic_play_gray);
            }
        });

        setPlayStopDraftButton((ImageButton)rootView.findViewById(R.id.fragment_remote_check_play_draft_button)); */
        //set text
        messageSent.setHint(R.string.message_hint);
        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        messageSent.setText(prefs.getString(storyName+slideNumber+TO_SEND_MESSAGE, ""));
        getMessages();
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        //dramatize phase not unlocked yet
        /*final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        final String prefsKeyString = storyName + IS_R_CONSULTANT_APPROVED;

        //if story not approved yet, receieve updates
        if(!prefs.getBoolean(prefsKeyString, false)) {
            getSlidesStatus();
            setCheckmarkButton((ImageButton) rootView.findViewById(R.id.fragment_remote_check_r_concheck_checkmark_button));

            //set receive text
            if(prefs.getBoolean(storyName+slideNumber+WAS_RECEIVED, false)) {
                messageReceieved.setText(prefs.getString(storyName+slideNumber+RECEIVED_MESSAGE,""));
            }
            //set send text
            messageSent.setText(prefs.getString(storyName+slideNumber+TO_SEND_MESSAGE, ""));

            phaseUnlocked = checkAllMarked();
            if (phaseUnlocked) {
                unlockDramatizationPhase();
            }
        }*/

    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on pause.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (recordingToolbar != null) {
            recordingToolbar.onClose();
        }
        closeKeyboard(rootView);

        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, messageSent.getText().toString());
        prefsEditor.apply();

        //TODO:save message adapter?
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on stop.
     */
    @Override
    public void onStop()  {
        super.onStop();
        //draftPlayer.release();
        if(recordingToolbar != null){
            recordingToolbar.onClose();
            recordingToolbar.releaseToolbarAudio();
        }

        closeKeyboard(rootView);
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is visible to user
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                if (recordingToolbar != null) {
                    recordingToolbar.onClose();
                }
                closeKeyboard(rootView);
            }
        }
    }

    /**
     * Used to stop playing and recording any media. The calling class should be responsible for
     * stopping its own media. Used in {@link DramaListRecordingsModal}.
     */
    public void stopPlayBackAndRecording() {
        recordingToolbar.stopToolbarMedia();
    }

    /**
     * Used to hide the play and multiple recordings button.
     */
    public void hideButtonsToolbar(){
        recordingToolbar.hideButtons();
    }

    /**
     * sets the playback path
     */
    public void updatePlayBackPath() {
        String playBackFilePath = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber).getPath();
        recordingToolbar.setPlaybackRecordFilePath(playBackFilePath);
    }

    /**
     * Stop the toolbar from continuing the appending session.
     */
    //  public void stopAppendingRecordingFile(){
    //     recordingToolbar.stopAppendingSession();
    // }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    /*private void setUiColors() {
        if (slideNumber == 0) {
            LinearLayout rl = (LinearLayout) rootView.findViewById(R.id.fragment_remote_check_root_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }*/

    /**
     * This function is used to the set the picture per slide.
     *
     * @param slideImage    The view that will have the picture rendered on it.
     * @param slideNum The respective slide number for the dramatization slide.
     */
    private void setPic(final ImageView slideImage, int slideNum) {

        Bitmap slidePicture = ImageFiles.getBitmap(storyName, slideNum);

        if (slidePicture == null) {
            Snackbar.make(rootView, R.string.backTranslation_draft_no_picture, Snackbar.LENGTH_SHORT).show();
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
     * sets the playback path
     */
    /*public void setPlayBackPath() {
        String playBackFilePath = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber).getPath();
        recordingToolbar.setPlaybackRecordFilePath(playBackFilePath);
    }*/

    /**
     * This function serves to set the play and stop button for the draft playback button.
     */

    private void setPlayStopDraftButton(final ImageButton playPauseDraftButton) {

        if (!draftAudioExists) {
            //draft recording does not exist
            playPauseDraftButton.setAlpha(0.8f);
            playPauseDraftButton.setColorFilter(Color.argb(200, 200, 200, 200));
        } else {
            //remove x mark from ImageButton play
            playPauseDraftButton.setImageResource(0);
        }
        playPauseDraftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!draftAudioExists){
                    Toast.makeText(getContext(), R.string.backTranslation_no_draft_recording_available, Toast.LENGTH_SHORT).show();
                }
                else if (draftPlayer.isAudioPlaying()) {
                    draftPlayer.stopAudio();
                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_play_gray);
                } else {
                    //recordingToolbar.stopToolbarMedia();
                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_pause_gray);
                    draftPlayer.playAudio();

                    if(draftPlayer != null){ //if there is a draft available to play
                        //recordingToolbar.onToolbarTouchStopAudio(playPauseDraftButton, R.drawable.ic_play_gray, draftPlayer);
                    }
                    Toast.makeText(getContext(), R.string.backTranslation_playback_draft_recording, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*private void setRecordFilePath() {
        int nextDraftIndex = AudioFiles.getBackTranslationTitles(StoryState.getStoryName(), slideNumber).length + 1;
        File recordFile = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber,getString(R.string.backTranslation_record_file_backT_name, nextDraftIndex));
        while (recordFile.exists()) {
            nextDraftIndex++;
            recordFile = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber, getString(R.string.backTranslation_record_file_backT_name, nextDraftIndex));
        }
        backTranslationRecordingFile = recordFile;
    }*/

    /**
     * Initializes the toolbar and toolbar buttons.
     */
   /* private void setToolbar(View toolbar) {
        if (rootView instanceof RelativeLayout) {
            String playBackFilePath = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber).getPath();
            RecordingToolbar.RecordingListener recordingListener = new RecordingToolbar.RecordingListener() {
                @Override
                public void onStoppedRecording() {
                    //update to new recording path
                    setRecordFilePath();
                    recordingToolbar.setRecordFilePath(backTranslationRecordingFile.getAbsolutePath());
                }
                @Override
                public void onStartedRecordingOrPlayback(boolean isRecording) {
                    if(isRecording) {
                        String title = AudioFiles.getBackTranslationTitle(backTranslationRecordingFile);
                        StorySharedPreferences.setBackTranslationForSlideAndStory(title, slideNumber, StoryState.getStoryName());
                        //update to old recording or whatever was set by StorySharedPreferences.setDramatizationForSlideAndStory(title, slideNumber, StoryState.getStoryName());
                        setPlayBackPath();
                    }
                }
            };
            BackTranslationListRecordingsModal modal = new BackTranslationListRecordingsModal(getContext(), slideNumber, this);

            recordingToolbar = new PausingRecordingToolbar(getActivity(), toolbar, (RelativeLayout)rootView,
                    true, false, true, true, playBackFilePath, backTranslationRecordingFile.getAbsolutePath(), modal,recordingListener);
            recordingToolbar.keepToolbarVisible();
        }
    } */

    /**
     * This function will set a listener to the passed in view so that when the passed in view
     * is touched the keyboard close function will be called see: {@link #closeKeyboard(View)}.
     *
     * @param touchedView The view that will have an on touch listener assigned so that a touch of
     *                    the view will close the softkeyboard.
     */
    private void closeKeyboardOnTouch(final View touchedView) {
        if (touchedView != null) {
            touchedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeKeyboard(touchedView);
                }
            });
        }
    }

    /**
     * This function closes the keyboard. The passed in view will gain focus after the keyboard is
     * hidden. The reestablished focus allows the removal of a cursor or any other focus indicator
     * from the previously focused view.
     *
     * @param viewToFocus The view that will gain focus after the keyboard is hidden.
     *
     */
    private void closeKeyboard(View viewToFocus) {
        if(viewToFocus != null){
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(viewToFocus.getWindowToken(), 0);
            viewToFocus.requestFocus();
        }
    }

    //save remote consultant approval
    private void saveConsultantApproval(){
        SharedPreferences.Editor prefsEditor = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE).edit();
        prefsEditor.putBoolean(storyName + IS_R_CONSULTANT_APPROVED, true);
        prefsEditor.apply();
    }


    //initializes the checkmark button
    private void setCheckmarkButton(final ImageButton button){
        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        final String prefsKeyString = storyName + slideNumber + IS_CHECKED;
        int isChecked = prefs.getInt(prefsKeyString, 0);
        if(isChecked == 1) {
            //TODO: use non-deprecated method; currently used to support older devices
            button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_checkmark_green, null));
        } else if (isChecked == -1) {
            //TODO: use non-deprecated method; currently used to support older devices
            button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_checkmark_red, null));
        } else{
            button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_checkmark_yellow, null));
        }
    }

    //TODO: check to see if all slides have been approved.
    public boolean checkAllMarked(){
        int marked;
        SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        int numStorySlides = FileSystem.getContentSlideAmount(storyName);
        for (int i = 0; i < numStorySlides; i ++) {
            marked = prefs.getInt(storyName + i + IS_CHECKED, 0);
            if (marked != 1) {
                return false;
            }
        }
        return true;
    }


    //TODO: unlock dramatize phase after all slides are approved
    private void unlockDramatizationPhase(){
        Toast.makeText(getContext(), "Congrats!", Toast.LENGTH_SHORT).show();
        saveConsultantApproval();
        int dramatizationPhaseIndex = 7;
        Phase[] phases = StoryState.getPhases();
        StoryState.setCurrentPhase(phases[dramatizationPhaseIndex]);
        Intent intent = new Intent(getContext(), StoryState.getCurrentPhase().getTheClass());
        intent.putExtra(SLIDE_NUM, 0);
        getActivity().startActivity(intent);
    }

    //function to send messages to remote consultant the given slide
    private void sendMessage(){

        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        final String api_token = "XUKYjBHCsD6OVla8dYAt298D9zkaKSqd";
        String phone_id = Settings.Secure.getString(getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String url = "https://storyproducer.eastus.cloudapp.azure.com/API/SendMessage.php";

        js = new HashMap<String,String>();


        //Get msg for current slide
        String  message = messageSent.getText().toString();
        //TODO: SANITIZE POTENTIAL HARMFUL MESSAGE BEFORE SENDING
        js.put("Message",message);
        js.put("Key", api_token);
        js.put("PhoneId", phone_id);
        js.put("StoryTitle" , StoryState.getStoryName());
        js.put("SlideNumber", Integer.toString(slideNumber));

        paramStringRequest req = new paramStringRequest(Request.Method.POST, url, js, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("LOG_VOLLEY_MSG", response.toString());
                resp  = response;
                //TODO: create new message bubble, save to data struct and add bubble to new view
                Message m = new Message(true, messageSent.getText().toString());
                msgAdapter.add(m);
                msgAdapter.notifyDataSetChanged();
                messagesView.setSelection(messagesView.getCount() - 1);

                //set text back to blank
                prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, "");
                prefsEditor.apply();
                messageSent.setText("");

                Toast.makeText(getContext(), R.string.remote_check_msg_sent, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG_VOLLEY_MSG_ERR", error.toString());
                Log.e("LOG_VOLLEY", "HIT ERROR");
                //Save the message to send next time
                prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, messageSent.getText().toString());
                prefsEditor.apply();

                if(error instanceof TimeoutError || error instanceof NoConnectionError || error
                        instanceof NetworkError || error instanceof ServerError ||
                        error instanceof AuthFailureError){
                    Toast.makeText(getContext(), R.string.remote_check_msg_no_connection, Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getContext(), R.string.remote_check_msg_failed, Toast.LENGTH_SHORT).show();
                }
            }

        }) {
            @Override
            protected Map<String, String> getParams()
            {
                return this.mParams;
            }
        };



        RequestQueue test = VolleySingleton.getInstance(getContext()).getRequestQueue();

        test.add(req);


    }

    private void getMessages(){
        final String api_token = "XUKYjBHCsD6OVla8dYAt298D9zkaKSqd";
        String phone_id = Settings.Secure.getString(getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String url = "https://storyproducer.eastus.cloudapp.azure.com/API/GetMessages.php";

        js = new HashMap<String,String>();

        js.put("Key", api_token);
        js.put("PhoneId", phone_id);
        js.put("StoryTitle" , StoryState.getStoryName());
        js.put("SlideNumber", Integer.toString(slideNumber));
        int lastId = msgAdapter.getCount()-1;
        if(lastId >= 0) {
            js.put("LastId", Integer.toString(lastId));
        }
        else{
            js.put("LastId",Integer.toString(0));
        }

        StringRequest req = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                //returns messages array IsTranslator: boolean + Message: String
                //returns LastId: Integer
                try {
                    obj = new JSONObject(response);
                }
                catch(JSONException e){
                    e.printStackTrace();
                }

                JSONArray msgs = null;

                try {
                    msgs = obj.getJSONArray("Messages");
                    //int id = obj.getInt("LastId");
                }
                catch(JSONException e){
                    e.printStackTrace();
                }


                //get all msgs and store into shared preferences
                //TODO: save the receieved msgs to data struct and create bubbles to add to new view
                for(int j=0; j<msgs.length();j++){

                    try{
                        boolean isFromTranslator = msgs.getBoolean(j);
                        String msg = msgs.getString(j);
                        Message m = new Message(isFromTranslator, msg);
                        msgAdapter.add(m);
                        msgAdapter.notifyDataSetChanged();
                    }
                    catch(JSONException e){
                        e.printStackTrace();
                    }
                }

                Log.i("LOG_VOLEY", response.toString());

                resp  = response;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG_VOLLEY", error.toString());
                Log.e("LOG_VOLLEY", "HIT ERROR IN RECEIEVE MSG");
                //testErr = error.toString();

            }

        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {

                return js;
            }
        };


        RequestQueue test = VolleySingleton.getInstance(getContext()).getRequestQueue();

        test.add(req);


    }

    //function to get the slide status for all slides & any messages sent to the phone
    public void getSlidesStatus() {

        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        final String prefsKeyString = storyName + slideNumber + IS_CHECKED;

        final String api_token = "XUKYjBHCsD6OVla8dYAt298D9zkaKSqd";

        String phone_id = Settings.Secure.getString(getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        String url = "https://storyproducer.eastus.cloudapp.azure.com/API/GetSlideStatuses.php";

        js = new HashMap<String,String>();


        js.put("Key", api_token);
        js.put("PhoneId", phone_id);
        js.put("TemplateTitle" , StoryState.getStoryName());

        StringRequest req = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    obj = new JSONObject(response);
                }
                catch(JSONException e){
                    e.printStackTrace();
                }

                JSONArray arr = null;
                JSONArray msgs = null;
                try {

                    arr = obj.getJSONArray("Status");
                    msgs = obj.getJSONArray("Msgs");

                }
                catch(JSONException e){
                    e.printStackTrace();
                }

                //int[] arr = {-1,1,0,1,1,1};
                for(int i = 0; i<arr.length(); i++){
                    //-1 not approved, 0 pending, 1 approved

                    try {
                        prefsEditor.putInt(storyName + i + IS_CHECKED, arr.getInt(i));
                        prefsEditor.apply();
                    }
                    catch(JSONException e){
                        e.printStackTrace();
                    }

                }

                //get all msgs and store into shared preferences
                //TODO: save the receieved msgs to data struct and create bubbles to add to new view
                for(int j=0; j<msgs.length();j++){

                    try{
                        prefsEditor.putString(storyName + j + RECEIVED_MESSAGE, msgs.getString(j));
                        prefsEditor.apply();
                        if(msgs.getString(j) != ""){
                            prefsEditor.putBoolean(storyName + j + WAS_RECEIVED, true);
                            prefsEditor.apply();
                        }
                    }
                    catch(JSONException e){
                        e.printStackTrace();
                    }
                }

                Log.i("LOG_VOLEY", response.toString());

                resp  = response;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG_VOLLEY", error.toString());
                Log.e("LOG_VOLLEY", "HIT ERROR");
                //testErr = error.toString();

            }

        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {

                return js;
            }
        };


        RequestQueue test = VolleySingleton.getInstance(getContext()).getRequestQueue();

        test.add(req);


    }

}
