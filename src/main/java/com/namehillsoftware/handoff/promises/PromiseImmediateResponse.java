package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

class PromiseImmediateResponse<Resolution, Response> extends PromiseResponse<Resolution, Response> {

	private final ImmediateResponse<Resolution, Response> onFulfilled;
	private final ImmediateResponse<Throwable, Response> onRejected;

	PromiseImmediateResponse(ImmediateResponse<Resolution, Response> onFulfilled) {
		this(onFulfilled, null);
	}

	PromiseImmediateResponse(
			ImmediateResponse<Resolution, Response> onFulfilled,
			ImmediateResponse<Throwable, Response> onRejected) {
		this.onFulfilled = onFulfilled;
		this.onRejected = onRejected;
	}

	@Override
	protected void respond(Resolution resolution) throws Throwable {
		resolve(onFulfilled.respond(resolution));
	}

	@Override
	protected void respond(Throwable rejection) throws Throwable {
		if (onRejected != null)
			resolve(onRejected.respond(rejection));
		else
			reject(rejection);
	}
}
