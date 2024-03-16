package com.lasthopesoftware.policies.caching

import com.google.common.cache.CacheBuilder
import org.joda.time.Duration
import java.util.concurrent.TimeUnit

class TimedExpirationPromiseCache<Input : Any, Output>(expireAfter: Duration) :
	GuavaPromiseCache<Input, Output>(
		CacheBuilder.newBuilder()
		.expireAfterWrite(expireAfter.millis, TimeUnit.MILLISECONDS)
		.build())
