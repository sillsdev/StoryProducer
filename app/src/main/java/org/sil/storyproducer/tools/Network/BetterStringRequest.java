package org.sil.storyproducer.tools.Network;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import org.sil.storyproducer.tools.Network.VolleySingleton.*;
import java.util.Map;

public class BetterStringRequest extends StringRequest {

    //private final Response.ErrorListener mErrorListener;
    public final Map<String, String> mParams;
  //  private final Response.Listener<NetworkResponse> mListener;
   // private final Response.Listener<String> listener;

    public BetterStringRequest(int method ,String url, Map<String, String> params,  Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);

        this.mParams = params;

    }



}

