package com.lasthopesoftware.resources.executors;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class CachedSingleThreadExecutor extends ThreadPoolExecutor {
	public CachedSingleThreadExecutor() {
		super(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
	}
}
