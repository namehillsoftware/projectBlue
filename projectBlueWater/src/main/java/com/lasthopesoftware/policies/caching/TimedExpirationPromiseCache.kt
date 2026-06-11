package com.lasthopesoftware.policies.caching

import com.google.common.cache.CacheBuilder
import org.joda.time.Duration

class TimedExpirationPromiseCache<Input : Any, Output>(expireAfter: Duration) :
	GuavaPromiseCache<Input, Output>(
		CacheBuilder.newBuilder()
			.expireAfterWrite(java.time.Duration.ofMillis(expireAfter.millis))
			.build())
