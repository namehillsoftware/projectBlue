package com.lasthopesoftware;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

/**
 * Created by david on 10/8/16.
 */
class RejectedInternalExecutor implements ThreeParameterRunnable<Exception, OneParameterRunnable<Void>, OneParameterRunnable<Exception>> {
	private final OneParameterRunnable<Exception> onRejected;

	RejectedInternalExecutor(OneParameterRunnable<Exception> onRejected) {
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
