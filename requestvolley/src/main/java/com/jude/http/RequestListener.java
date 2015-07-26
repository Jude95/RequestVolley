package com.jude.http;

public interface RequestListener {
	void onRequest();
	void onSuccess(String response);
	void onError(String errorMsg);
}
