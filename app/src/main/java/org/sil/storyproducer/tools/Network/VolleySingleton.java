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
                }
            }
            return mRequestQueue;
        }

        public <T> void addToRequestQueue(Request<T> req) {

            getRequestQueue().add(req);

        }
        public void startQueue(){ getRequestQueue().start(); }
        public void stopQueue(){ getRequestQueue().stop(); }

    }


