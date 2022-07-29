package org.tyndalebt.spadv.tools.Network;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


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
