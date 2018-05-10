package org.sil.storyproducer.tools.Network;

import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import static android.content.Context.CONNECTIVITY_SERVICE;


/**
 * Created by btburns on 2/21/2018.
 */

public class ConnectivityStatus extends ContextWrapper {

    public ConnectivityStatus(Context base){
        super(base);
    }

    public static boolean isConnected(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo connection = manager.getActiveNetworkInfo();
        return connection != null && connection.isConnectedOrConnecting();
    }

}
