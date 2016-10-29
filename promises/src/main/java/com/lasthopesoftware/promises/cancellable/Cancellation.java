package com.lasthopesoftware.promises.cancellable;

import com.lasthopesoftware.promises.NoOpRunnable;

/**
 * Created by david on 10/25/16.
 */

public class Cancellation {

	private Runnable reaction = NoOpRunnable.getInstance();

	public void cancel() {
		reaction.run();
	}

	public void onCancelled(Runnable react) {
		reaction = react;
	}
}
