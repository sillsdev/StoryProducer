package org.tyndalebt.spadv.tools.Network;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by brendon burns on 11/16/17.
 */

public class BackTranslationUpload {
        public static String resp;
        public static String testErr;
        public static Map<String,String> js;

        public static void Upload ( final File fileName, Context con, int slide) throws IOException {
        /* FIXME
            final String api_token = "XUKYjBHCsD6OVla8dYAt298D9zkaKSqd";
            final String token =     "XUKYjBHCsD6OVla8dYAt298D9zkaKSqd";
            String phone_id = Settings.Secure.getString(con.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            String templateTitle = StoryState.getStoryName();
          //  File testFile = AudioFiles.getBackTranslation(StoryState.getStoryName(), StoryState.getCurrentStorySlide(), fileName);
          //  File testFile = file;
            String currentSlide = Integer.toString(slide);
            InputStream input = new FileInputStream(fileName);
            byte[] audioBytes = IOUtils.toByteArray(input);

            String byteString = Base64.encodeToString( audioBytes ,Base64.DEFAULT);
            String url = "https://storyproducer.eastus.cloudapp.azure.com/API/UploadSlideBacktranslation.php";

            js = new HashMap<>();
             js.put("Key", api_token);
             js.put("PhoneId", phone_id);
             js.put("TemplateTitle", templateTitle);
             js.put("SlideNumber", currentSlide);
             js.put("Data", byteString);


            // }
            //  catch(JSONException e){
            //     e.printStackTrace();
            // }

           // Log.i("LOG_VOLLEY", js.toString());
            StringRequest req = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("LOG_VOLEY", response);
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


            VolleySingleton.getInstance(con).addToRequestQueue(req);
*/
        }


}
