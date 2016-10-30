package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterAction;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/25/16.
 */

class Cancellation implements OneParameterAction<Runnable> {

	private Runnable reaction;

	public void cancel() {
		(reaction != null ? reaction : NoOpRunnable.getInstance()).run();
	}

	@Override
	public void runWith(@NotNull Runnable reaction) {
		this.reaction = reaction;
	}
}
