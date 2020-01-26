package org.sil.storyproducer.tools.Network;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Workspace;

/**
 * Created by Brendon on 11/13/17.
 */

public class VolleySingleton {
        private static VolleySingleton mInstance;
        private RequestQueue mRequestQueue;
        private boolean isStopped;
        private static Context mCtx;

        private VolleySingleton(Context context) {
            mCtx = context;
            mRequestQueue = getRequestQueue();
        }

        public static synchronized VolleySingleton getInstance(Context context) {
            if (mInstance == null) {
                mInstance = new VolleySingleton(context);
            }
            return mInstance;
        }

        public RequestQueue getRequestQueue() {
            if (mRequestQueue == null) {
                // getApplicationContext() is key, it keeps you from leaking the
                // Activity or BroadcastReceiver if someone passes one in.
                mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
                boolean isConnected = ConnectivityStatus.isConnected(mCtx);
                if(!isConnected){
                    stopQueue();
                    isStopped = true;
                }
            }
            return mRequestQueue;
        }

        public <T> void addToRequestQueue(Request<T> req) {


            if(isStopped){
                //notify currently no connection
                String rup = Workspace.INSTANCE.getRoccUrlPrefix(mCtx);
                String uploadAudioUrl = rup + mCtx.getString(R.string.url_upload_audio);
                String registerPhoneUrl = rup + mCtx.getString(R.string.url_register_phone);
                String sendMessageUrl = rup + mCtx.getString(R.string.url_send_message);
                String getMessagesUrl = rup + mCtx.getString(R.string.url_get_messages);
                String getSlideStatusUrl = rup + mCtx.getString(R.string.url_get_slide_status);
                // TODO @pwhite: This is basically a switch statement on what
                // type of request the parameter is. I'll have to look more
                // thoroughly through the code to understand, but I think it
                // would be better if we didn't have to compare URL strings to
                // see which one of these kinds of requests it is. It would be
                // better if there were an enum that has all possible kinds of
                // requests. This would make it unnecessary to re-compute the
                // request urls here.
                if (req.getUrl() == uploadAudioUrl) {
                    Toast.makeText(mCtx, R.string.queue_status_upload, Toast.LENGTH_SHORT).show();
                } else if (req.getUrl() == registerPhoneUrl) {
                    Toast.makeText(mCtx, R.string.queue_status_register, Toast.LENGTH_SHORT).show();
                    //TODO: allow for the queueing of sending & receiving msgs right now it does
                    //TODO: allow for this as it causes major issues
                    //TODO: may eventually just want to use WebSockets for send/get msgs instead
                } else if (req.getUrl() == sendMessageUrl) {
                    Toast.makeText(mCtx, R.string.remote_check_msg_no_connection, Toast.LENGTH_SHORT).show();
                    return;
                } else if (req.getUrl() == getMessagesUrl) {
                    Toast.makeText(mCtx, R.string.queue_status_message_get, Toast.LENGTH_SHORT).show();
                    return;
                } else if (req.getUrl() == getSlideStatusUrl) {
                    Toast.makeText(mCtx, R.string.queue_status_approved, Toast.LENGTH_SHORT).show();
                    return;
                }

            }
            getRequestQueue().add(req);

        }
        public void startQueue(){
            getRequestQueue().start();
            isStopped = false;
        }
        public void stopQueue(){
            getRequestQueue().stop();
            isStopped = true;
        }

    }


