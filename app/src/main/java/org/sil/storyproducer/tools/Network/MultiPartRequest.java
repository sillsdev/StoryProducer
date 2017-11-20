/*
package org.sil.storyproducer.tools.Network;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.AuthFailureError;
import com.android.volley.VolleyError;
import com.android.volley.ParseError;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.android.volley.toolbox.HttpHeaderParser;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;


*/
/**
 * Created by Brendon on 11/15/17.
 *//*


public class MultiPartRequest extends Request<NetworkResponse> {

    private final Response.ErrorListener mErrorListener;
    private final Map<String, String> mHeaders;
    private final String mMimeType;
    private final byte[] mMultipartBody;
    private final Response.Listener<NetworkResponse> mListener;

    public MultiPartRequest(String url, Map<String, String> headers, String mimeType, byte[] multipartBody, Response.Listener<NetworkResponse> listener, Response.ErrorListener errorListener) {
        super(Method.POST, url, errorListener);
        this.mListener = listener;
        this.mErrorListener = errorListener;
        this.mHeaders = headers;
        this.mMimeType = mimeType;
        this.mMultipartBody = multipartBody;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return (mHeaders != null) ? mHeaders : super.getHeaders();
    }

    @Override
    public String getBodyContentType() {
        return mMimeType;
    }
    

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        try {
            return Response.success(
                    response,
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        mErrorListener.onErrorResponse(error);
    }
    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // populate text payload
            Map<String, String> params = getParams();
            if (params != null && params.size() > 0) {
                textParse(dos, params, getParamsEncoding());
            }

            // populate data byte payload
            Map<String, byte[]> data = getByteData();
            if (data != null && data.size() > 0) {
                dataParse(dos, data);
                return null;
            }

        } catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }



    protected Map<String, byte[]> getByteData() throws AuthFailureError {
        return null;
    }

    private void dataParse(DataOutputStream dataOutputStream, Map<String, byte[]> data) throws IOException{
        dataOutputStream.
        for(Map.Entry<String, byte[]> entry :data.entrySet()){
           // buildDataPart(dataOutputStream, entry.getValue(), entry.getKey());

        }
    }
}

*/
