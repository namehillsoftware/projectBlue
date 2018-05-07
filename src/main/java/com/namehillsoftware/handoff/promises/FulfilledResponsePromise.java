package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

final class FulfilledResponsePromise<Resolution, Response> extends ResolutionResponsePromise<Resolution, Response> {
	private final ImmediateResponse<Resolution, Response> onFulfilled;

	FulfilledResponsePromise(ImmediateResponse<Resolution, Response> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	protected void respond(Resolution resolution) throws Throwable {
		resolve(onFulfilled.respond(resolution));
	}
}
