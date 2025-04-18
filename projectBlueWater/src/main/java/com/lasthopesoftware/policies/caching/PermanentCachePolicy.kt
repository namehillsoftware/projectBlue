package com.lasthopesoftware.policies.caching

object PermanentCachePolicy : CachingPolicyFactory() {
	override fun <Input : Any, Output> getCache(): CachePromiseFunctions<Input, Output> = PermanentPromiseFunctionCache()
}
