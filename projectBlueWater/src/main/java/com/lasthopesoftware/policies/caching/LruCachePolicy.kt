package com.lasthopesoftware.policies.caching

class LruCachePolicy(private val maxValues: Int) : CachingPolicyFactory() {
	override fun <Input : Any, Output> getCache() = LruPromiseCache<Input, Output>(maxValues)
}
