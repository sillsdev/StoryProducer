package org.tyndalebt.spadv.tools.Network;

import com.android.volley.toolbox.Volley;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import android.content.Context;
import android.widget.Toast;

import org.tyndalebt.spadv.R;

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
                if(req.getUrl() == mCtx.getString(R.string.url_upload_audio)){
                    Toast.makeText(mCtx, R.string.queue_status_upload, Toast.LENGTH_SHORT).show();
                }else if(req.getUrl() == mCtx.getString(R.string.url_register_phone)){
                    Toast.makeText(mCtx, R.string.queue_status_register, Toast.LENGTH_SHORT).show();
                    //TODO: allow for the queueing of sending & receiving msgs right now it does
                    //TODO: allow for this as it causes major issues
                    //TODO: may eventually just want to use WebSockets for send/get msgs instead
                }else if(req.getUrl() == mCtx.getString(R.string.url_send_message)){
                    Toast.makeText(mCtx, R.string.remote_check_msg_no_connection, Toast.LENGTH_SHORT).show();
                    return;
                }else if(req.getUrl() == mCtx.getString(R.string.url_get_messages)){
                    Toast.makeText(mCtx, R.string.queue_status_message_get, Toast.LENGTH_SHORT).show();
                    return;
                }else if(req.getUrl() == mCtx.getString(R.string.url_get_slide_status)){
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


