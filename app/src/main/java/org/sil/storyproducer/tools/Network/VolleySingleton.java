package org.sil.storyproducer.tools.Network;

import com.android.volley.toolbox.Volley;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Brendon on 11/13/17.
 */

public class VolleySingleton {
        private static VolleySingleton mInstance;
        private RequestQueue mRequestQueue;
        private RequestQueue offlineQueue;

        private static Context mCtx;

        private VolleySingleton(Context context) {
            mCtx = context;
            mRequestQueue = getRequestQueue();
            offlineQueue = Volley.newRequestQueue(context);
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
            }
            return mRequestQueue;
        }

        public <T> void addToRequestQueue(Request<T> req) {
            ConnectivityManager cm = (ConnectivityManager)mCtx.getSystemService(mCtx.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork !=null && activeNetwork.isConnectedOrConnecting();

            if(isConnected){
                getRequestQueue().add(req);
            }
            else{

                offlineQueue.add(req);
            }
        }
        public void startOffline(){
            offlineQueue.start();
        }
        public void stopOffline(){
            offlineQueue.stop();
        }

    }


