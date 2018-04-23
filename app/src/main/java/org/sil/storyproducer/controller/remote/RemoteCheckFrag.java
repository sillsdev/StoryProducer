package org.sil.storyproducer.controller.remote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
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
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.model.messaging.Message;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.Network.VolleySingleton;
import org.sil.storyproducer.tools.Network.paramStringRequest;
import org.sil.storyproducer.tools.file.AudioFiles;
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
    private static final String TO_SEND_MESSAGE = "SND_MSG";

    private View rootView;
    private int slideNumber;
    private String storyName;
    private AudioPlayer draftPlayer;
    private boolean draftAudioExists;
    private File backTranslationRecordingFile = null;
    private TextView messageTitle;
    private Button sendMessageButton;
    private EditText messageSent;

    private JSONObject obj;
    private String resp;
    private Map<String, String> js;

    private MessageAdapter msgAdapter;
    private ListView messagesView;
    private PausingRecordingToolbar recordingToolbar;

    private Toast successToast;
    private Toast noConnection;
    private Toast unknownError;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Bundle passedArgs = this.getArguments();
        slideNumber = passedArgs.getInt(SLIDE_NUM);
        storyName = StoryState.getStoryName();
        setHasOptionsMenu(true);
        msgAdapter = new MessageAdapter(getContext());
        successToast = Toast.makeText(getActivity().getApplicationContext(), R.string.remote_check_msg_sent, Toast.LENGTH_SHORT);
        noConnection = Toast.makeText(getActivity().getApplicationContext(), R.string.remote_check_msg_no_connection, Toast.LENGTH_SHORT);
        unknownError = Toast.makeText(getActivity().getApplicationContext(),R.string.remote_check_msg_failed, Toast.LENGTH_SHORT);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_remote_check_layout, container, false);

        //draftPlayButton = (ImageButton)rootView.findViewById(R.id.fragment_remote_check_play_draft_button);
        messageTitle = (TextView)rootView.findViewById(R.id.messaging_title);
        messagesView = (ListView) rootView.findViewById(R.id.message_history);
        messagesView.setAdapter(msgAdapter);
        sendMessageButton = (Button)rootView.findViewById(R.id.button_send_msg);
        messageSent = (EditText)rootView.findViewById(R.id.sendMessage);

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
        //set texts for this view
        final String titleString = " " + slideNumber;
        messageTitle.append(titleString);
        messageSent.setHint(R.string.message_hint);
        messageSent.setHintTextColor(ContextCompat.getColor(getContext(),R.color.black));
        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        messageSent.setText(prefs.getString(storyName+slideNumber+TO_SEND_MESSAGE, ""));
        getMessages();
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on pause.
     */
    @Override
    public void onPause() {
        super.onPause();
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
        String playBackFilePath = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).getPath();
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
                Message m = new Message(true, messageSent.getText().toString());
                msgAdapter.add(m);
                messagesView.setSelection(messagesView.getCount());

                //set text back to blank
                prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, "");
                prefsEditor.apply();
                messageSent.setText("");

                successToast.show();
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
                    noConnection.show();
                }
                else {
                    unknownError.show();
                }
            }

        }) {
            @Override
            protected Map<String, String> getParams()
            {
                return this.mParams;
            }
        };


        VolleySingleton.getInstance(getActivity().getApplicationContext()).addToRequestQueue(req);

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
        js.put("LastId", Integer.toString(msgAdapter.getCount()-1));


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
                //TODO: save the receieved msgs to data struct
                for(int j=0; j<msgs.length();j++){

                    try{
                        JSONObject currMsg = msgs.getJSONObject(j);
                        int num = currMsg.getInt("IsTranslator");
                        boolean isFromTranslator;
                        if(num == 1){
                            isFromTranslator = true;
                        }
                        else{
                            isFromTranslator = false;
                        }
                        String msg = currMsg.getString("Message");
                        Message m = new Message(isFromTranslator, msg);
                        msgAdapter.add(m);
                    }
                    catch(JSONException e){
                        e.printStackTrace();
                    }
                }

                messagesView.setSelection(msgAdapter.getCount());

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


        VolleySingleton.getInstance(getActivity().getApplicationContext()).addToRequestQueue(req);
    }

}
