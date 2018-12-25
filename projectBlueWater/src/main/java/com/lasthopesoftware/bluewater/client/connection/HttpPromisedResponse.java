package com.lasthopesoftware.bluewater.client.connection;

import com.namehillsoftware.handoff.promises.Promise;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

public class HttpPromisedResponse extends Promise<Response> implements Callback {
	HttpPromisedResponse(Call call) {
		call.enqueue(this);
		respondToCancellation(call::cancel);
	}

	@Override
	public void onFailure(Call call, IOException e) {
		reject(e);
	}

	@Override
	public void onResponse(Call call, Response response) {
		resolve(response);
	}
}
