package com.jude.http;

import com.jude.volley.VolleyError;
import com.jude.volley.Response;

/**
 * ByteArrayLoadControler implements Volley Listener & ErrorListener
 * 
 * @author steven pan
 * 
 */
class ByteArrayLoadController extends AbsLoadController implements Response.Listener<byte[]>, Response.ErrorListener {
	
	private LoadListener mOnLoadListener;


	public ByteArrayLoadController(LoadListener requestListener) {
		this.mOnLoadListener = requestListener;
	}

	@Override
	public void onErrorResponse(VolleyError error) {
		String errorMsg = null;
		if (error.getMessage() != null) {
			errorMsg = error.getMessage();
		} else {
			try {
				errorMsg = "Server Response Error (" + error.networkResponse.statusCode + ")";
			} catch (Exception e) {
				errorMsg = "Server Response Error";
			}
		}
		this.mOnLoadListener.onError(errorMsg, getOriginUrl());
	}

	@Override
	public void onResponse(byte[] response) {
		this.mOnLoadListener.onSuccess(response, getOriginUrl());
	}
}