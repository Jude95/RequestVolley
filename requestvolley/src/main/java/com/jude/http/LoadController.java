package com.jude.http;

import com.jude.volley.Request;

/**
 * LoadControler for Request
 * 
 * @author steven pan
 * 
 */
public interface LoadController {
	void cancel();
    Request<?> getmRequest();
}

/**
 * Abstract LoaderControler that implements LoadControler
 * 
 * @author steven pan
 * 
 */
class AbsLoadController implements LoadController {
	
	protected Request<?> mRequest;

    public Request<?> getmRequest() {
        return mRequest;
    }

    public void bindRequest(Request<?> request) {
		this.mRequest = request;
	}

	@Override
	public void cancel() {
		if (this.mRequest != null) {
			this.mRequest.cancel();
		}
	}

	protected String getOriginUrl() {
		return this.mRequest.getOriginUrl();
	}
}
