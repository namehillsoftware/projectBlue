package com.lasthopesoftware.promises.cancellable;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by david on 10/25/16.
 */

class Cancellation {

	private final Queue<Runnable> cancellationReactions =
		new LinkedList<>();

	public void cancel() {
		Runnable cancellationReaction;
		while ((cancellationReaction = cancellationReactions.poll()) != null)
			cancellationReaction.run();
	}

	void onCancelled(Runnable react) {
		if (react != null)
			cancellationReactions.offer(react);
	}
}
