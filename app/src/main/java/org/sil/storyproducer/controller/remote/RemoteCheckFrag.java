package org.sil.storyproducer.controller.remote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.adapter.MessageAdapter;
import org.sil.storyproducer.model.messaging.Message;
import org.sil.storyproducer.tools.Network.VolleySingleton;
import org.sil.storyproducer.tools.Network.paramStringRequest;
import org.sil.storyproducer.tools.media.AudioPlayer;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by annmcostantino on 2/19/2018.
 */

public class RemoteCheckFrag extends Fragment {

    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    public static final String R_CONSULTANT_PREFS = "Consultant_Checks";
    private static final String TO_SEND_MESSAGE = "SND_MSG";
    private static final String R_MESSAGE_HISTORY = "Message History";
    private static final String R_LAST_ID = "Last Int";

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

    private Toast successToast;
    private Toast noConnection;
    private Toast unknownError;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Bundle passedArgs = this.getArguments();
        slideNumber = passedArgs.getInt(SLIDE_NUM);
        //FIXME
        // storyName = StoryState.getStoryName();
        setHasOptionsMenu(true);
        successToast = Toast.makeText(getActivity().getApplicationContext(), R.string.remote_check_msg_sent, Toast.LENGTH_SHORT);
        noConnection = Toast.makeText(getActivity().getApplicationContext(), R.string.remote_check_msg_no_connection, Toast.LENGTH_SHORT);
        unknownError = Toast.makeText(getActivity().getApplicationContext(),R.string.remote_check_msg_failed, Toast.LENGTH_SHORT);

        //these lines should be put somewhere else (one time only)
        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        String phone_id = Settings.Secure.getString(getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        prefsEditor.putString("PhoneId", phone_id).apply();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_remote_check_layout, container, false);

        messageTitle = (TextView)rootView.findViewById(R.id.messaging_title);
        messagesView = (ListView) rootView.findViewById(R.id.message_history);

        sendMessageButton = (Button)rootView.findViewById(R.id.button_send_msg);
        messageSent = (EditText)rootView.findViewById(R.id.sendMessage);

