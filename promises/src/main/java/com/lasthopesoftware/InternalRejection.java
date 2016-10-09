package com.lasthopesoftware;

import com.vedsoft.futures.runnables.OneParameterRunnable;

/**
 * Created by david on 10/8/16.
 */
class InternalRejection implements OneParameterRunnable<Exception> {

	private final UnresolvedPromise<Exception, Void> rejection;

	InternalRejection(UnresolvedPromise<Exception, Void> rejection) {
		this.rejection = rejection;
	}

	@Override
	public void run(Exception error) {
		if (rejection != null)
			rejection.execute(error);
	}
}
