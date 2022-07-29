/*
package org.tyndalebt.spadv.tools.Network;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.*;
import com.android.volley.VolleyError;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.JSONException;
import org.tyndalebt.spadv.model.StoryState;
import org.tyndalebt.spadv.tools.file.AudioFiles;
import org.apache.commons.io.FileUtils;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

*/
/*
 * Created by alexamhepner on 11/15/17.
 *//*


public class AudioUpload {



    public AudioUpload() throws IOException {
    }

    public static void Upload(final String fileName, Context con) throws IOException{

        final String api_token = "XUKYjBHCsD6OVLa8dYAt298DzkaKSqd";
        Context myContext = con;
        String phone_id = Settings.Secure.getString(myContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String templateTitle = StoryState.getStoryName();
        File testFile = AudioFiles.getBackTranslation(StoryState.getStoryName(), StoryState.getCurrentStorySlide(), fileName);
        InputStream input = new FileInputStream(testFile);
        byte[] audioBytes = IOUtils.toByteArray(input);


        String url = "http://www.angga-ari.com/api/something/awesome";
     MultiPartRequest multipartRequest = new MultiPartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                String resultResponse = new String(response.data);
                try {
                    JSONObject result = new JSONObject(resultResponse);
                    String status = result.getString("status");
                    String message = result.getString("message");

                    if (status.equals(Constant.REQUEST_SUCCESS)) {
                        // tell everybody you have succed upload image and post strings
                        Log.i("Messsage", message);
                    } else {
                        Log.i("Unexpected", message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                    }
                } else {
                    String result = new String(networkResponse.data);
                    try {
                        JSONObject response = new JSONObject(result);
                        String status = response.getString("status");
                        String message = response.getString("message");

                        Log.e("Error Status", status);
                        Log.e("Error Message", message);

                        if (networkResponse.statusCode == 404) {
                            errorMessage = "Resource not found";
                        } else if (networkResponse.statusCode == 401) {
                            errorMessage = message+" Please login again";
                        } else if (networkResponse.statusCode == 400) {
                            errorMessage = message+ " Check your inputs";
                        } else if (networkResponse.statusCode == 500) {
                            errorMessage = message+" Something is getting wrong";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.i("Error", errorMessage);
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("api_token", "gh659gjhvdyudo973823tt9gvjf7i6ric75r76");

                return params;
            }

            @Override
            protected Map<String, byte[]> getByteData() {
                Map<String, byte[] > params = new HashMap<>();
                // file name could found file base or direct access from real path
                // for now just get bitmap data from ImageView

                params.put("audio", audioBytes );
                return params;
            }
        };

        VolleySingleton.getInstance(myContext).addToRequestQueue(multipartRequest);
    }

}
*/
