package com.namehillsoftware.handoff.promises.queued.cancellation;


import java.util.concurrent.atomic.AtomicBoolean;

public class CancellationToken implements Runnable {

	private final AtomicBoolean isCancelled = new AtomicBoolean();

	public final boolean isCancelled() {
		return isCancelled.get();
	}

	@Override
	public void run() {
		isCancelled.set(true);
	}
}
