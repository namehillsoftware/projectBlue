package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

final class FulfilledResponseExecutor<Resolution, Response> extends ResolutionResponseMessenger<Resolution, Response> {
	private final ImmediateResponse<Resolution, Response> onFulfilled;

	FulfilledResponseExecutor(ImmediateResponse<Resolution, Response> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	protected void respond(Resolution resolution) throws Throwable {
		sendResolution(onFulfilled.respond(resolution));
	}
}
