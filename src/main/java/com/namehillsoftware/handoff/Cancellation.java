package com.namehillsoftware.handoff;

import java.util.concurrent.locks.ReentrantLock;

class Cancellation {

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

	public final void respondToCancellation(Runnable reaction) {
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
