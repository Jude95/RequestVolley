package com.jude.http;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.jude.volley.AuthFailureError;
import com.jude.volley.DefaultRetryPolicy;
import com.jude.volley.Request;
import com.jude.volley.Request.Method;
import com.jude.volley.RequestQueue;
import com.jude.volley.RetryPolicy;
import com.jude.volley.VolleyError;
import com.jude.volley.toolbox.ImageLoader;
import com.jude.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


public class RequestManager {

	private static final String CHARSET_UTF_8 = "UTF-8";

	private int TIMEOUT_COUNT = 10 * 1000;

	private int RETRY_TIMES = 1;
	
	private boolean SHOULD_CACHE = false;

	private HashMap<String,String> HEADER;
	
	private volatile static RequestManager instance = null;

	private RequestQueue mRequestQueue = null;
	
	private NetworkImageCache mImageCache = null;
	
	private ImageLoader mImageLoader = null;
	
    private int times = 0;
	
	private boolean Debug = false;
	private String DebugTag;


	private RequestManager() {

	}
	
	public void setDebugMode(boolean isDebug,String DebugTag){
		this.Debug = isDebug;
		this.DebugTag = DebugTag;
	}
	
	public void init(Context context) {
		this.mRequestQueue = Volley.newRequestQueue(context);
		mImageCache = new NetworkImageCache(context);
		mImageLoader = new ImageLoader(RequestManager.getInstance()
				.getRequestQueue(), mImageCache);
	}

	public static RequestManager getInstance() {
		if (null == instance) {
			synchronized (RequestManager.class) {
				if (null == instance) {
					instance = new RequestManager();
				}
			}
		}
		return instance;
	}

	public RequestQueue getRequestQueue() {
		return this.mRequestQueue;
	}

	public void setTimeOut(int time){
		TIMEOUT_COUNT = time;
	}
	
	public void setRetryTimes(int times){
		RETRY_TIMES = times;
	}
	
	public void setCacheEnable(boolean isCache){
		SHOULD_CACHE = isCache;
	}
	
	public void setHeader(HashMap<String,String> header){
		HEADER = header;
	}

	public LoadController get(String url, RequestListener requestListener) {
		return this.request(Method.GET, url, null , null, requestListener, SHOULD_CACHE, TIMEOUT_COUNT, RETRY_TIMES);
	}
	
	public LoadController get(String url, HashMap<String,String> header,RequestListener requestListener, boolean shouldCache) {
		return this.request(Method.GET, url, null, header, requestListener, shouldCache, TIMEOUT_COUNT, RETRY_TIMES);
	}


	public LoadController post(final String url, Object data, final RequestListener requestListener) {
		return this.post(url, data, HEADER,requestListener, SHOULD_CACHE, TIMEOUT_COUNT, RETRY_TIMES);
	}

    public LoadController post(final String url, Object data, boolean checkCache,final RequestListener requestListener) {
        return this.post(url, data, HEADER,requestListener, checkCache, TIMEOUT_COUNT, RETRY_TIMES);
    }

    public void invalidate(String url, Object data){
        mRequestQueue.getCache().invalidate(url+data.toString(),true);
    }


	public LoadController post(final String url, Object data,HashMap<String,String> header, final RequestListener requestListener, boolean shouldCache,
			int timeoutCount, int retryTimes) {
		return request(Method.POST, url, data,header, requestListener, shouldCache, timeoutCount, retryTimes);
	}


    public ImageLoader.ImageContainer img(final String url,final ImageView imageView){
        return mImageLoader.get(url, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                if (response.getBitmap()!=null){
                    imageView.setImageBitmap(response.getBitmap());
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
    }

    public ImageLoader.ImageContainer img(final String url,final ImageView imageView, final int resError){
        return mImageLoader.get(url, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                if (response.getBitmap()!=null){
                    imageView.setImageBitmap(response.getBitmap());
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                imageView.setImageResource(resError);
            }
        });
    }

	public ImageLoader.ImageContainer img(final String url,final ImageLoader.ImageListener imageListener){
		return mImageLoader.get(url, imageListener);
	}

	public ImageLoader.ImageContainer img(final String url,final ImageLoader.ImageListener imageListener,int maxWidth,int maxHeight){
		return mImageLoader.get(url, imageListener, maxWidth, maxHeight);
	}
	

	public LoadController request(int method, final String url, final Object data, final Map<String, String> headers,
			final RequestListener requestListener, boolean shouldCache, int timeoutCount, int retryTimes) {
        final int curIndex = this.times++;
		return this.sendRequest(method, url, data, headers, new LoadListener() {
			@Override
			public void onStart() {
				if(requestListener!=null)
				requestListener.onRequest();
                if(Debug)Log.i(DebugTag, curIndex+"Times-Params:"+url+(data==null?"":data.toString()));
			}

			@Override
			public void onSuccess(byte[] data, String url) {
				
				String parsed = null;
				try {
					parsed = new String(data, CHARSET_UTF_8);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				if(Debug)Log.i(DebugTag, curIndex+"Times-Response:"+parsed);
				if(requestListener!=null)
				requestListener.onSuccess(parsed);
			}

			@Override
			public void onError(String errorMsg, String url) {
				if(Debug)Log.i(DebugTag, curIndex+"Times-Error:"+errorMsg);
				if(requestListener!=null)
				requestListener.onError(errorMsg);
			}
		}, shouldCache, timeoutCount, retryTimes);
	}


	public LoadController sendRequest(int method, final String url, Object data,final Map<String, String> headers,
			final LoadListener requestListener, boolean shouldCache, int timeoutCount, int retryTimes) {
		if (requestListener == null)
			throw new NullPointerException();

		final ByteArrayLoadController loadControler = new ByteArrayLoadController(requestListener);

		Request<?> request = null;
        request = new ByteArrayRequest(method, url, data, headers,loadControler, loadControler);
        request.setShouldCache(shouldCache);


		if (headers != null && !headers.isEmpty()) {// add headers if not empty
			try {
				request.getHeaders().putAll(headers);
			} catch (AuthFailureError e) {
				e.printStackTrace();
			}
		}

		RetryPolicy retryPolicy = new DefaultRetryPolicy(timeoutCount, retryTimes, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
		request.setRetryPolicy(retryPolicy);

		loadControler.bindRequest(request);

		if (this.mRequestQueue == null)
			throw new NullPointerException();
		requestListener.onStart();
		this.mRequestQueue.add(request);

		return loadControler;
	}
	
	
	

}
