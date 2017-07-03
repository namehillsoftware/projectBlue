package com.lasthopesoftware.messenger.promise.queued.cancellation;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CancellationToken implements Runnable {

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private boolean isCancelled;

	public boolean isCancelled() {
		final Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			return isCancelled;
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void run() {
		if (isCancelled) return;

		final Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			isCancelled = true;
		} finally {
			writeLock.unlock();
		}
	}
}
