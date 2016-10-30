package com.lasthopesoftware.promises;

import com.vedsoft.lazyj.Lazy;

/**
 * Created by david on 10/29/16.
 */

class NoOpRunnable implements Runnable {

	private final static Lazy<Runnable> instance = new Lazy<>(NoOpRunnable::new);

	public static Runnable getInstance() {
		return instance.getObject();
	}

	private NoOpRunnable() {

	}

	@Override
	public void run() {

	}
}
