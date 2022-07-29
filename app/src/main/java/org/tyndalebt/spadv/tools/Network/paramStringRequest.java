package org.tyndalebt.spadv.tools.Network;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import java.util.Map;

public class paramStringRequest extends StringRequest {
    public final Map<String, String> mParams;
    public paramStringRequest(int method , String url, Map<String, String> params,
                              Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        this.mParams = params;
    }
}



