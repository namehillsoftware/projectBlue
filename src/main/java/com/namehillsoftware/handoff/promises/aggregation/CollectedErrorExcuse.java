package com.namehillsoftware.handoff.promises.aggregation;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.util.Collection;

public class CollectedErrorExcuse<TResult> implements ImmediateResponse<Throwable, Throwable> {

	private final Messenger<Collection<TResult>> messenger;
	private Throwable error;

	public CollectedErrorExcuse(Messenger<Collection<TResult>> messenger, Collection<Promise<TResult>> promises) {
		this.messenger = messenger;
		for (Promise<TResult> promise : promises) promise.excuse(this);
	}

	@Override
	public Throwable respond(Throwable throwable) {
		this.error = throwable;
		attemptRejection();
		return throwable;
	}

	private void attemptRejection() {
		if (messenger != null && error != null)
			messenger.sendRejection(error);
	}

	public boolean isRejected() {
		return error != null;
	}
}
