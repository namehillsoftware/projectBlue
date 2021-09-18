package com.lasthopesoftware.resources.executors;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class CachedManyThreadExecutor extends ThreadPoolExecutor {
	public CachedManyThreadExecutor(int maximumThreads, long keepAliveTime, TimeUnit keepAliveUnit) {
		super(0, maximumThreads, keepAliveTime, keepAliveUnit, new LinkedBlockingQueue<>());
	}
}
