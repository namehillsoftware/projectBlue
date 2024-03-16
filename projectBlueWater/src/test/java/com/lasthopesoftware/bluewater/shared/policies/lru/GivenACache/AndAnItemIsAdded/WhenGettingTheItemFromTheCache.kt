package com.lasthopesoftware.bluewater.shared.policies.lru.GivenACache.AndAnItemIsAdded

import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheItemFromTheCache {
	private val firstItem = Any()

	private val item by lazy {
		val cache = LruPromiseCache<String, Any>(2)
		cache.getOrAdd("first-key") { Promise(firstItem) }.toExpiringFuture().get()
		cache.getOrAdd("second-key") { Promise(Any()) }.toExpiringFuture().get()
		cache.getOrAdd("first-key") { Promise(Any()) }.toExpiringFuture().get()
	}

	@Test
	fun `then the first item has not changed`() {
		assertThat(item).isEqualTo(firstItem)
	}
}
