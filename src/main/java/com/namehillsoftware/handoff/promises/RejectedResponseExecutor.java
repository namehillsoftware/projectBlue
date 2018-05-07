package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

final class RejectedResponseExecutor<TResult, TNewResult> extends RejectionResponseMessenger<TResult, TNewResult> {
	private final ImmediateResponse<Throwable, TNewResult> onFulfilled;

	RejectedResponseExecutor(ImmediateResponse<Throwable, TNewResult> onRejected) {
		this.onFulfilled = onRejected;
	}

	@Override
	protected void respond(Throwable throwable) throws Throwable {
		sendResolution(onFulfilled.respond(throwable));
	}
}
