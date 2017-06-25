package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.concurrent.locks.ReentrantLock;

class Cancellation implements OneParameterAction<Runnable> {

	private final ReentrantLock reentrantLock = new ReentrantLock();
	private Runnable reaction;
	private boolean isCancelled;

	public final void cancel() {
		reentrantLock.lock();
		try {
			isCancelled = true;

			if (reaction != null)
				reaction.run();

			reaction = null;
		} finally {
			reentrantLock.unlock();
		}
	}

	@Override
	public final void runWith(Runnable reaction) {
		reentrantLock.lock();
		try {
			this.reaction = reaction;
			if (isCancelled)
				cancel();
		} finally {
			reentrantLock.unlock();
		}
	}
}
