package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

final class RejectedResponsePromise<Resolution, Response> extends ResponseRoutingPromise<Resolution, Response> {
	private final ImmediateResponse<Throwable, Response> onFulfilled;

	RejectedResponsePromise(ImmediateResponse<Throwable, Response> onRejected) {
		this.onFulfilled = onRejected;
	}

	@Override
	protected void respond(Resolution resolution) {}

	@Override
	protected void respond(Throwable throwable) throws Throwable {
		resolve(onFulfilled.respond(throwable));
	}
}
