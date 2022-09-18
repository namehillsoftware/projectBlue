package com.lasthopesoftware.bluewater.shared.policies.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.lasthopesoftware.resources.executors.ThreadPools
import org.joda.time.Duration
import java.util.concurrent.TimeUnit

class TimedExpirationPromiseCache<Input : Any, Output>(expireAfter: Duration) :
	CaffeinePromiseCache<Input, Output>(
		Caffeine.newBuilder()
			.executor(ThreadPools.compute)
			.expireAfterWrite(expireAfter.millis, TimeUnit.MILLISECONDS)
			.build())
