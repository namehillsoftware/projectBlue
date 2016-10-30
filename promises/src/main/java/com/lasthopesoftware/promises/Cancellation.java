package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterAction;

/**
 * Created by david on 10/25/16.
 */

class Cancellation implements OneParameterAction<Runnable> {

	private Runnable reaction = NoOpRunnable.getInstance();

	public void cancel() {
		reaction.run();
	}

	@Override
	public void runWith(Runnable reaction) {
		this.reaction = reaction;
	}
}
