package com.lasthopesoftware.messenger.promises.aggregation;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;

import java.util.Collection;

public class CollectedErrorExcuse<TResult> implements ImmediateResponse<Throwable, Throwable> {

	private final Messenger<Collection<TResult>> messenger;
	private Throwable error;

	public CollectedErrorExcuse(Messenger<Collection<TResult>> messenger, Collection<Promise<TResult>> promises) {
		this.messenger = messenger;
		for (Promise<TResult> promise : promises) promise.excuse(this);
	}

	@Override
	public Throwable respond(Throwable throwable) throws Exception {
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
