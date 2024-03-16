package com.lasthopesoftware.policies.caching

import com.google.common.cache.CacheBuilder

class LruPromiseCache<Input : Any, Output>(maxValues: Int) :
	GuavaPromiseCache<Input, Output>(CacheBuilder.newBuilder()
		.maximumSize(maxValues.toLong())
		.build())
