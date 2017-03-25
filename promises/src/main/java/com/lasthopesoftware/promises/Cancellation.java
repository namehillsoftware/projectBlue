package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterAction;

/**
 * Created by david on 10/25/16.
 */

class Cancellation implements OneParameterAction<Runnable> {

	private Runnable reaction;
	private boolean isCancelled;

	public void cancel() {
		isCancelled = true;

		if (reaction != null)
			reaction.run();

		reaction = null;
	}

	@Override
	public void runWith(Runnable reaction) {
		this.reaction = reaction;
		if (isCancelled)
			cancel();
	}
}
