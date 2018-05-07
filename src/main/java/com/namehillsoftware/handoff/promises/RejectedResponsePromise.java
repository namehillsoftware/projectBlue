package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

final class RejectedResponsePromise<TResult, TNewResult> extends RejectionResponsePromise<TResult, TNewResult> {
	private final ImmediateResponse<Throwable, TNewResult> onFulfilled;

	RejectedResponsePromise(ImmediateResponse<Throwable, TNewResult> onRejected) {
		this.onFulfilled = onRejected;
	}

	@Override
	protected void respond(Throwable throwable) throws Throwable {
		resolve(onFulfilled.respond(throwable));
	}
}
