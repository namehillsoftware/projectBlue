package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/8/16.
 */
class RejectedExecutor implements ThreeParameterRunnable<Exception, OneParameterRunnable<Void>, OneParameterRunnable<Exception>> {
	private final OneParameterRunnable<Exception> onRejected;

	RejectedExecutor(@NotNull OneParameterRunnable<Exception> onRejected) {
		this.onRejected = onRejected;
	}

	@Override
	public void run(Exception exception, OneParameterRunnable<Void> newResolve, OneParameterRunnable<Exception> newReject) {
		try {
			onRejected.run(exception);
			newResolve.run(null);
		} catch (Exception e) {
			newReject.run(e);
		}
	}
}
