package com.lasthopesoftware.bluewater.shared.policies.lru.GivenACache.AndAnItemIsAdded

import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenGettingTheItemFromTheCache {
	companion object {
		private val firstItem = Any()

		private val item by lazy {
			val cache = LruPromiseCache<String, Any>(2)
			cache.getOrAdd("first-key") { Promise(firstItem) }.toFuture().get()
			cache.getOrAdd("second-key") { Promise(Any()) }.toFuture().get()
			cache.getOrAdd("first-key") { Promise(Any()) }.toFuture().get()
		}
	}

	@Test
	fun thenTheFirstItemDoesNotChanged() {
		assertThat(item).isEqualTo(firstItem)
	}
}
