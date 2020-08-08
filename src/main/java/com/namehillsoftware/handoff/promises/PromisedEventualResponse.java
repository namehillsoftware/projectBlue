package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.PromisedResponse;

class PromisedEventualResponse<Resolution, Response> extends EventualResponse<Resolution, Response> {

	private final PromisedResponse<Resolution, Response> onFulfilled;
	private final PromisedResponse<Throwable, Response> onRejected;

	PromisedEventualResponse(PromisedResponse<Resolution, Response> onFulfilled) {
		this(onFulfilled, null);
	}

	PromisedEventualResponse(PromisedResponse<Resolution, Response> onFulfilled, PromisedResponse<Throwable, Response> onRejected) {
		this.onFulfilled = onFulfilled;
		this.onRejected = onRejected;
	}

	@Override
	protected void respond(Resolution resolution) throws Throwable {
		proxy(onFulfilled.promiseResponse(resolution));
	}

	@Override
	protected void respond(Throwable reason) throws Throwable {
		if (onRejected != null)
			proxy(onRejected.promiseResponse(reason));
		else
			reject(reason);
	}
}
