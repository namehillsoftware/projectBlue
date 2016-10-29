package com.lasthopesoftware.promises.cancellable;

import java.util.Stack;

/**
 * Created by david on 10/25/16.
 */

class Cancellation {

	private final Stack<Runnable> cancellationReactions = new Stack<>();

	public void cancel() {
		while (!cancellationReactions.empty())
			cancellationReactions.pop().run();
	}

	void onCancelled(Runnable react) {
		if (react != null)
			cancellationReactions.push(react);
	}
}