        closeKeyboardOnTouch(rootView);


        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item =  menu.getItem(0);
        super.onCreateOptionsMenu(menu, inflater);
        item.setIcon(R.drawable.ic_message_white_24dp);
    }

    public void onStart() {
        super.onStart();

        //grab old adapter or make a new one
        msgAdapter = loadSharedPreferenceMessageHistory();
        messagesView.setAdapter(msgAdapter);

        //set texts for this view
        final String titleString = " " + slideNumber;
        messageTitle.append(titleString);
        messageSent.setHint(R.string.message_hint);
        messageSent.setHintTextColor(ContextCompat.getColor(getContext(),R.color.black));
        //load saved message draft and load saved message adapter
        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        messageSent.setText(prefs.getString(storyName+slideNumber+TO_SEND_MESSAGE, ""));
        getMessages();
        if(msgAdapter.getCount() > 0) {
            messagesView.setSelection(msgAdapter.getCount());
        }
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    /**
     * This function serves to save data when leaving phase or slide.
     */
    @Override
    public void onPause() {
        super.onPause();
        closeKeyboard(rootView);

        //save message draft
        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, messageSent.getText().toString()).apply();

        //save message adapter as well
        saveSharedPreferenceMessageHistory();
    }

    /**
     * This function serves to stop the audio streams from continuing after voicestudio has been
     * put on stop.
     */
    @Override
    public void onStop()  {
        super.onStop();
        closeKeyboard(rootView);
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

    //loads the (local) message history or creates a new one if it doesn't yet exist
    private MessageAdapter loadSharedPreferenceMessageHistory(){
        MessageAdapter msgAdapter = new MessageAdapter(getContext());
        List<Message> msgs;
        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        String json = prefs.getString(R_MESSAGE_HISTORY+storyName+slideNumber,"");
        int lastID = prefs.getInt(R_LAST_ID +storyName+slideNumber, -1);
        if(!json.isEmpty()){
            Type type = new TypeToken<List<Message>>(){

            }.getType();
            msgs = gson.fromJson(json, type);
            msgAdapter.setMessageHistory(msgs);
            msgAdapter.setLastID(lastID);

        }
        return msgAdapter;
    }

    //saves the (local) message history
    private void saveSharedPreferenceMessageHistory(){
        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        String json = gson.toJson(msgAdapter.getMessageHistory());
        int lastID = msgAdapter.getLastID();
        prefsEditor.putString(R_MESSAGE_HISTORY+storyName+slideNumber, json).apply();
        prefsEditor.putInt(R_LAST_ID+storyName+slideNumber, lastID).apply();
    }

    //function to send messages to remote consultant the given slide
    private void sendMessage(){
        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        String phone_id = prefs.getString(getString(R.string.PhoneId), "");
        js = new HashMap<String,String>();


        //Get msg for current slide
        String  message = messageSent.getText().toString();
        //TODO: SANITIZE POTENTIAL HARMFUL MESSAGE BEFORE SENDING
        js.put("Message",message);
        js.put("Key", getString(R.string.api_token));
        js.put("PhoneId", phone_id);
        //FIXME
        // js.put("StoryTitle" , StoryState.getStoryName());
        js.put("SlideNumber", Integer.toString(slideNumber));

        paramStringRequest req = new paramStringRequest(Request.Method.POST, getString(R.string.url_send_message), js, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("LOG_VOLLEY_MSG", response.toString());
                resp = response;

                try {
                    obj = new JSONObject(response);
                }
                catch(JSONException e){
                    e.printStackTrace();
                }

                try {
                    resp = obj.getString("Success");

                }
                catch(JSONException e){
                    e.printStackTrace();
                }
                if(resp != null){
                    if(resp == "true"){
                        //set text back to blank
                        prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, "");
                        prefsEditor.apply();
                        messageSent.setText("");
                        successToast.show();

                        //pull new messages from the server
                        getMessages();
                    }
                    else{
                        unknownError.show();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG_VOLLEY_MSG_ERR", error.toString());
                Log.e("LOG_VOLLEY", "HIT ERROR");
                //Save the message to send next time
                prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, messageSent.getText().toString());
                prefsEditor.apply();

                if(error instanceof NoConnectionError || error instanceof NetworkError
                        || error instanceof AuthFailureError){
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
        final SharedPreferences prefs = getActivity().getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        String phone_id = prefs.getString(getString(R.string.PhoneId), "");

        js = new HashMap<String,String>();
        js.put("Key", getString(R.string.api_token));
        js.put("PhoneId", phone_id);
        //FIXME
        // js.put("StoryTitle" , StoryState.getStoryName());
        js.put("SlideNumber", Integer.toString(slideNumber));
        js.put("LastId", Integer.toString(msgAdapter.getLastID()));


        StringRequest req = new StringRequest(Request.Method.POST, getString(R.string.url_get_messages), new Response.Listener<String>() {

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
                    int id = obj.getInt("LastId");
                    if(id!= -1) {
                        msgAdapter.setLastID(id);
                    }

                }
                catch(JSONException e){
                    e.printStackTrace();
                }


                //get all msgs and store into shared preferences
                if(msgs != null) {
                    for (int j = 0; j < msgs.length(); j++) {

                        try {
                            JSONObject currMsg = msgs.getJSONObject(j);
                            int num = currMsg.getInt("IsTranslator");
                            boolean isFromTranslator = (num == 1);
                            String msg = currMsg.getString("Message");
                            Message m = new Message(isFromTranslator, msg);
                            msgAdapter.add(m);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if (messagesView != null) {
                        if (msgAdapter.getCount() > 0) {
                            messagesView.setSelection(msgAdapter.getCount());
                        }
                    }
                }

                Log.i("LOG_VOLLEY", response.toString());

                resp  = response;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG_VOLLEY", error.toString());
                Log.e("LOG_VOLLEY", "HIT ERROR IN RECEIVE MSG");
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
