package com.lasthopesoftware.bluewater.client.connection;

import com.namehillsoftware.handoff.promises.Promise;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

public class HttpPromisedResponse extends Promise<Response> implements Callback, Runnable {
	private final Call call;

	public HttpPromisedResponse(Call call) {
		this.call = call;
		respondToCancellation(this);
		call.enqueue(this);
	}

	@Override
	public void onFailure(Call call, IOException e) {
		reject(e);
	}

	@Override
	public void onResponse(Call call, Response response) {
		resolve(response);
	}

	@Override
	public void run() {
		this.call.cancel();
	}
}
