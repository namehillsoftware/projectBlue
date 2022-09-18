package com.lasthopesoftware.bluewater.shared.policies.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.lasthopesoftware.resources.executors.ThreadPools

class LruPromiseCache<Input : Any, Output>(maxValues: Int) :
	CaffeinePromiseCache<Input, Output>(Caffeine.newBuilder()
		.maximumSize(maxValues.toLong())
		.executor(ThreadPools.compute)
		.build())
