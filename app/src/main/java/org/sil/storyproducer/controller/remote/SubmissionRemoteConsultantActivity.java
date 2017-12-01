package org.sil.storyproducer.controller.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.apache.commons.io.IOUtils;
import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.phase.PhaseBaseActivity;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.Network.BackTranslationUpload;
import org.sil.storyproducer.tools.Network.BetterStringRequest;
import org.sil.storyproducer.tools.Network.VolleySingleton;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.FileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.sil.storyproducer.controller.remote.BackTranslationFrag.IS_R_CONSULTANT_APPROVED;
import static org.sil.storyproducer.controller.remote.BackTranslationFrag.R_CONSULTANT_PREFS;

/**
 * Created by annmcostantino on 11/4/2017.
 */

public class SubmissionRemoteConsultantActivity extends PhaseBaseActivity {

    private Button submitButton;
    private TextView submissionText;
    private boolean submitForReview;
    private boolean uploadBackTranslations;
    private String resp;
    private String testErr;
    private Map<String,String> js;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission_remote_consultant);
        submitButton = (Button)findViewById(R.id.submit_backtranslations);
        submissionText = (TextView)findViewById(R.id.submission_status_text);
        setSubmissionStatusText();
    }

    //TODO: new icon
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item =  menu.getItem(0);
        item.setIcon(R.drawable.ic_concheck);
        return true;
    }

    //TODO: Subroutine: Change text and button visibility based on conditions.

    private void setSubmissionStatusText(){
        SharedPreferences prefs = getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE);
        boolean isApproved  = prefs.getBoolean(StoryState.getStoryName() + IS_R_CONSULTANT_APPROVED,false);
        isApproved = false;
        //can submit
        //cond: all slides have a recording
        submissionText.setText(R.string.recordings_ready);
        submitButton.setVisibility(View.VISIBLE);

        //sent & awaiting response
        //cond: all slides have yellow status
        if(resp == null & uploadBackTranslations == true){
            submissionText.setText(R.string.recordings_sent);
            submitButton.setVisibility(View.GONE);
        }

        //not all accepted please re-record appropriate slides and resubmit
        //cond:any slide has red status
        if(uploadBackTranslations== true && !isApproved)
        submissionText.setText(R.string.recordings_disapproved);
        submitButton.setVisibility(View.VISIBLE);

        //all accepted
        //cond: all slides green status
        if(isApproved) {
            submissionText.setText(R.string.recordings_approved);
            submitButton.setVisibility(View.GONE);
        }

        submitButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                postBackTranslations();
            }
        });
    }

    //TODO: send the audio files to the server on button click
    private void postBackTranslations(){
        Context con = getApplicationContext();

        int numSlides = FileSystem.getContentSlideAmount(StoryState.getStoryName()); // need ot get the number of slides in story or that have been changed

        //Request Remote Review
       // requestRemoteReview(con, numSlides);

        //Loop through UploadSlideBacktranslation until out of slides
        for(int i =0; i< numSlides; i++){
            File slide = AudioFiles.getBackTranslation(StoryState.getStoryName(), i);

            try {
                Upload(slide, con, i);
             }
             catch(IOException e){
                e.printStackTrace();
            }
        }
        uploadBackTranslations =true;




    }

    private void requestRemoteReview(Context con, int numSlides){

        Context myContext = con;

        final String url = "http://storyproducer.azurewebsites.net/API/RequestRemoteReview.php";
        final String api_token = "XUKYjBHCsD6OVla8dYAt298D9zkaKSqd";
        final String phone_id = Settings.Secure.getString(myContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        js = new HashMap<String,String>();
        js.put("Key", api_token);
        js.put("PhoneId", phone_id);
        js.put("TemplateTitle", StoryState.getStoryName());
        js.put("NumberOfSlides", Integer.toString(numSlides));

        StringRequest req = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("LOG_VOLEY", response.toString());
                resp  = response;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG_VOLLEY", error.toString());
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


        RequestQueue test = VolleySingleton.getInstance(myContext).getRequestQueue();

        test.add(req);

    }


        public void Upload ( final File fileName, Context con, int slide) throws IOException {


        final String api_token = "XUKYjBHCsD6OVla8dYAt298D9zkaKSqd";
        final String token =     "XUKYjBHCsD6OVla8dYAt298D9zkaKSqd";
        Context myContext = con;
        String phone_id = Settings.Secure.getString(myContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String templateTitle = StoryState.getStoryName();

        String currentSlide = Integer.toString(slide);
        InputStream input = new FileInputStream(fileName);
        byte[] audioBytes = IOUtils.toByteArray(input);

        String byteString = Base64.encodeToString( audioBytes ,Base64.DEFAULT);
        String url = "http://storyproducer.azurewebsites.net/API/UploadSlideBacktranslation.php";

            js = new HashMap<String,String>();
        js.put("Key", api_token);
        js.put("PhoneId", phone_id);
        js.put("TemplateTitle", templateTitle);
        js.put("SlideNumber", currentSlide);
        js.put("Data", byteString);


            BetterStringRequest req = new BetterStringRequest(Request.Method.POST, url, js, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("LOG_VOLEY", response.toString());
                    resp  = response;
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("LOG_VOLLEY", error.toString());
                    Log.e("LOG_VOLLEY", "HIT ERROR");
                    testErr = error.toString();

                }

            }) {
                @Override
                protected Map<String, String> getParams()
                {
                    return this.mParams;
                }
            };


        RequestQueue test = VolleySingleton.getInstance(myContext).getRequestQueue();

        test.add(req);

    }




}
