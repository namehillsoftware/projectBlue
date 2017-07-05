package com.lasthopesoftware.providers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AbstractProvider {
	public static final ExecutorService providerExecutor = Executors.newSingleThreadExecutor();
}