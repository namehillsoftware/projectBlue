package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

class ResponseExecutor<Resolution, Response> extends ResponseRoutingMessenger<Resolution, Response> {

	private final ImmediateResponse<Resolution, Response> onFulfilled;
	private final ImmediateResponse<Throwable, Response> onRejected;

	ResponseExecutor(
			ImmediateResponse<Resolution, Response> onFulfilled,
			ImmediateResponse<Throwable, Response> onRejected) {

		this.onFulfilled = onFulfilled;
		this.onRejected = onRejected;
	}

	@Override
	protected void respond(Resolution resolution) throws Throwable {
		sendResolution(onFulfilled.respond(resolution));
	}

	@Override
	protected void respond(Throwable rejection) throws Throwable {
		sendResolution(onRejected.respond(rejection));
	}
}
