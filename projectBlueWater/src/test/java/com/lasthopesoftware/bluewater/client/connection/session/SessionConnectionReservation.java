package com.lasthopesoftware.bluewater.client.connection.session;

import java.util.concurrent.locks.ReentrantLock;

public final class SessionConnectionReservation implements AutoCloseable {
	private final ReentrantLock lock = new ReentrantLock();

	public SessionConnectionReservation() {
		lock.lock();
	}

	@Override
	public void close() {
		lock.unlock();
	}
}
